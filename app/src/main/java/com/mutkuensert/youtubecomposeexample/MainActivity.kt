package com.mutkuensert.youtubecomposeexample

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mutkuensert.youtubecomposeexample.ui.theme.YoutubeComposeExampleTheme
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.loadOrCueVideo
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YoutubeComposeExampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Screen(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun Screen(modifier: Modifier = Modifier) {
    Box(modifier) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            VideoPlayer(videoId = "VyHV0BRtdxo")
            Text(text = "Other screen content")
        }
    }
}

@Composable
private fun VideoPlayer(
    videoId: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current

    var playerView: YouTubePlayerView? by remember { mutableStateOf(null) }
    var _fullScreenView: View? by remember { mutableStateOf(null) }
    var defaultPlayer: YouTubePlayer? by remember { mutableStateOf(null) }
    var fullscreenPlayer: YouTubePlayer? by remember { mutableStateOf(null) }
    var shouldBeFullscreen: Boolean by rememberSaveable { mutableStateOf(false) }
    var currentSecond: Float by rememberSaveable { mutableStateOf(0f) }
    var playing: Boolean by rememberSaveable { mutableStateOf(false) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val view = createPlayerView(
                context,
                lifecycleOwner,
                currentSecond,
                onClickFullscreenToggle = {
                    if (playerView != null) {
                        if (_fullScreenView != null) {
                            _fullScreenView!!.removeFromParent()
                            _fullScreenView = null
                            shouldBeFullscreen = false
                        } else {
                            _fullScreenView =
                                createPlayerView(
                                    context,
                                    lifecycleOwner,
                                    currentSecond,
                                    onClickFullscreenToggle = {
                                        _fullScreenView!!.removeFromParent()
                                        _fullScreenView = null
                                        shouldBeFullscreen = false
                                    },
                                    onReady = {
                                        fullscreenPlayer = it
                                    }, onStateChange = {
                                        playing = it == PlayerConstants.PlayerState.PLAYING
                                    }, onCurrentSecond = {
                                        currentSecond = it
                                    })
                            shouldBeFullscreen = true
                            activity?.addViewMatchingParent(_fullScreenView!!)
                        }
                    }
                },
                onReady = {
                    defaultPlayer = it
                }, onStateChange = {
                    playing = it == PlayerConstants.PlayerState.PLAYING
                }, onCurrentSecond = {
                    currentSecond = it
                })
            playerView = view
            view
        }
    )

    OrientationListener(
        shouldBeFullscreen,
        getFullScreenView = {
            val view = createPlayerView(
                context,
                lifecycleOwner,
                currentSecond,
                onClickFullscreenToggle = {
                    _fullScreenView!!.removeFromParent()
                    _fullScreenView = null
                    shouldBeFullscreen = false
                },
                onReady = {
                    fullscreenPlayer = it
                },
                onStateChange = {
                    playing = it == PlayerConstants.PlayerState.PLAYING
                },
                onCurrentSecond = {
                    currentSecond = it
                })
            _fullScreenView = view
            view
        },
    )

    LaunchedEffect(defaultPlayer, fullscreenPlayer) {
        val player = if (fullscreenPlayer != null) fullscreenPlayer else defaultPlayer
        if (playing) {
            player?.loadOrCueVideo(lifecycleOwner.lifecycle, videoId, currentSecond)
        } else {
            player?.cueVideo(videoId, currentSecond)
        }
    }

    DisposableEffect(lifecycleOwner, activity) {
        onDispose {
            _fullScreenView?.removeFromParent()
            _fullScreenView = null
            playerView?.release()
            playerView = null
        }
    }
}

fun createPlayerView(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    currentSecond: Float,
    onClickFullscreenToggle: () -> Unit,
    onReady: (YouTubePlayer) -> Unit,
    onStateChange: (PlayerConstants.PlayerState) -> Unit,
    onCurrentSecond: (Float) -> Unit
): YouTubePlayerView {
    val youTubePlayerView = YouTubePlayerView(context)
    val overlayUi = youTubePlayerView.inflateCustomPlayerUi(R.layout.custom_layout)
    val view = overlayUi.findViewById<View>(R.id.fullscreenToggle)
    view.setOnClickListener {
        onClickFullscreenToggle.invoke()
    }
    youTubePlayerView.enableAutomaticInitialization = false

    lifecycleOwner.lifecycle.addObserver(youTubePlayerView)

    val options = IFramePlayerOptions.Builder(context)
        .controls(1)
        .fullscreen(0)
        .start(currentSecond.toInt())
        .ivLoadPolicy(3)
        .build()

    val listener = object : AbstractYouTubePlayerListener() {
        override fun onReady(youTubePlayer: YouTubePlayer) {
            onReady(youTubePlayer)
        }

        override fun onStateChange(
            youTubePlayer: YouTubePlayer,
            state: PlayerConstants.PlayerState
        ) {
            onStateChange(state)
        }

        override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
            onCurrentSecond(second)
        }
    }

    youTubePlayerView.initialize(listener, options)
    return youTubePlayerView
}

@Composable
private fun OrientationListener(
    shouldBeFullscreen: Boolean,
    getFullScreenView: () -> View,
) {
    val orientation = LocalConfiguration.current.orientation
    var previousOrientation by rememberSaveable { mutableStateOf(orientation) }
    val activity = LocalContext.current.findActivity()
    LaunchedEffect(orientation) {
        if (shouldBeFullscreen && orientation != previousOrientation && activity != null) {
            val fullScreenView = getFullScreenView()
            activity.addViewMatchingParent(fullScreenView)
        }
        previousOrientation = orientation
    }
}

private fun Activity.addViewMatchingParent(view: View) {
    val decor = window.decorView as ViewGroup
    decor.addView(
        view,
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )
    decor.post {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(decor.width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(decor.height, View.MeasureSpec.EXACTLY)
        view.measure(widthSpec, heightSpec)
        view.layout(0, 0, decor.width, decor.height)
        view.requestLayout()
        view.invalidate()
    }
}

fun View.removeFromParent() {
    (this.parent as ViewGroup).removeView(this)
}

fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> this.baseContext.findActivity()
        else -> null
    }
}

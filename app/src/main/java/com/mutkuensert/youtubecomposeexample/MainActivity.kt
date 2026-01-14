package com.mutkuensert.youtubecomposeexample

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mutkuensert.youtubecomposeexample.ui.theme.YoutubeComposeExampleTheme
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker
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
    var player: YouTubePlayer? by remember { mutableStateOf(null) }
    var shouldBeFullscreen: Boolean by rememberSaveable { mutableStateOf(false) }
    var currentSecond: Float by rememberSaveable { mutableStateOf(0f) }
    var playing: Boolean by rememberSaveable { mutableStateOf(false) }
    val tracker = remember { YouTubePlayerTracker() }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val youTubePlayerView = YouTubePlayerView(context)
            youTubePlayerView.enableAutomaticInitialization = false

            lifecycleOwner.lifecycle.addObserver(youTubePlayerView)

            val options = IFramePlayerOptions.Builder(context)
                .controls(1)
                .fullscreen(1)
                .start(currentSecond.toInt())
                .ivLoadPolicy(3)
                .build()

            val listener = object : AbstractYouTubePlayerListener() {
                override fun onReady(youTubePlayer: YouTubePlayer) {
                    player = youTubePlayer
                }

                override fun onStateChange(
                    youTubePlayer: YouTubePlayer,
                    state: PlayerConstants.PlayerState
                ) {
                    playing = state == PlayerConstants.PlayerState.PLAYING
                }

                override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                    currentSecond = second
                }
            }

            youTubePlayerView.initialize(listener, options)

            youTubePlayerView.addFullscreenListener(object : FullscreenListener {
                override fun onEnterFullscreen(fullscreenView: View, exitFullscreen: () -> Unit) {
                    activity?.addViewMatchingParent(fullscreenView)
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    _fullScreenView = fullscreenView
                    shouldBeFullscreen = true
                }


                override fun onExitFullscreen() {
                    _fullScreenView?.removeFromParent()
                    _fullScreenView = null
                    shouldBeFullscreen = false
                }
            })

            playerView = youTubePlayerView
            youTubePlayerView
        }
    )

    OrientationListener(shouldBeFullscreen, playerView, activity)

    LaunchedEffect(player) {
        if (playing) {
            player?.loadOrCueVideo(lifecycleOwner.lifecycle, videoId, currentSecond)
        } else {
            player?.cueVideo(videoId, 0f)
        }
        player?.addListener(tracker)
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

@Composable
private fun OrientationListener(
    shouldBeFullscreen: Boolean,
    playerView: YouTubePlayerView?,
    activity: Activity?
) {
    val orientation = LocalConfiguration.current.orientation
    var previousOrientation by rememberSaveable { mutableStateOf(orientation) }
    LaunchedEffect(shouldBeFullscreen, orientation) {
        val _playerView = playerView
        if (shouldBeFullscreen && orientation != previousOrientation && _playerView?.isFullscreen() == false && activity != null) {
            _playerView.removeFromParent()
            activity.addViewMatchingParent(_playerView)
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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

fun YouTubePlayerView.isFullscreen(): Boolean {
    return isMatchParentWidth() && isMatchParentHeight()
}

fun View.isMatchParentWidth(): Boolean {
    return layoutParams?.width == ViewGroup.LayoutParams.MATCH_PARENT
}

fun View.isMatchParentHeight(): Boolean {
    return layoutParams?.height == ViewGroup.LayoutParams.MATCH_PARENT
}

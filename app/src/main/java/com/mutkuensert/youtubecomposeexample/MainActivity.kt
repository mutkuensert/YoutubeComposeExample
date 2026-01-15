package com.mutkuensert.youtubecomposeexample

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var playerView: YouTubePlayerView? by remember { mutableStateOf(null) }
    var fullscreenPlayerView: YouTubePlayerView? by remember { mutableStateOf(null) }
    var defaultPlayer: YouTubePlayer? by remember { mutableStateOf(null) }
    var fullscreenPlayer: YouTubePlayer? by remember { mutableStateOf(null) }
    var shouldBeFullscreen: Boolean by rememberSaveable { mutableStateOf(false) }
    var currentSecond: Float by rememberSaveable { mutableFloatStateOf(0f) }
    var playing: Boolean by rememberSaveable { mutableStateOf(false) }
    val defaultPlayerTracker = remember { YouTubePlayerTracker() }
    val fullscreenPlayerTracker = remember { YouTubePlayerTracker() }
    val defaultPlayerListener = remember {
        object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                youTubePlayer.addListener(defaultPlayerTracker)
                defaultPlayer = youTubePlayer
            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                currentSecond = second
            }
        }
    }

    val fullscreenPlayerListener = remember {
        object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                youTubePlayer.addListener(fullscreenPlayerTracker)
                fullscreenPlayer = youTubePlayer
            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                currentSecond = second
            }
        }
    }

    val cancelFullscreenResources = {
        fullscreenPlayerView?.let {
            //lifecycleOwner.lifecycle.removeObserver(it)
        }
        fullscreenPlayerView?.removeFromParent()
        fullscreenPlayerView?.release()
        fullscreenPlayerView = null
        fullscreenPlayer = null
    }

    val closeFullscreen = {
        playing = shouldPlay(fullscreenPlayerTracker.state)
        cancelFullscreenResources.invoke()
        shouldBeFullscreen = false
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val view = createPlayerView(
                context,
                lifecycleOwner,
                onClickFullscreenToggle = {
                    fullscreenPlayerView = createPlayerView(
                        context,
                        lifecycleOwner,
                        onClickFullscreenToggle = closeFullscreen,
                        fullscreenPlayerListener
                    )
                    shouldBeFullscreen = true
                    playing = shouldPlay(defaultPlayerTracker.state)
                    activity?.addViewMatchingScreen(fullscreenPlayerView!!)
                },
                defaultPlayerListener
            )
            playerView = view
            view
        }
    )

    OrientationListener(
        shouldBeFullscreen,
        createFullScreenView = {
            val view = createPlayerView(
                context,
                lifecycleOwner,
                onClickFullscreenToggle = closeFullscreen,
                fullscreenPlayerListener
            )
            fullscreenPlayerView = view
            view
        },
    )

    LaunchedEffect(defaultPlayer, fullscreenPlayer) {
        val player = if (fullscreenPlayer != null) {
            fullscreenPlayer
        } else {
            defaultPlayer
        }
        if (playing) {
            player?.loadOrCueVideo(lifecycleOwner.lifecycle, videoId, currentSecond)
        } else {
            player?.cueVideo(videoId, currentSecond)
        }
    }

    DisposableEffect(lifecycleOwner, activity) {
        onDispose {
            val state = if (shouldBeFullscreen) {
                fullscreenPlayerTracker.state
            } else {
                defaultPlayerTracker.state
            }
            playing = shouldPlay(state)
            cancelFullscreenResources.invoke()
            playerView?.release()
            playerView = null
        }
    }
}

private fun shouldPlay(state: PlayerConstants.PlayerState): Boolean {
    return state == PlayerConstants.PlayerState.PLAYING
            || state == PlayerConstants.PlayerState.BUFFERING
            || state == PlayerConstants.PlayerState.VIDEO_CUED
}

private fun createPlayerView(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    onClickFullscreenToggle: () -> Unit,
    listener: AbstractYouTubePlayerListener,
): YouTubePlayerView {
    val youTubePlayerView = YouTubePlayerView(context)
    youTubePlayerView.enableAutomaticInitialization = false
    lifecycleOwner.lifecycle.addObserver(youTubePlayerView)
    youTubePlayerView.addFullscreenListener(object : FullscreenListener {
        override fun onEnterFullscreen(
            fullscreenView: View,
            exitFullscreen: () -> Unit
        ) {
            onClickFullscreenToggle()
        }

        override fun onExitFullscreen() {
            onClickFullscreenToggle()
        }
    })

    val options = IFramePlayerOptions.Builder(context)
        .controls(1)
        .rel(0)
        .fullscreen(1)
        .ivLoadPolicy(3)
        .build()

    youTubePlayerView.initialize(listener, options)
    return youTubePlayerView
}

@Composable
private fun OrientationListener(
    shouldBeFullscreen: Boolean,
    createFullScreenView: () -> View,
) {
    val orientation = LocalConfiguration.current.orientation
    var previousOrientation by rememberSaveable { mutableIntStateOf(orientation) }
    val activity = LocalActivity.current
    LaunchedEffect(orientation) {
        if (shouldBeFullscreen && orientation != previousOrientation && activity != null) {
            val fullScreenView = createFullScreenView()
            activity.addViewMatchingScreen(fullScreenView)
        }
        previousOrientation = orientation
    }
}

private fun Activity.addViewMatchingScreen(view: View) {
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

private fun View.removeFromParent() {
    (this.parent as ViewGroup).removeView(this)
}

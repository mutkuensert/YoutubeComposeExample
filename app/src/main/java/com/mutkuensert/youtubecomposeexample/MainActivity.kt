package com.mutkuensert.youtubecomposeexample

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mutkuensert.youtubecomposeexample.ui.theme.YoutubeComposeExampleTheme
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker
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
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var playerView: YouTubePlayerView? by remember { mutableStateOf(null) }
    var fullscreenView: View? by remember { mutableStateOf(null) }
    var player: YouTubePlayer? by remember { mutableStateOf(null) }
    val tracker = remember { YouTubePlayerTracker() }
    val playerListener = remember {
        object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                youTubePlayer.addListener(tracker)
                player = youTubePlayer
            }
        }
    }
    val exitFullscreen = {
        activity?.showSystemBars()
        fullscreenView?.removeFromParent()
        fullscreenView = null
    }

    if (fullscreenView != null) {
        BackHandler { exitFullscreen() }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val view = createPlayerView(
                context,
                lifecycleOwner,
                onEnterFullscreen = { view ->
                    fullscreenView = view
                    activity?.enableTransientSystemBars()
                    activity?.addViewMatchingScreen(view)
                },
                exitFullscreen,
                playerListener
            )
            playerView = view
            view
        }
    )

    LaunchedEffect(player, videoId) {
        player?.cueVideo(videoId, 0f)
    }

    DisposableEffect(lifecycleOwner, activity) {
        onDispose {
            fullscreenView?.removeFromParent()
            fullscreenView = null
            playerView?.let { lifecycleOwner.lifecycle.removeObserver(it) }
            playerView?.release()
            playerView = null
        }
    }
}

private fun Activity.enableTransientSystemBars() {
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    windowInsetsController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
}

private fun Activity.showSystemBars() {
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
    windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
}

private fun createPlayerView(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    onEnterFullscreen: (View) -> Unit,
    onExitFullscreen: () -> Unit,
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
            onEnterFullscreen(fullscreenView)
        }

        override fun onExitFullscreen() {
            onExitFullscreen()
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

private fun Activity.addViewMatchingScreen(view: View) {
    val decor = window.decorView as ViewGroup
    decor.addView(
        view,
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )
}

private fun View.removeFromParent() {
    (this.parent as ViewGroup).removeView(this)
}

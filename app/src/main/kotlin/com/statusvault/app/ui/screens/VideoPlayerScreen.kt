package com.statusvault.app.ui.screens

import android.content.ActivityNotFoundException
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    videoPath: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val file = remember(videoPath) { File(videoPath) }
    val fileName = file.name
    val fileExists = remember(videoPath) { file.exists() && file.canRead() }

    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Only create ExoPlayer if the file exists
    val exoPlayer = remember(videoPath) {
        if (!fileExists) {
            null
        } else {
            try {
                ExoPlayer.Builder(context).build().apply {
                    val uri = android.net.Uri.fromFile(file)
                    setMediaItem(MediaItem.fromUri(uri))
                    prepare()
                    playWhenReady = true
                    repeatMode = Player.REPEAT_MODE_ONE

                    addListener(object : Player.Listener {
                        override fun onPlayerError(error: PlaybackException) {
                            hasError = true
                            errorMessage = error.localizedMessage ?: "Unknown playback error"
                            Log.e("VideoPlayerScreen", "ExoPlayer error", error)
                        }
                    })
                }
            } catch (e: Exception) {
                Log.e("VideoPlayerScreen", "Failed to create ExoPlayer", e)
                hasError = true
                errorMessage = e.localizedMessage ?: "Failed to initialize player"
                null
            }
        }
    }

    DisposableEffect(videoPath) {
        onDispose {
            exoPlayer?.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = fileName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (file.exists()) {
                            try {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val shareIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    type = "video/*"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    android.content.Intent.createChooser(shareIntent, "Share Video")
                                )
                            } catch (e: ActivityNotFoundException) {
                                Log.e("VideoPlayerScreen", "No activity to handle share", e)
                            } catch (e: IllegalArgumentException) {
                                Log.e("VideoPlayerScreen", "FileProvider error", e)
                            } catch (e: Exception) {
                                Log.e("VideoPlayerScreen", "Share failed", e)
                            }
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(innerPadding)
        ) {
            when {
                !fileExists -> {
                    // File doesn't exist error state
                    ErrorState(
                        message = "Video file not found.\nIt may have been deleted from WhatsApp.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                hasError -> {
                    // Playback error state
                    ErrorState(
                        message = "Error playing video.\n$errorMessage",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                exoPlayer != null -> {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = true
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.BrokenImage,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

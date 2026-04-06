@file:OptIn(androidx.media3.common.util.UnstableApi::class)
package com.example.modernplayer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.media.audiofx.Visualizer

@OptIn(UnstableApi::class)
class MainActivity : ComponentActivity() {

    private var mediaFiles by mutableStateOf<List<DocumentFile>>(emptyList())
    private var selectedIndex by mutableStateOf(-1)

    private val openDocumentTree = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let { selectedUri ->
            contentResolver.takePersistableUriPermission(
                selectedUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            loadDirectory(selectedUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }
            ModernPlayerTheme(darkTheme = isDarkTheme) {
                MainScreen(
                    mediaFiles = mediaFiles,
                    selectedIndex = selectedIndex,
                    onFileClick = { index -> selectedIndex = index },
                    onFolderSelectClick = { openDocumentTree.launch(null) },
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = { isDarkTheme = !isDarkTheme }
                )
            }
        }
    }

    private fun loadDirectory(uri: Uri) {
        val root = DocumentFile.fromTreeUri(this, uri)
        val newFiles = mutableListOf<DocumentFile>()
        root?.listFiles()?.forEach { file ->
            val type = file.type?.lowercase() ?: ""
            val name = file.name?.lowercase() ?: ""
            val isMedia = type.startsWith("audio/") || type.startsWith("video/") || 
                          name.endsWith(".mp3") || name.endsWith(".wav") || 
                          name.endsWith(".m4a") || name.endsWith(".aac") || 
                          name.endsWith(".flac") || name.endsWith(".mp4") || 
                          name.endsWith(".mkv") || name.endsWith(".webm")
            
            if (isMedia) newFiles.add(file)
        }
        mediaFiles = newFiles
        selectedIndex = if (mediaFiles.isNotEmpty()) 0 else -1
    }
}

@Composable
fun ModernPlayerTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val darkColors = darkColorScheme(background = Color(0xFF0A0A0A), surface = Color(0xFF121212), primary = Color(0xFF00E5FF), secondary = Color(0xFF00B8D4))
    val lightColors = lightColorScheme(background = Color(0xFFF5F5F5), surface = Color(0xFFFFFFFF), primary = Color(0xFF00BCD4), secondary = Color(0xFF00838F))
    MaterialTheme(colorScheme = if (darkTheme) darkColors else lightColors, content = content)
}

@Composable
fun MainScreen(
    mediaFiles: List<DocumentFile>,
    selectedIndex: Int,
    onFileClick: (Int) -> Unit,
    onFolderSelectClick: () -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    var isFullscreen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Real Immersive Mode Logic
    LaunchedEffect(isFullscreen) {
        val activity = context as? Activity
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (isFullscreen) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!isFullscreen) {
                val bgLogoRes = if (isDarkTheme) R.drawable.alexmusic_watermark_logo else R.drawable.alexmusic_watermark_logo_light
                Image(
                    painter = painterResource(id = bgLogoRes),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                    alpha = 0.15f
                )
            }

            PlayerScreen(
                mediaFiles = mediaFiles,
                selectedIndex = selectedIndex,
                onFolderSelectClick = onFolderSelectClick,
                onFileClick = onFileClick,
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle,
                isFullscreen = isFullscreen,
                onFullscreenToggle = { isFullscreen = it }
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    mediaFiles: List<DocumentFile>,
    selectedIndex: Int,
    onFolderSelectClick: () -> Unit,
    onFileClick: (Int) -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    isFullscreen: Boolean,
    onFullscreenToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var hasAudioPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    var isVideoAvailable by remember { mutableStateOf(false) }
    var currentAudioSessionId by remember { mutableIntStateOf(0) }
    var controlsOverlayVisible by remember { mutableStateOf(false) }

    val exoPlayer = remember { ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_ALL } }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { 
                onFileClick(exoPlayer.currentMediaItemIndex) 
            }

            @androidx.media3.common.util.UnstableApi
            override fun onAudioSessionIdChanged(id: Int) { 
                currentAudioSessionId = id 
            }

            override fun onIsPlayingChanged(playing: Boolean) { 
                if (playing) currentAudioSessionId = exoPlayer.audioSessionId 
            }

            @androidx.media3.common.util.UnstableApi
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                isVideoAvailable = tracks.groups.any { group ->
                    if (group.type == androidx.media3.common.C.TRACK_TYPE_VIDEO) {
                        val format = group.getTrackFormat(0)
                        val mime = format.sampleMimeType ?: ""
                        !mime.startsWith("image/") && !mime.contains("artwork")
                    } else false
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.release() }
    }

    LaunchedEffect(mediaFiles) { if (mediaFiles.isNotEmpty()) { exoPlayer.setMediaItems(mediaFiles.map { MediaItem.fromUri(it.uri) }); exoPlayer.prepare() } }
    LaunchedEffect(selectedIndex) { if (selectedIndex in 0 until exoPlayer.mediaItemCount && exoPlayer.currentMediaItemIndex != selectedIndex) { exoPlayer.seekToDefaultPosition(selectedIndex); exoPlayer.play() } }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasAudioPermission = it }
    LaunchedEffect(Unit) { if (!hasAudioPermission) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }

    Box(modifier = Modifier.fillMaxSize()) {
        // LAYER 1: Normal Content
        if (!isFullscreen) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val smallLogoRes = if (isDarkTheme) R.drawable.alexmusic_watermark_logo else R.drawable.alexmusic_watermark_logo_light
                        Image(
                            painter = painterResource(id = smallLogoRes),
                            contentDescription = "App Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "AlexMusic", 
                            color = MaterialTheme.colorScheme.secondary, 
                            fontSize = 36.sp, 
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { onThemeToggle() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                                uncheckedThumbColor = Color.DarkGray,
                                uncheckedTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                            )
                        )
                        IconButton(onClick = onFolderSelectClick) { 
                            Icon(Icons.Default.Menu, null, tint = Color(0xFFFF007F), modifier = Modifier.size(32.dp)) 
                        }
                    }
                }

                // Normal Player Box
                Box(modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(24.dp)).background(Color.Black)) {
                    PlayerContent(exoPlayer, isVideoAvailable, hasAudioPermission, currentAudioSessionId, false, onFullscreenToggle, {})
                }

                ModernMediaControls(exoPlayer, Modifier.padding(vertical = 12.dp))

                // Playlist
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(mediaFiles.size) { index ->
                            val isSel = index == selectedIndex
                            
                            val cardBgColor by animateColorAsState(
                                if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                                animationSpec = tween(300)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(cardBgColor)
                                    .clickable { onFileClick(index) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = mediaFiles[index].name ?: "Unknown",
                                    color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        // LAYER 2: Fullscreen Overlay
        if (isFullscreen) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black).clickable { controlsOverlayVisible = !controlsOverlayVisible }) {
                PlayerContent(exoPlayer, isVideoAvailable, hasAudioPermission, currentAudioSessionId, true, onFullscreenToggle, { controlsOverlayVisible = !controlsOverlayVisible })
                
                // Overlaid Controls
                AnimatedVisibility(visible = controlsOverlayVisible, enter = fadeIn() + slideInVertically { it }, exit = fadeOut() + slideOutVertically { it }, modifier = Modifier.align(Alignment.BottomCenter)) {
                    Box(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.6f)).padding(bottom = 32.dp)) {
                        ModernMediaControls(exoPlayer)
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerContent(
    exoPlayer: ExoPlayer,
    isVideoAvailable: Boolean,
    hasAudioPermission: Boolean,
    audioSessionId: Int,
    isFullscreen: Boolean,
    onFullscreenToggle: (Boolean) -> Unit,
    onTap: () -> Unit
) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { PlayerView(context).apply { player = exoPlayer; useController = false; useArtwork = false; setBackgroundColor(android.graphics.Color.TRANSPARENT) } },
            modifier = Modifier.fillMaxSize().graphicsLayer(alpha = if (isVideoAvailable) 1f else 0f)
        )

        if (hasAudioPermission && audioSessionId != 0 && !isVideoAvailable) {
            VisualizerGraph(audioSessionId = audioSessionId, modifier = Modifier.fillMaxSize().padding(bottom = 60.dp))
        }

        // Fullscreen Toggle Button (Floating)
        IconButton(
            onClick = { onFullscreenToggle(!isFullscreen) },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).size(48.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen, null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun ModernMediaControls(exoPlayer: ExoPlayer, modifier: Modifier = Modifier) {
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var currentPos by remember { mutableLongStateOf(exoPlayer.currentPosition) }
    var duration by remember { mutableLongStateOf(exoPlayer.duration) }
    var shuffle by remember { mutableStateOf(exoPlayer.shuffleModeEnabled) }
    var repeat by remember { mutableIntStateOf(exoPlayer.repeatMode) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPos = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(0L)
            kotlinx.coroutines.delay(500)
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(p: Boolean) { isPlaying = p }
            override fun onPlaybackStateChanged(s: Int) { duration = exoPlayer.duration.coerceAtLeast(0L) }
            override fun onShuffleModeEnabledChanged(s: Boolean) { shuffle = s }
            override fun onRepeatModeChanged(r: Int) { repeat = r }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(value = currentPos.toFloat(), onValueChange = { currentPos = it.toLong() }, onValueChangeFinished = { exoPlayer.seekTo(currentPos) }, valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f), modifier = Modifier.padding(horizontal = 16.dp), colors = SliderDefaults.colors(thumbColor = Color(0xFFFF007F), activeTrackColor = Color(0xFF00D1FF)))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(currentPos), style = MaterialTheme.typography.labelSmall)
            Text(formatTime(duration), style = MaterialTheme.typography.labelSmall)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { exoPlayer.shuffleModeEnabled = !shuffle }) { Icon(Icons.Filled.Shuffle, null, tint = if (shuffle) Color(0xFF00D1FF) else Color.Gray) }
            IconButton(onClick = { exoPlayer.seekToPrevious() }) { Icon(Icons.Filled.SkipPrevious, null) }
            IconButton(onClick = { exoPlayer.seekBack() }) { Icon(Icons.Filled.Replay5, null) }
            Surface(onClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() }, shape = CircleShape, color = Color(0xFFFF007F), modifier = Modifier.size(56.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
            }
            IconButton(onClick = { exoPlayer.seekForward() }) { Icon(Icons.Filled.Forward5, null) }
            IconButton(onClick = { exoPlayer.seekToNext() }) { Icon(Icons.Filled.SkipNext, null) }
            IconButton(onClick = { exoPlayer.repeatMode = when(repeat) { Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE; Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL; else -> Player.REPEAT_MODE_OFF } }) { Icon(if (repeat == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat, null, tint = if (repeat != Player.REPEAT_MODE_OFF) Color(0xFF00D1FF) else Color.Gray) }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VisualizerGraph(audioSessionId: Int, modifier: Modifier = Modifier) {
    val bars = 32
    val heights = remember { FloatArray(bars) { 0f } }
    var trigger by remember { mutableIntStateOf(0) }
    DisposableEffect(audioSessionId) {
        val vis = try {
            Visualizer(audioSessionId).apply {
                enabled = false
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, w: ByteArray?, r: Int) {}
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, r: Int) {
                        if (fft == null) return
                        val maxBins = fft.size / 2
                        for (i in 0 until bars) {
                            val rawIdx = kotlin.math.floor(maxBins * Math.pow(i.toDouble() / bars, 2.0)).toInt()
                            val idx = (rawIdx * 2 + 2).coerceIn(2, fft.size - 2)
                            val mag = kotlin.math.hypot(fft[idx].toDouble(), fft[idx + 1].toDouble()).toFloat()
                            val norm = (mag / 128f).coerceIn(0f, 1f)
                            heights[i] = if (norm > heights[i]) norm else heights[i] * 0.85f
                        }
                        trigger++
                    }
                }, Visualizer.getMaxCaptureRate() / 2, false, true)
                enabled = true
            }
        } catch (e: Exception) { null }
        onDispose { vis?.release() }
    }
    val pColor = MaterialTheme.colorScheme.primary
    val sColor = MaterialTheme.colorScheme.secondary
    androidx.compose.foundation.Canvas(modifier = modifier) {
        if (trigger >= 0) {
            val barW = size.width / (bars * 1.5f)
            val space = barW * 0.5f
            for (i in 0 until bars) {
                val h = size.height * heights[i]
                val x = i * (barW + space) + space / 2
                drawRoundRect(brush = androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.lerp(pColor, sColor, heights[i])), topLeft = androidx.compose.ui.geometry.Offset(x, size.height - h), size = androidx.compose.ui.geometry.Size(barW, h.coerceAtLeast(4f)), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f))
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val min = totalSeconds / 60
    val sec = totalSeconds % 60
    return "%02d:%02d".format(min, sec)
}

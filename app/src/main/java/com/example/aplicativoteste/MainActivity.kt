package com.example.aplicativoteste

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.MediaController
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aplicativoteste.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var mediaPlayer: MediaPlayer? = null
    private val mediaList = mutableListOf<File>()

    // 🔹 PERMISSION LAUNCHER
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                loadMediaFiles()
            } else {
                binding.txtNowPlaying.text = "Permissão negada"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        checkPermission()
    }

    // 🔹 PERMISSÕES CORRETAS ANDROID 13+
    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            val audioGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            val videoGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED

            if (!audioGranted) {
                permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                return
            }

            if (!videoGranted) {
                permissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
                return
            }

            loadMediaFiles()

        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                loadMediaFiles()
            }
        }
    }

    // 🔹 CARREGA MP3 / WAV / MP4
    private fun loadMediaFiles() {
        val dir = File("/storage/emulated/0/MUSICAS")

        if (!dir.exists() || !dir.isDirectory) {
            binding.txtNowPlaying.text = "Pasta /MUSICAS não encontrada"
            return
        }

        mediaList.clear()

        dir.listFiles()?.filter {
            it.isFile && it.extension.lowercase() in listOf(
                "mp3", "wav", "mp4"
            )
        }?.let {
            mediaList.addAll(it)
        }

        if (mediaList.isEmpty()) {
            binding.txtNowPlaying.text = "Nenhuma mídia encontrada"
        }

        binding.recyclerView.adapter =
            MusicAdapter(mediaList) { file ->
                playMedia(file)
            }
    }

    // 🔹 TOCA ÁUDIO OU VÍDEO
    private fun playMedia(file: File) {
        mediaPlayer?.release()
        mediaPlayer = null

        val ext = file.extension.lowercase()

        if (ext == "mp4") {
            playVideo(file)
        } else {
            playAudio(file)
        }
    }

    private fun playAudio(file: File) {
        binding.videoView.stopPlayback()
        binding.videoView.visibility = android.view.View.GONE

        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            start()
        }

        binding.txtNowPlaying.text = "Tocando áudio: ${file.name}"
    }

    private fun playVideo(file: File) {
        mediaPlayer?.release()
        mediaPlayer = null

        binding.videoView.visibility = android.view.View.VISIBLE

        val controller = MediaController(this)
        controller.setAnchorView(binding.videoView)

        binding.videoView.setMediaController(controller)
        binding.videoView.setVideoURI(Uri.fromFile(file))
        binding.videoView.start()

        binding.txtNowPlaying.text = "Reproduzindo vídeo: ${file.name}"
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        binding.videoView.stopPlayback()
    }
}

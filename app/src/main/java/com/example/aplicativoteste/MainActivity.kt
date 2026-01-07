package com.example.aplicativoteste

import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aplicativoteste.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mediaList = mutableListOf<File>()
    private var mediaPlayer: MediaPlayer? = null
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        loadMedia()

        binding.btnPlay.setOnClickListener { playCurrent() }

        binding.btnPause.setOnClickListener {
            mediaPlayer?.pause()
            binding.videoView.pause()
        }

        binding.btnNext.setOnClickListener {
            if (mediaList.isNotEmpty()) {
                currentIndex = (currentIndex + 1) % mediaList.size
                playCurrent()
            }
        }
    }

    private fun loadMedia() {
        val dir = File("/storage/emulated/0/MUSICAS")

        if (!dir.exists() || !dir.isDirectory) {
            binding.txtNowPlaying.text = "Pasta /MUSICAS não encontrada"
            return
        }

        mediaList.clear()

        dir.listFiles()?.filter {
            it.isFile && it.extension.lowercase() in listOf(
                "mp3", "wav", "mp4", "mkv", "3gp", "webm"
            )
        }?.let {
            mediaList.addAll(it)
        }

        binding.recyclerView.adapter = MusicAdapter(mediaList) { file ->
            currentIndex = mediaList.indexOf(file)
            playCurrent()
        }
    }

    private fun playCurrent() {
        if (mediaList.isEmpty()) return

        val file = mediaList[currentIndex]
        binding.txtNowPlaying.text = file.name

        stopAll()

        if (isVideo(file)) {
            playVideo(file)
        } else {
            playAudio(file)
        }
    }

    private fun playAudio(file: File) {
        binding.videoView.visibility = android.view.View.GONE

        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            start()
        }
    }

    private fun playVideo(file: File) {
        binding.videoView.visibility = android.view.View.VISIBLE

        val controller = MediaController(this)
        controller.setAnchorView(binding.videoView)

        binding.videoView.setMediaController(controller)
        binding.videoView.setVideoURI(Uri.fromFile(file))
        binding.videoView.start()
    }

    private fun stopAll() {
        mediaPlayer?.release()
        mediaPlayer = null
        binding.videoView.stopPlayback()
    }

    private fun isVideo(file: File): Boolean {
        return file.extension.lowercase() in listOf(
            "mp4", "mkv", "3gp", "webm"
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAll()
    }
}

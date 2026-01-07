package com.example.aplicativoteste

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicativoteste.databinding.ItemMusicBinding
import java.io.File

class MusicAdapter(
    private val files: List<File>,
    private val onClick: (File) -> Unit
) : RecyclerView.Adapter<MusicAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemMusicBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(file: File) {
            binding.txtMusicName.text = file.name

            if (isVideo(file)) {
                val thumb = getVideoThumbnail(file)
                if (thumb != null) {
                    binding.imgThumb.setImageBitmap(thumb)
                } else {
                    binding.imgThumb.setImageResource(
                        android.R.drawable.ic_media_play
                    )
                }
            } else {
                binding.imgThumb.setImageResource(
                    android.R.drawable.ic_media_play
                )
            }

            binding.root.setOnClickListener {
                onClick(file)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMusicBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(files[position])
    }

    override fun getItemCount(): Int = files.size

    private fun isVideo(file: File): Boolean {
        return file.extension.lowercase() in listOf(
            "mp4", "mkv", "3gp", "webm"
        )
    }

    private fun getVideoThumbnail(file: File): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val bitmap = retriever.frameAtTime
            retriever.release()
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}

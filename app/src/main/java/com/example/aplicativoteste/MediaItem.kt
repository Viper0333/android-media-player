package com.example.aplicativoteste

import android.graphics.Bitmap
import java.io.File

data class MediaItem(
    val file: File,
    val isVideo: Boolean,
    val thumbnail: Bitmap?
)

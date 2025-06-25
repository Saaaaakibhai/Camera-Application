package com.example.cameraapplication.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File

object ImageSaver {
    fun saveImage(context: Context, filename: String): Uri {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "CameraApplication")
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, "$filename.jpg")
        // Save image logic here
        return Uri.fromFile(file)
    }
    enum class AspectRatioOption {
        RATIO_3_2,
        RATIO_4_3,
        RATIO_16_9
    }
}
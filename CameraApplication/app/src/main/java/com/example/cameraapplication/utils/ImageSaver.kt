package com.example.cameraapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.util.*

object ImageSaver {
    /**
     * Saves an image to a custom directory with compression.
     *
     * @param context The context of the application.
     * @param inputFile The original image file to be compressed and saved.
     * @param quality The compression quality (0-100).
     * @return The URI of the saved compressed image.
     */
    fun saveCompressedImage(context: Context, inputFile: File, quality: Int = 85): Uri {
        // Get or create the custom directory
        val customDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "CameraApplication"
        )
        if (!customDir.exists()) customDir.mkdirs()

        // Define a unique filename for the compressed image
        val compressedFile = File(customDir, "${UUID.randomUUID()}.jpg")

        // Load the original bitmap
        val bitmap = BitmapFactory.decodeFile(inputFile.absolutePath)

        // Save the compressed bitmap to the file
        FileOutputStream(compressedFile).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.flush()
        }

        // Return the URI of the saved file
        return Uri.fromFile(compressedFile)
    }
}

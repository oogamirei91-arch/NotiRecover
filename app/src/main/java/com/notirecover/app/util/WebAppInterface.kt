package com.notirecover.app.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

class WebAppInterface(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onMediaDownloaded(base64Data: String, mimeType: String, filename: String) {
        try {
            val cleanBase64 = base64Data.substringAfter("base64,")
            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)

            val isVideo = mimeType.contains("video") || filename.endsWith(".mp4")
            val targetName = "Status_${System.currentTimeMillis()}_${filename.ifBlank { if (isVideo) "video.mp4" else "image.jpg" }}"

            val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, targetName)
                    put(MediaStore.MediaColumns.MIME_TYPE, if (isVideo) "video/mp4" else "image/jpeg")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        if (isVideo) Environment.DIRECTORY_MOVIES + "/ChatRestore" else Environment.DIRECTORY_PICTURES + "/ChatRestore"
                    )
                }

                val collection = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val itemUri = context.contentResolver.insert(collection, contentValues)

                if (itemUri != null) {
                    context.contentResolver.openOutputStream(itemUri)?.use { out ->
                        out.write(bytes)
                    }
                    true
                } else false
            } else {
                val destDir = File(
                    Environment.getExternalStoragePublicDirectory(
                        if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                    ),
                    "ChatRestore"
                )
                if (!destDir.exists()) destDir.mkdirs()
                val destFile = File(destDir, targetName)
                FileOutputStream(destFile).use { out ->
                    out.write(bytes)
                }
                true
            }

            handler.post {
                if (success) {
                    Toast.makeText(context, "Status Berhasil Diunduh ke Galeri! 🎉", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Gagal menyimpan file", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("WebAppInterface", "Error decoding and saving web media", e)
            handler.post {
                Toast.makeText(context, "Terjadi kesalahan saat mengunduh status", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

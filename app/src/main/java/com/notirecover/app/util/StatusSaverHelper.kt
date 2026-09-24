package com.notirecover.app.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class StatusItem(
    val file: File,
    val isVideo: Boolean,
    val lastModified: Long = file.lastModified()
)

object StatusSaverHelper {

    private const val TAG = "StatusSaverHelper"

    // Path folder status WhatsApp standar di Android
    private val STATUS_PATHS = listOf(
        // Android 11+ Scoped Media Path
        "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        "/storage/emulated/0/Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses",
        // Android 10 ke bawah
        "/storage/emulated/0/WhatsApp/Media/.Statuses",
        "/storage/emulated/0/WhatsApp Business/Media/.Statuses"
    )

    /**
     * Mengambil daftar semua file foto & video status WhatsApp yang sedang aktif.
     */
    fun getActiveStatuses(): List<StatusItem> {
        val result = mutableListOf<StatusItem>()

        for (path in STATUS_PATHS) {
            val dir = File(path)
            if (dir.exists() && dir.isDirectory) {
                val files = dir.listFiles { file ->
                    val name = file.name.lowercase()
                    (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".mp4")) &&
                            !name.startsWith(".nomedia")
                }
                if (files != null) {
                    for (file in files) {
                        val isVideo = file.name.lowercase().endsWith(".mp4")
                        result.add(StatusItem(file = file, isVideo = isVideo))
                    }
                }
            }
        }

        return result.sortedByDescending { it.lastModified }
    }

    /**
     * Menyimpan file foto atau video status ke Galeri HP (Pictures/ChatRestore).
     */
    fun saveStatusToGallery(context: Context, statusItem: StatusItem): Boolean {
        val sourceFile = statusItem.file
        if (!sourceFile.exists()) return false

        return try {
            val fileName = "Status_${System.currentTimeMillis()}_${sourceFile.name}"
            val mimeType = if (statusItem.isVideo) "video/mp4" else "image/jpeg"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        if (statusItem.isVideo) Environment.DIRECTORY_MOVIES + "/ChatRestore" else Environment.DIRECTORY_PICTURES + "/ChatRestore"
                    )
                }

                val collectionUri = if (statusItem.isVideo) {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }

                val itemUri = context.contentResolver.insert(collectionUri, contentValues)
                if (itemUri != null) {
                    context.contentResolver.openOutputStream(itemUri)?.use { outputStream ->
                        FileInputStream(sourceFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    true
                } else {
                    false
                }
            } else {
                @Suppress("DEPRECATION")
                val destDir = File(
                    Environment.getExternalStoragePublicDirectory(
                        if (statusItem.isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                    ),
                    "ChatRestore"
                )
                if (!destDir.exists()) destDir.mkdirs()

                val destFile = File(destDir, fileName)
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menyimpan status ke galeri", e)
            false
        }
    }
}

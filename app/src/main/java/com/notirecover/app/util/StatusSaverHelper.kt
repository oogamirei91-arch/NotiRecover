package com.notirecover.app.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
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

    /**
     * Memeriksa apakah izin akses penyimpanan (All Files Access) sudah diberikan oleh user.
     */
    fun isStoragePermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    /**
     * Membuka halaman pengaturan sistem agar user dapat mengizinkan "Akses Semua File".
     */
    fun requestStoragePermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }
    }

    /**
     * Mengambil daftar semua file foto & video status WhatsApp yang sedang aktif di HP.
     */
    fun getActiveStatuses(): List<StatusItem> {
        val result = mutableListOf<StatusItem>()

        val storageRoot = Environment.getExternalStorageDirectory()

        val possiblePaths = listOf(
            // WhatsApp Biasa (Android 11+)
            File(storageRoot, "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
            // WhatsApp Business (Android 11+)
            File(storageRoot, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"),
            // WhatsApp Biasa (Android 10 ke bawah)
            File(storageRoot, "WhatsApp/Media/.Statuses"),
            // WhatsApp Business (Android 10 ke bawah)
            File(storageRoot, "WhatsApp Business/Media/.Statuses"),
            // Fallback path
            File("/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
            File("/storage/emulated/0/WhatsApp/Media/.Statuses")
        )

        for (dir in possiblePaths) {
            if (dir.exists() && dir.isDirectory) {
                val files = dir.listFiles()
                if (files != null) {
                    for (file in files) {
                        val name = file.name.lowercase()
                        if ((name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".mp4")) &&
                            !name.startsWith(".nomedia") && file.length() > 0
                        ) {
                            val isVideo = name.endsWith(".mp4")
                            // Hindari duplikat file
                            if (result.none { it.file.absolutePath == file.absolutePath }) {
                                result.add(StatusItem(file = file, isVideo = isVideo))
                            }
                        }
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

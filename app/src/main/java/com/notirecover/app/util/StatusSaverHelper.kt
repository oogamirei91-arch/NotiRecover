package com.notirecover.app.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

data class StatusMediaItem(
    val uri: Uri,
    val name: String,
    val isVideo: Boolean,
    val lastModified: Long = System.currentTimeMillis(),
    val file: File? = null
)

object StatusSaverHelper {

    private const val TAG = "StatusSaverHelper"
    private const val PREFS_KEY_TREE_URI = "saved_status_tree_uri"

    /**
     * Menyimpan izin Tree URI yang dipilih pengguna agar tidak perlu meminta izin ulang.
     */
    fun saveTreeUri(context: Context, uri: Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            context.getSharedPreferences("notirecover_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString(PREFS_KEY_TREE_URI, uri.toString())
                .apply()
            Log.i(TAG, "Berhasil menyimpan persistable Tree URI: $uri")
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menyimpan Tree URI", e)
        }
    }

    fun getSavedTreeUri(context: Context): Uri? {
        val uriStr = context.getSharedPreferences("notirecover_prefs", Context.MODE_PRIVATE)
            .getString(PREFS_KEY_TREE_URI, null)
        return if (!uriStr.isNullOrBlank()) Uri.parse(uriStr) else null
    }

    /**
     * Membuat Intent untuk membuka pemilih folder SAF langsung ke folder WhatsApp Statuses.
     */
    fun createOpenDocumentTreeIntent(): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val whatsappUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses")
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, whatsappUri)
        }
        return intent
    }

    /**
     * Mengambil daftar foto & video status menggunakan gabungan SAF DocumentFile dan Direct Storage.
     */
    fun getAllStatuses(context: Context): List<StatusMediaItem> {
        val result = mutableListOf<StatusMediaItem>()

        // 1. Coba baca via SAF DocumentFile jika pengguna sudah memilih folder
        val treeUri = getSavedTreeUri(context)
        if (treeUri != null) {
            try {
                val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                if (rootDoc != null && rootDoc.exists() && rootDoc.isDirectory) {
                    for (file in rootDoc.listFiles()) {
                        val name = file.name.orEmpty().lowercase()
                        if ((name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".mp4")) &&
                            !name.startsWith(".nomedia") && file.length() > 0
                        ) {
                            val isVideo = name.endsWith(".mp4")
                            result.add(
                                StatusMediaItem(
                                    uri = file.uri,
                                    name = file.name ?: "status",
                                    isVideo = isVideo,
                                    lastModified = file.lastModified()
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saat membaca DocumentFile", e)
            }
        }

        // 2. Fallback: Scan direct file paths (untuk Android 10 atau HP yang mengizinkan)
        try {
            val storageRoot = Environment.getExternalStorageDirectory()
            val possiblePaths = listOf(
                File(storageRoot, "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
                File(storageRoot, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"),
                File(storageRoot, "WhatsApp/Media/.Statuses"),
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
                                val fileUri = Uri.fromFile(file)
                                if (result.none { it.name == file.name }) {
                                    result.add(
                                        StatusMediaItem(
                                            uri = fileUri,
                                            name = file.name,
                                            isVideo = isVideo,
                                            lastModified = file.lastModified(),
                                            file = file
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saat direct scan", e)
        }

        return result.sortedByDescending { it.lastModified }
    }

    /**
     * Menyimpan foto/video status ke Galeri HP (Pictures/ChatRestore).
     */
    fun saveStatusToGallery(context: Context, statusItem: StatusMediaItem): Boolean {
        return try {
            val fileName = "ChatRestore_${System.currentTimeMillis()}_${statusItem.name}"
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
                        val inputStream: InputStream? = if (statusItem.file != null) {
                            FileInputStream(statusItem.file)
                        } else {
                            context.contentResolver.openInputStream(statusItem.uri)
                        }
                        inputStream?.use { input ->
                            input.copyTo(outputStream)
                        }
                    }
                    true
                } else {
                    false
                }
            } else {
                val destDir = File(
                    Environment.getExternalStoragePublicDirectory(
                        if (statusItem.isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                    ),
                    "ChatRestore"
                )
                if (!destDir.exists()) destDir.mkdirs()

                val destFile = File(destDir, fileName)
                val inputStream: InputStream? = if (statusItem.file != null) {
                    FileInputStream(statusItem.file)
                } else {
                    context.contentResolver.openInputStream(statusItem.uri)
                }
                FileOutputStream(destFile).use { output ->
                    inputStream?.use { input ->
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

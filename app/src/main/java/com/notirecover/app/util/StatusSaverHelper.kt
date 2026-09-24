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
    val file: File? = null,
    val sourceApp: String = "WhatsApp",
    val sizeBytes: Long = 0L
) {
    val formattedDate: String
        get() {
            return try {
                val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale("id", "ID"))
                sdf.format(java.util.Date(lastModified))
            } catch (e: Exception) {
                "-"
            }
        }

    val formattedSize: String
        get() {
            if (sizeBytes <= 0L) return if (isVideo) "Video" else "Foto"
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1.0) {
                String.format(java.util.Locale.US, "%.1f MB", mb)
            } else {
                String.format(java.util.Locale.US, "%.0f KB", kb)
            }
        }
}

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
     * Memindai DocumentFile secara rekursif hingga kedalaman 4 folder
     * agar file di dalam .Statuses selalu ditemukan meskipun user memilih folder induk.
     */
    private fun scanDocTreeForStatuses(doc: DocumentFile, depth: Int = 0): List<StatusMediaItem> {
        if (depth > 4) return emptyList()
        val list = mutableListOf<StatusMediaItem>()
        try {
            val files = doc.listFiles()
            for (file in files) {
                val name = file.name.orEmpty()
                if (file.isDirectory) {
                    // Cari masuk ke subfolder (misal: WhatsApp, Media, .Statuses)
                    list.addAll(scanDocTreeForStatuses(file, depth + 1))
                } else if (file.isFile && file.length() > 0 && !name.startsWith(".nomedia")) {
                    val lower = name.lowercase()
                    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".mp4")) {
                        val isVideo = lower.endsWith(".mp4")
                        val uriStr = file.uri.toString().lowercase()
                        val sourceApp = when {
                            uriStr.contains("w4b") || uriStr.contains("business") -> "WhatsApp Business"
                            uriStr.contains("999") || uriStr.contains("10") || uriStr.contains("dual") -> "WhatsApp (Dual App)"
                            else -> "WhatsApp"
                        }
                        list.add(
                            StatusMediaItem(
                                uri = file.uri,
                                name = name,
                                isVideo = isVideo,
                                lastModified = file.lastModified(),
                                sourceApp = sourceApp,
                                sizeBytes = file.length()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning doc at depth $depth", e)
        }
        return list
    }

    /**
     * Mengambil daftar foto & video status menggunakan gabungan SAF DocumentFile dan Direct Storage.
     */
    fun getAllStatuses(context: Context): List<StatusMediaItem> {
        val result = mutableListOf<StatusMediaItem>()

        // 1. Baca via SAF DocumentFile jika pengguna sudah menghubungkan folder
        val treeUri = getSavedTreeUri(context)
        if (treeUri != null) {
            try {
                val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                if (rootDoc != null && rootDoc.exists()) {
                    val safItems = scanDocTreeForStatuses(rootDoc)
                    safItems.forEach { item ->
                        if (result.none { it.name == item.name }) {
                            result.add(item)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saat membaca DocumentFile", e)
            }
        }

        // 2. Scan direct file paths (Mendukung Android biasa, Dual App / Clone, & WA Business)
        try {
            val storageRoots = listOf(
                Pair(Environment.getExternalStorageDirectory(), "Utama"),
                Pair(File("/storage/emulated/0"), "Utama"),
                Pair(File("/storage/emulated/999"), "Dual App (Xiaomi)"),
                Pair(File("/storage/emulated/10"), "Dual Messenger (Samsung)")
            )

            val relativePaths = listOf(
                Pair("Android/media/com.whatsapp/WhatsApp/Media/.Statuses", "WhatsApp"),
                Pair("Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses", "WhatsApp Business"),
                Pair("WhatsApp/Media/.Statuses", "WhatsApp"),
                Pair("WhatsApp Business/Media/.Statuses", "WhatsApp Business")
            )

            for ((root, rootLabel) in storageRoots) {
                if (root.exists()) {
                    for ((rel, appLabel) in relativePaths) {
                        val dir = File(root, rel)
                        if (dir.exists() && dir.isDirectory) {
                            val detectedSource = if (rootLabel != "Utama") "$appLabel ($rootLabel)" else appLabel
                            dir.listFiles()?.forEach { file ->
                                val name = file.name.lowercase()
                                if ((name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".mp4")) &&
                                    !name.startsWith(".nomedia") && file.length() > 0
                                ) {
                                    val isVideo = name.endsWith(".mp4")
                                    if (result.none { it.name == file.name }) {
                                        result.add(
                                            StatusMediaItem(
                                                uri = Uri.fromFile(file),
                                                name = file.name,
                                                isVideo = isVideo,
                                                lastModified = file.lastModified(),
                                                file = file,
                                                sourceApp = detectedSource,
                                                sizeBytes = file.length()
                                            )
                                        )
                                    }
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

    /**
     * Mengambil daftar status yang telah diunduh/disimpan pengguna ke Galeri ChatRestore.
     */
    fun getDownloadedStatuses(context: Context): List<StatusMediaItem> {
        val result = mutableListOf<StatusMediaItem>()

        // 1. Scan direct folder Pictures/ChatRestore & Movies/ChatRestore
        try {
            val picDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ChatRestore")
            val movieDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "ChatRestore")

            val scanDirs = listOf(picDir, movieDir)
            for (dir in scanDirs) {
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles()?.forEach { file ->
                        val name = file.name.lowercase()
                        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".mp4")) {
                            val isVideo = name.endsWith(".mp4")
                            result.add(
                                StatusMediaItem(
                                    uri = Uri.fromFile(file),
                                    name = file.name,
                                    isVideo = isVideo,
                                    lastModified = file.lastModified(),
                                    file = file,
                                    sourceApp = "Galeri ChatRestore",
                                    sizeBytes = file.length()
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scan downloaded statuses dir", e)
        }

        // 2. Query MediaStore for ChatRestore files
        try {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.MediaColumns.SIZE
            )

            // Images
            val imageCursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?",
                arrayOf("%ChatRestore%"),
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )
            imageCursor?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol)
                    val date = cursor.getLong(dateCol) * 1000
                    val size = cursor.getLong(sizeCol)
                    val contentUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                    if (result.none { it.name == name }) {
                        result.add(
                            StatusMediaItem(
                                uri = contentUri,
                                name = name,
                                isVideo = false,
                                lastModified = date,
                                sourceApp = "Galeri ChatRestore",
                                sizeBytes = size
                            )
                        )
                    }
                }
            }

            // Videos
            val videoCursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?",
                arrayOf("%ChatRestore%"),
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )
            videoCursor?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol)
                    val date = cursor.getLong(dateCol) * 1000
                    val size = cursor.getLong(sizeCol)
                    val contentUri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                    if (result.none { it.name == name }) {
                        result.add(
                            StatusMediaItem(
                                uri = contentUri,
                                name = name,
                                isVideo = true,
                                lastModified = date,
                                sourceApp = "Galeri ChatRestore",
                                sizeBytes = size
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore for ChatRestore media", e)
        }

        return result.sortedByDescending { it.lastModified }
    }

    /**
     * Menghapus file status yang tersimpan di memori lokal.
     */
    fun deleteStatus(context: Context, statusItem: StatusMediaItem): Boolean {
        var deleted = false
        try {
            // 1. Delete direct file
            if (statusItem.file != null && statusItem.file.exists()) {
                deleted = statusItem.file.delete()
            }

            // 2. Delete via SAF DocumentFile
            if (!deleted && statusItem.uri.scheme == "content") {
                try {
                    val doc = DocumentFile.fromSingleUri(context, statusItem.uri)
                    if (doc != null && doc.exists()) {
                        deleted = doc.delete()
                    }
                } catch (ignored: Exception) {}
            }

            // 3. Delete via ContentResolver
            if (!deleted && statusItem.uri.scheme == "content") {
                try {
                    val rows = context.contentResolver.delete(statusItem.uri, null, null)
                    deleted = rows > 0
                } catch (ignored: Exception) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting status file", e)
        }
        return deleted
    }
}

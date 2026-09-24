package com.notirecover.app.util

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object BackupHelper {

    private const val TAG = "BackupHelper"
    private const val DB_NAME = "notirecover_db"
    private const val BACKUP_DIR_NAME = "ChatRestore/backup"
    private const val BACKUP_FILE_NAME = "chatrestore_backup.db"

    /**
     * Memulihkan database SQLite dari penyimpanan eksternal jika aplikasi baru di-install ulang.
     */
    fun restoreDatabaseIfAvailable(context: Context): Boolean {
        try {
            val internalDbFile = context.getDatabasePath(DB_NAME)
            // Jika database internal belum ada atau ukurannya 0, coba pulihkan dari backup eksternal
            if (!internalDbFile.exists() || internalDbFile.length() <= 0) {
                val backupFile = getPersistentBackupFile()
                if (backupFile != null && backupFile.exists() && backupFile.length() > 0) {
                    internalDbFile.parentFile?.mkdirs()
                    FileInputStream(backupFile).use { input ->
                        FileOutputStream(internalDbFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.i(TAG, "Berhasil memulihkan riwayat database SQLite dari backup eksternal!")
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memulihkan database", e)
        }
        return false
    }

    /**
     * Mencadangkan database SQLite ke folder publik Documents/ChatRestore agar tidak hilang saat uninstall.
     */
    fun backupDatabase(context: Context) {
        try {
            val internalDbFile = context.getDatabasePath(DB_NAME)
            if (internalDbFile.exists() && internalDbFile.length() > 0) {
                val backupFile = getPersistentBackupFile()
                if (backupFile != null) {
                    backupFile.parentFile?.mkdirs()
                    FileInputStream(internalDbFile).use { input ->
                        FileOutputStream(backupFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d(TAG, "Database SQLite berhasil dicadangkan ke: ${backupFile.absolutePath}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mencadangkan database", e)
        }
    }

    private fun getPersistentBackupFile(): File? {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val backupDir = File(documentsDir, BACKUP_DIR_NAME)
        return File(backupDir, BACKUP_FILE_NAME)
    }
}

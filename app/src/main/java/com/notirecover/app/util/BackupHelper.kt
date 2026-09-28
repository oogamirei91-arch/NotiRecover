package com.notirecover.app.util

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import com.notirecover.app.data.AppDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupHelper {

    private const val TAG = "BackupHelper"
    private const val DB_NAME = "notirecover_db"
    private const val BACKUP_DIR_NAME = "ChatRestore/backup"
    private const val BACKUP_FILE_NAME = "chatrestore_backup.db"

    /**
     * Memeriksa apakah aplikasi memiliki izin untuk mengakses folder publik di memori HP.
     */
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    /**
     * Memulihkan database SQLite jika database internal belum ada atau kosong.
     */
    fun restoreDatabaseIfAvailable(context: Context): Boolean {
        try {
            val internalDbFile = context.getDatabasePath(DB_NAME)
            if (!internalDbFile.exists() || internalDbFile.length() <= 0) {
                return restoreDatabase(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking internal db for auto-restore", e)
        }
        return false
    }

    /**
     * Memulihkan database SQLite dari penyimpanan eksternal.
     * Dapat dipanggil saat startup atau dipanggil manual oleh pengguna dari menu Pengaturan.
     */
    fun restoreDatabase(context: Context): Boolean {
        try {
            val backupFile = getPersistentBackupFile()
            if (backupFile == null || !backupFile.exists() || backupFile.length() <= 0) {
                Log.w(TAG, "File backup eksternal tidak ditemukan atau kosong")
                return false
            }

            val internalDbFile = context.getDatabasePath(DB_NAME)
            internalDbFile.parentFile?.mkdirs()

            // Salin file backup ke database internal
            FileInputStream(backupFile).use { input ->
                FileOutputStream(internalDbFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Hapus file -wal dan -shm lama jika ada agar SQLite memuat data baru secara konsisten
            val walFile = File(internalDbFile.parentFile, "$DB_NAME-wal")
            val shmFile = File(internalDbFile.parentFile, "$DB_NAME-shm")
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()

            Log.i(TAG, "Berhasil memulihkan database dari backup eksternal!")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memulihkan database", e)
            return false
        }
    }

    /**
     * Mencadangkan database SQLite ke folder publik Documents/ChatRestore.
     * Melakukan checkpoint WAL terlebih dahulu agar transaksi terbaru tersimpan utuh di file utama.
     */
    fun backupDatabase(context: Context): Boolean {
        try {
            // Jalankan SQLite Checkpoint WAL agar semua data tersimpan ke file db utama
            try {
                val db = AppDatabase.getDatabase(context)
                val cursor = db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)")
                cursor.close()
            } catch (e: Exception) {
                Log.w(TAG, "Gagal menjalankan WAL checkpoint: ${e.message}")
            }

            val internalDbFile = context.getDatabasePath(DB_NAME)
            if (!internalDbFile.exists() || internalDbFile.length() <= 0) {
                Log.w(TAG, "Database internal belum berisi data")
                return false
            }

            val backupFile = getPersistentBackupFile() ?: return false
            backupFile.parentFile?.mkdirs()

            FileInputStream(internalDbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }

            Log.i(TAG, "Database SQLite berhasil dicadangkan ke: ${backupFile.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mencadangkan database", e)
            return false
        }
    }

    /**
     * Mendapatkan informasi status file backup yang ada (tanggal & ukuran).
     */
    fun getBackupStatusInfo(): String? {
        val file = getPersistentBackupFile()
        if (file != null && file.exists() && file.length() > 0) {
            val dateStr = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
            val sizeKb = file.length() / 1024
            return "Tersedia ($dateStr, $sizeKb KB)"
        }
        return null
    }

    private fun getPersistentBackupFile(): File? {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val backupDir = File(documentsDir, BACKUP_DIR_NAME)
        return File(backupDir, BACKUP_FILE_NAME)
    }
}

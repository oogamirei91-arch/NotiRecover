package com.notirecover.app.util

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object MediaHelper {

    private const val TAG = "MediaHelper"
    private const val MEDIA_FOLDER_NAME = "recovered_media"

    /**
     * Mengekstrak Bitmap gambar dari notifikasi (EXTRA_PICTURE atau EXTRA_LARGE_ICON)
     * dan menyimpannya ke folder privat internal aplikasi.
     * @return Path file lokal (misal: /data/user/0/.../recovered_media/img_12345.jpg)
     */
    fun saveNotificationMedia(context: Context, notification: Notification): String? {
        val extras = notification.extras ?: return null

        val bitmap = extractBitmap(context, extras, notification) ?: return null

        return try {
            val mediaDir = File(context.filesDir, MEDIA_FOLDER_NAME)
            if (!mediaDir.exists()) {
                mediaDir.mkdirs()
            }

            val fileName = "img_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"
            val destFile = File(mediaDir, fileName)

            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            Log.i(TAG, "Berhasil menyimpan media ke: ${destFile.absolutePath}")
            destFile.absolutePath
        } catch (e: IOException) {
            Log.e(TAG, "Gagal menyimpan media dari notifikasi", e)
            null
        }
    }

    private fun extractBitmap(context: Context, extras: Bundle, notification: Notification): Bitmap? {
        // 1. Coba ambil dari EXTRA_PICTURE (BigPictureStyle / Kiriman Foto Penuh)
        val picture = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(Notification.EXTRA_PICTURE, Bitmap::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable(Notification.EXTRA_PICTURE) as? Bitmap
        }
        if (picture != null) return picture

        // 2. Coba ambil dari EXTRA_PICTURE_ICON (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val pictureIcon = extras.getParcelable(Notification.EXTRA_PICTURE_ICON, Icon::class.java)
            if (pictureIcon != null) {
                val drawable = pictureIcon.loadDrawable(context)
                if (drawable is BitmapDrawable) {
                    return drawable.bitmap
                }
            }
        }

        // 3. Coba ambil dari EXTRA_LARGE_ICON jika mengandung preview gambar (bukan sekadar avatar profil)
        val largeIcon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(Notification.EXTRA_LARGE_ICON, Bitmap::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable(Notification.EXTRA_LARGE_ICON) as? Bitmap
        }
        if (largeIcon != null && largeIcon.width > 200 && largeIcon.height > 200) {
            return largeIcon
        }

        return null
    }
}

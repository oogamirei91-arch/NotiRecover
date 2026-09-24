package com.notirecover.app.util

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object MediaHelper {

    private const val TAG = "MediaHelper"
    private const val MEDIA_FOLDER_NAME = "recovered_media"

    /**
     * Mengekstrak dan menyimpan foto/media dari berbagai jenis payload notifikasi Android modern.
     */
    fun saveNotificationMedia(context: Context, notification: Notification): String? {
        val extras = notification.extras ?: return null

        val bitmap = extractBitmapFromAllSources(context, extras, notification) ?: return null

        return try {
            val mediaDir = File(context.filesDir, MEDIA_FOLDER_NAME)
            if (!mediaDir.exists()) {
                mediaDir.mkdirs()
            }

            val fileName = "img_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"
            val destFile = File(mediaDir, fileName)

            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }

            Log.i(TAG, "Foto WhatsApp berhasil disimpan ke: ${destFile.absolutePath}")
            destFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menulis file foto", e)
            null
        }
    }

    private fun extractBitmapFromAllSources(
        context: Context,
        extras: Bundle,
        notification: Notification
    ): Bitmap? {
        // 1. Ekstrak dari MessagingStyle Data URI (Format WhatsApp & Telegram Terbaru)
        try {
            val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
            if (messagingStyle != null) {
                for (message in messagingStyle.messages) {
                    val dataUri = message.dataUri
                    val mimeType = message.dataMimeType
                    if (dataUri != null && (mimeType == null || mimeType.startsWith("image/"))) {
                        val bitmap = getBitmapFromUri(context, dataUri)
                        if (bitmap != null) {
                            Log.d(TAG, "Berhasil ekstrak dari MessagingStyle Data URI")
                            return bitmap
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saat ekstrak MessagingStyle", e)
        }

        // 2. Ekstrak dari EXTRA_PICTURE (BigPictureStyle - Bitmap atau Icon)
        val pictureObj = extras.get(Notification.EXTRA_PICTURE)
        if (pictureObj is Bitmap) {
            return pictureObj
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && pictureObj is Icon) {
            val drawable = pictureObj.loadDrawable(context)
            val bmp = drawableToBitmap(drawable)
            if (bmp != null) return bmp
        }

        // 3. Ekstrak dari EXTRA_PICTURE_ICON (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val pictureIcon = extras.getParcelable(Notification.EXTRA_PICTURE_ICON, Icon::class.java)
            if (pictureIcon != null) {
                val drawable = pictureIcon.loadDrawable(context)
                val bmp = drawableToBitmap(drawable)
                if (bmp != null) return bmp
            }
        }

        // 4. Ekstrak dari LargeIcon jika ukurannya adalah gambar/foto
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val largeIcon = notification.getLargeIcon()
            if (largeIcon != null) {
                val drawable = largeIcon.loadDrawable(context)
                val bmp = drawableToBitmap(drawable)
                if (bmp != null && bmp.width > 200 && bmp.height > 200) {
                    return bmp
                }
            }
        } else {
            @Suppress("DEPRECATION")
            val largeIcon = extras.getParcelable(Notification.EXTRA_LARGE_ICON) as? Bitmap
            if (largeIcon != null && largeIcon.width > 200 && largeIcon.height > 200) {
                return largeIcon
            }
        }

        // 5. Cek Parcelable Messages Raw Array
        try {
            val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (messages != null) {
                for (item in messages) {
                    if (item is Bundle) {
                        val uriString = item.getString("uri")
                        if (!uriString.isNullOrBlank()) {
                            val bmp = getBitmapFromUri(context, Uri.parse(uriString))
                            if (bmp != null) return bmp
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing raw messages bundle", e)
        }

        return null
    }

    private fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use {
                BitmapFactory.decodeStream(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal membaca bitmap dari URI: $uri", e)
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable?): Bitmap? {
        if (drawable == null) return null
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }

        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 300
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 300

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}

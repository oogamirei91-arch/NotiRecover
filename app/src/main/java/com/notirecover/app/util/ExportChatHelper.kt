package com.notirecover.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.notirecover.app.data.model.ConversationEntity
import com.notirecover.app.data.model.MessageEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportChatHelper {

    fun exportChatAsHtml(
        context: Context,
        conversation: ConversationEntity,
        messages: List<MessageEntity>
    ) {
        try {
            val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val exportTime = dateFormat.format(Date())

            val sb = StringBuilder()
            sb.append("<!DOCTYPE html>\n<html>\n<head>\n")
            sb.append("<meta charset='utf-8'>\n")
            sb.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n")
            sb.append("<title>Riwayat Chat - ${conversation.chatTitle}</title>\n")
            sb.append("""
                <style>
                    body { font-family: 'Segoe UI', Helvetica, Arial, sans-serif; background: #f0f2f5; margin: 0; padding: 20px; color: #111b21; }
                    .container { max-width: 650px; margin: 0 auto; background: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
                    .header { background: #008069; color: #ffffff; padding: 20px; text-align: center; }
                    .header h1 { margin: 0; font-size: 20px; font-weight: 600; }
                    .header p { margin: 4px 0 0; font-size: 13px; opacity: 0.9; }
                    .chat-box { padding: 20px; display: flex; flex-direction: column; gap: 12px; background: #efeae2; min-height: 400px; }
                    .bubble { max-width: 80%; padding: 10px 14px; border-radius: 12px; position: relative; font-size: 14px; line-height: 1.4; word-wrap: break-word; }
                    .bubble-normal { background: #ffffff; align-self: flex-start; box-shadow: 0 1px 2px rgba(0,0,0,0.1); border-bottom-left-radius: 2px; }
                    .bubble-deleted { background: #fee2e2; border: 1px solid #f87171; align-self: flex-start; box-shadow: 0 1px 2px rgba(0,0,0,0.1); border-bottom-left-radius: 2px; }
                    .deleted-badge { display: inline-block; color: #dc2626; font-size: 11px; font-weight: bold; margin-bottom: 4px; }
                    .time { font-size: 10px; color: #667781; margin-top: 4px; text-align: right; }
                    .deleted-time { color: #b91c1c; }
                    .footer { text-align: center; padding: 14px; font-size: 12px; color: #8696a0; background: #ffffff; border-top: 1px solid #e9edef; }
                </style>
            """.trimIndent())
            sb.append("\n</head>\n<body>\n")
            sb.append("<div class='container'>\n")
            sb.append("<div class='header'>\n")
            sb.append("<h1>${conversation.chatTitle}</h1>\n")
            sb.append("<p>Diekspor oleh ChatRestore PRO pada $exportTime</p>\n")
            sb.append("</div>\n")
            sb.append("<div class='chat-box'>\n")

            for (msg in messages) {
                val timeStr = timeFormat.format(Date(msg.receivedAt))
                if (msg.isDeleted) {
                    val delTimeStr = msg.deletedAt?.let { " • Ditarik: ${timeFormat.format(Date(it))}" } ?: ""
                    sb.append("<div class='bubble bubble-deleted'>\n")
                    sb.append("<span class='deleted-badge'>⚠️ PESAN INI DIHAPUS OLEH PENGIRIM</span><br/>\n")
                    sb.append("<div>${escapeHtml(msg.messageText)}</div>\n")
                    sb.append("<div class='time deleted-time'>Diterima: $timeStr$delTimeStr</div>\n")
                    sb.append("</div>\n")
                } else {
                    sb.append("<div class='bubble bubble-normal'>\n")
                    sb.append("<div>${escapeHtml(msg.messageText)}</div>\n")
                    sb.append("<div class='time'>$timeStr</div>\n")
                    sb.append("</div>\n")
                }
            }

            sb.append("</div>\n")
            sb.append("<div class='footer'>Laporan Resmi Percakapan Pulih &bull; ChatRestore PRO</div>\n")
            sb.append("</div>\n</body>\n</html>")

            val safeTitle = conversation.chatTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val fileName = "Chat_${safeTitle}_${System.currentTimeMillis()}.html"
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val exportFile = File(exportDir, fileName)

            FileOutputStream(exportFile).use { out ->
                out.write(sb.toString().toByteArray(Charsets.UTF_8))
            }

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                exportFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/html"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Ekspor Chat - ${conversation.chatTitle}")
                putExtra(Intent.EXTRA_TEXT, "Berikut salinan riwayat chat ${conversation.chatTitle} yang diekspor dari ChatRestore PRO.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Bagikan / Simpan Riwayat Chat"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal mengekspor riwayat chat: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("\n", "<br/>")
    }
}

package com.notirecover.app.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notirecover.app.ui.theme.TextSecondaryLight
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectChatScreen() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var countryCode by remember { mutableStateOf("62") }
    var phoneNumber by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }

    val formattedNumber = remember(countryCode, phoneNumber) {
        var cleanNumber = phoneNumber.trim().replace(Regex("[^0-9]"), "")
        if (cleanNumber.startsWith("0")) {
            cleanNumber = cleanNumber.substring(1)
        }
        val cleanCountry = countryCode.trim().replace("+", "")
        "$cleanCountry$cleanNumber"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Direct WhatsApp",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Kirim Pesan Tanpa Simpan Kontak",
                            fontSize = 12.sp,
                            color = TextSecondaryLight
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF0FDF4),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🚀 Kirim Langsung Tanpa Simpan Nomor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF166534)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ketik nomor tujuan dan pesan Anda, lalu klik tombol untuk langsung membuka ruang obrolan di WhatsApp.",
                        fontSize = 12.sp,
                        color = Color(0xFF15803D)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Input Nomor Telepon dengan Kode Negara
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = "+$countryCode",
                    onValueChange = { newVal ->
                        countryCode = newVal.replace("+", "").filter { it.isDigit() }
                    },
                    label = { Text("Kode") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(90.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(10.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Nomor WhatsApp (Cth: 8123456789)") },
                    placeholder = { Text("8123456789") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Input Pesan Teks Opsional
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                label = { Text("Tulis Pesan (Opsional)") },
                placeholder = { Text("Halo, saya ingin bertanya...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Tombol Kirim ke WhatsApp Resmi
            Button(
                onClick = {
                    if (phoneNumber.isBlank()) {
                        Toast.makeText(context, "Silakan masukkan nomor telepon!", Toast.LENGTH_SHORT).show()
                    } else {
                        launchWhatsApp(context, formattedNumber, messageText, "com.whatsapp")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
            ) {
                Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Buka di WhatsApp",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tombol Kirim ke WhatsApp Business
            OutlinedButton(
                onClick = {
                    if (phoneNumber.isBlank()) {
                        Toast.makeText(context, "Silakan masukkan nomor telepon!", Toast.LENGTH_SHORT).show()
                    } else {
                        launchWhatsApp(context, formattedNumber, messageText, "com.whatsapp.w4b")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF128C7E))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Buka di WhatsApp Business",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFF128C7E)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tombol Salin Link WA.me
            TextButton(
                onClick = {
                    if (phoneNumber.isBlank()) {
                        Toast.makeText(context, "Silakan masukkan nomor telepon terlebih dahulu!", Toast.LENGTH_SHORT).show()
                    } else {
                        val encodedMsg = URLEncoder.encode(messageText, "UTF-8")
                        val link = "https://wa.me/$formattedNumber?text=$encodedMsg"
                        clipboardManager.setText(AnnotatedString(link))
                        Toast.makeText(context, "Link WhatsApp berhasil disalin!", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Salin Link Chat (wa.me)", fontSize = 13.sp)
            }
        }
    }
}

private fun launchWhatsApp(
    context: Context,
    fullNumber: String,
    message: String,
    targetPackage: String
) {
    try {
        val encodedMsg = URLEncoder.encode(message, "UTF-8")
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$fullNumber&text=$encodedMsg")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(targetPackage)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            val fallbackUri = Uri.parse("https://api.whatsapp.com/send?phone=$fullNumber&text=$encodedMsg")
            val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallbackIntent)
        } catch (ex: Exception) {
            Toast.makeText(context, "Aplikasi WhatsApp tidak ditemukan di HP ini", Toast.LENGTH_SHORT).show()
        }
    }
}

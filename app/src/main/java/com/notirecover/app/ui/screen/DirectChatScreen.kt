package com.notirecover.app.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.notirecover.app.NotiRecoverApp
import com.notirecover.app.data.model.DirectContactEntity
import com.notirecover.app.ui.theme.PrimaryBlue
import com.notirecover.app.ui.theme.TextSecondaryLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectChatScreen() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val app = context.applicationContext as NotiRecoverApp
    val directContactDao = app.database.directContactDao()
    val scope = rememberCoroutineScope()

    val contactsList by directContactDao.getAllDirectContacts()
        .collectAsState(initial = emptyList())

    var contactName by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("62") }
    var phoneNumber by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var contactToDelete by remember { mutableStateOf<DirectContactEntity?>(null) }

    val formattedNumber = remember(countryCode, phoneNumber) {
        var cleanNumber = phoneNumber.trim().replace(Regex("[^0-9]"), "")
        if (cleanNumber.startsWith("0")) {
            cleanNumber = cleanNumber.substring(1)
        }
        val cleanCountry = countryCode.trim().replace("+", "")
        "$cleanCountry$cleanNumber"
    }

    val filteredContacts = remember(contactsList, searchQuery) {
        if (searchQuery.isBlank()) {
            contactsList
        } else {
            contactsList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.phoneNumber.contains(searchQuery)
            }
        }
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
                            text = "Kirim Pesan & Simpan Kontak Cepat",
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
                        text = "🚀 Kirim Langsung Tanpa Simpan ke Kontak HP",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF166534)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ketik nomor dan pesan, kirim ke WhatsApp resmi/bisnis, atau simpan ke daftar database kontak cepat di bawah ini.",
                        fontSize = 12.sp,
                        color = Color(0xFF15803D)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Input Nama Kontak (Opsional untuk Database)
            OutlinedTextField(
                value = contactName,
                onValueChange = { contactName = it },
                label = { Text("Nama Kontak / Pelanggan (Opsional)") },
                placeholder = { Text("Cth: Pelanggan Kaos, Budi Laundry") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

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

            Spacer(modifier = Modifier.height(10.dp))

            // Input Pesan Teks Opsional
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                label = { Text("Tulis Pesan (Opsional)") },
                placeholder = { Text("Halo, saya ingin bertanya...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Template Pesan Cepat (Quick Templates)
            val templates = listOf(
                "Halo, apakah ini masih ada?",
                "Halo, saya ingin konfirmasi pesanan.",
                "Boleh minta share lokasi terkini?",
                "Halo, mohon info rinciannya ya."
            )
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(templates.size) { idx ->
                    val text = templates[idx]
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { messageText = text }
                    ) {
                        Text(
                            text = text,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Baris Tombol Aksi: Buka WA & Simpan Kontak
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tombol Buka WhatsApp
                Button(
                    onClick = {
                        if (phoneNumber.isBlank()) {
                            Toast.makeText(context, "Silakan masukkan nomor telepon!", Toast.LENGTH_SHORT).show()
                        } else {
                            launchWhatsApp(context, formattedNumber, messageText, "com.whatsapp")
                        }
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WhatsApp",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                // Tombol Simpan ke Database
                FilledTonalButton(
                    onClick = {
                        val cleanNum = phoneNumber.trim().replace(Regex("[^0-9]"), "")
                        if (cleanNum.isBlank()) {
                            Toast.makeText(context, "Ketik nomor telepon terlebih dahulu!", Toast.LENGTH_SHORT).show()
                        } else {
                            val savedName = if (contactName.isNotBlank()) {
                                contactName.trim()
                            } else {
                                "Kontak +$countryCode $cleanNum"
                            }
                            scope.launch(Dispatchers.IO) {
                                directContactDao.insertContact(
                                    DirectContactEntity(
                                        name = savedName,
                                        phoneNumber = cleanNum,
                                        countryCode = countryCode.trim().replace("+", "")
                                    )
                                )
                            }
                            Toast.makeText(context, "Kontak '$savedName' tersimpan di database!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Simpan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
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
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF128C7E), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Buka di WhatsApp Business",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color(0xFF128C7E)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Salin Link Chat (wa.me)", fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // BAGIAN DATABASE KONTAK TERSIMPAN
            // ==========================================
            HorizontalDivider(color = Color(0xFFE2E8F0))

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Buku Nomor Tersimpan (${contactsList.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Database internal cepat untuk chat berulang",
                        fontSize = 11.sp,
                        color = TextSecondaryLight
                    )
                }

                if (contactsList.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.ContactPhone,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (contactsList.isNotEmpty()) {
                // Pencarian Kontak Tersimpan
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari nama atau nomor...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Daftar Kontak
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    filteredContacts.forEach { contact ->
                        SavedContactCard(
                            contact = contact,
                            onSelect = {
                                contactName = contact.name
                                countryCode = contact.countryCode
                                phoneNumber = contact.phoneNumber
                                Toast.makeText(context, "Nomor ${contact.name} dipasang!", Toast.LENGTH_SHORT).show()
                            },
                            onDirectSend = {
                                val fullNum = "${contact.countryCode}${contact.phoneNumber.replace(Regex("[^0-9]"), "")}"
                                launchWhatsApp(context, fullNum, messageText, "com.whatsapp")
                            },
                            onDelete = {
                                contactToDelete = contact
                            }
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PermContactCalendar,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum Ada Kontak Tersimpan",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ketik nama & nomor di atas, lalu klik 'Simpan' untuk mencatat nomor penting atau pelanggan langganan tanpa perlu mengotori buku kontak HP Anda.",
                            fontSize = 12.sp,
                            color = TextSecondaryLight,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // Dialog Konfirmasi Hapus Kontak
    if (contactToDelete != null) {
        val target = contactToDelete!!
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = { Text("Hapus Kontak?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Apakah Anda yakin ingin menghapus '${target.name}' (+${target.countryCode} ${target.phoneNumber}) dari database kontak cepat?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            directContactDao.deleteContact(target)
                        }
                        Toast.makeText(context, "Kontak '${target.name}' dihapus", Toast.LENGTH_SHORT).show()
                        contactToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { contactToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun SavedContactCard(
    contact: DirectContactEntity,
    onSelect: () -> Unit,
    onDirectSend: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(contact.createdAt) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(contact.createdAt))
    }

    val initial = remember(contact.name) {
        contact.name.trim().take(1).uppercase(Locale.getDefault()).ifEmpty { "?" }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Inisial
            Surface(
                shape = CircleShape,
                color = PrimaryBlue.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = initial,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = PrimaryBlue
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Nama & Nomor
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "+${contact.countryCode} ${contact.phoneNumber}",
                    fontSize = 12.sp,
                    color = Color(0xFF16A34A),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Disimpan $dateStr",
                    fontSize = 10.sp,
                    color = TextSecondaryLight
                )
            }

            // Tombol Kirim Cepat WA
            IconButton(
                onClick = onDirectSend,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "Chat",
                    tint = Color(0xFF25D366),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Tombol Hapus
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Hapus",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp)
                )
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

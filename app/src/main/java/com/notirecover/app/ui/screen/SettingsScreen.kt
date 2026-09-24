package com.notirecover.app.ui.screen

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notirecover.app.data.preference.AppPreferences
import com.notirecover.app.ui.dialog.ProPaywallDialog
import com.notirecover.app.ui.theme.PrimaryBlue
import com.notirecover.app.ui.theme.TextSecondaryLight
import com.notirecover.app.util.LanguageHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: String,
    currentLanguage: String,
    onThemeChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }

    var isProUser by remember { mutableStateOf(prefs.isProUser) }
    var isAppLockEnabled by remember { mutableStateOf(prefs.isAppLockEnabled) }
    var useBiometric by remember { mutableStateOf(prefs.useBiometric) }
    var autoSaveFavoriteStatus by remember { mutableStateOf(prefs.autoSaveFavoriteStatus) }
    var isSmartAlertEnabled by remember { mutableStateOf(prefs.isSmartAlertEnabled) }
    var smartAlertKeywords by remember { mutableStateOf(prefs.smartAlertKeywords) }

    var showPaywallDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showKeywordsDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf(prefs.appLockPin) }
    var tempKeywordsInput by remember { mutableStateOf(prefs.smartAlertKeywords) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = LanguageHelper.get("settings_title", currentLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // BANNER UPGRADE PRO / STATUS PRO
            // ==========================================
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isProUser) Color(0xFFFEF3C7) else Color(0xFF1E293B),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPaywallDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isProUser) Color(0xFFF59E0B) else Color(0xFF334155),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = if (isProUser) Color.White else Color(0xFFFBBF24),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isProUser) "👑 ChatRestore PRO Aktif" else "Upgrade ke ChatRestore PRO",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isProUser) Color(0xFF92400E) else Color.White
                        )
                        Text(
                            text = if (isProUser) "Semua fitur VIP terbuka selamanya" else "Bebas Iklan, Bulk Save, Kunci PIN & Ekspor Chat",
                            fontSize = 11.sp,
                            color = if (isProUser) Color(0xFFB45309) else Color(0xFF94A3B8)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = if (isProUser) Color(0xFFB45309) else Color.White
                    )
                }
            }

            // ==========================================
            // KARTU 1: KEAMANAN & PRIVASI (APP LOCK)
            // ==========================================
            Text(
                text = "Kunci Aplikasi & Privasi",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = PrimaryBlue
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column {
                    SettingsSwitchRow(
                        title = "Kunci PIN Aplikasi",
                        subtitle = if (isAppLockEnabled) "PIN aktif (Klik untuk ganti PIN)" else "Amankan aplikasi saat dibuka",
                        icon = Icons.Default.Lock,
                        isChecked = isAppLockEnabled,
                        onCheckedChange = { checked ->
                            if (checked && !isProUser) {
                                showPaywallDialog = true
                            } else {
                                isAppLockEnabled = checked
                                prefs.isAppLockEnabled = checked
                                if (checked) {
                                    showPinDialog = true
                                }
                            }
                        },
                        onClick = {
                            if (isAppLockEnabled) {
                                newPinInput = prefs.appLockPin
                                showPinDialog = true
                            }
                        }
                    )

                    if (isAppLockEnabled) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = Color(0xFFF1F5F9))
                        SettingsSwitchRow(
                            title = "Buka dengan Sidik Jari (Biometrik)",
                            subtitle = "Gunakan sensor sidik jari HP untuk buka cepat",
                            icon = Icons.Default.Fingerprint,
                            isChecked = useBiometric,
                            onCheckedChange = { checked ->
                                useBiometric = checked
                                prefs.useBiometric = checked
                            }
                        )
                    }
                }
            }

            // ==========================================
            // KARTU 2: STATUS SAVER & GHOST VIEW PRO
            // ==========================================
            Text(
                text = "Status Saver & Ghost View PRO",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = PrimaryBlue
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column {
                    SettingsSwitchRow(
                        title = "Auto-Save Status di Background",
                        subtitle = "Otomatis simpan status WA kontak favorit tanpa buka satu per satu",
                        icon = Icons.Default.DownloadForOffline,
                        isChecked = autoSaveFavoriteStatus,
                        onCheckedChange = { checked ->
                            if (checked && !isProUser) {
                                showPaywallDialog = true
                            } else {
                                autoSaveFavoriteStatus = checked
                                prefs.autoSaveFavoriteStatus = checked
                                Toast.makeText(context, if (checked) "Auto-save status aktif!" else "Auto-save dimatikan", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // ==========================================
            // KARTU 3: SMART KEYWORD ALERT
            // ==========================================
            Text(
                text = "Smart Keyword Alert",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = PrimaryBlue
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column {
                    SettingsSwitchRow(
                        title = "Peringatan Kata Kunci Sensitif",
                        subtitle = "Beri alarm jika pesan terhapus mengandung kata khusus",
                        icon = Icons.Default.NotificationsActive,
                        isChecked = isSmartAlertEnabled,
                        onCheckedChange = { checked ->
                            if (checked && !isProUser) {
                                showPaywallDialog = true
                            } else {
                                isSmartAlertEnabled = checked
                                prefs.isSmartAlertEnabled = checked
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = Color(0xFFF1F5F9))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                tempKeywordsInput = prefs.smartAlertKeywords
                                showKeywordsDialog = true
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Daftar Kata Kunci Pantauan", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(text = smartAlertKeywords, fontSize = 11.sp, color = TextSecondaryLight, maxLines = 1)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }

            // ==========================================
            // KARTU 4: PENGATURAN TEMA
            // ==========================================
            Text(
                text = LanguageHelper.get("theme_title", currentLanguage),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = PrimaryBlue
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column {
                    ThemeOptionRow(
                        title = LanguageHelper.get("theme_system", currentLanguage),
                        subtitle = "Menyesuaikan dengan pengaturan dark/light di HP",
                        icon = Icons.Default.BrightnessAuto,
                        isSelected = currentTheme == AppPreferences.THEME_SYSTEM,
                        onClick = { onThemeChange(AppPreferences.THEME_SYSTEM) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = Color(0xFFF1F5F9))
                    ThemeOptionRow(
                        title = LanguageHelper.get("theme_light", currentLanguage),
                        subtitle = "Tampilan cerah dan bersih",
                        icon = Icons.Default.LightMode,
                        isSelected = currentTheme == AppPreferences.THEME_LIGHT,
                        onClick = { onThemeChange(AppPreferences.THEME_LIGHT) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = Color(0xFFF1F5F9))
                    ThemeOptionRow(
                        title = LanguageHelper.get("theme_dark", currentLanguage),
                        subtitle = "Hemat baterai dan nyaman di mata",
                        icon = Icons.Default.DarkMode,
                        isSelected = currentTheme == AppPreferences.THEME_DARK,
                        onClick = { onThemeChange(AppPreferences.THEME_DARK) }
                    )
                }
            }

            // ==========================================
            // KARTU 5: PENGATURAN BAHASA
            // ==========================================
            Text(
                text = LanguageHelper.get("lang_title", currentLanguage),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = PrimaryBlue
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column {
                    LanguageOptionRow(
                        title = "Bahasa Indonesia",
                        code = "IN",
                        flag = "🇮🇩",
                        isSelected = currentLanguage == AppPreferences.LANG_ID,
                        onClick = { onLanguageChange(AppPreferences.LANG_ID) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = Color(0xFFF1F5F9))
                    LanguageOptionRow(
                        title = "English",
                        code = "ENG",
                        flag = "🇬🇧",
                        isSelected = currentLanguage == AppPreferences.LANG_EN,
                        onClick = { onLanguageChange(AppPreferences.LANG_EN) }
                    )
                }
            }

            // ==========================================
            // KARTU INFORMASI & PRIVASI
            // ==========================================
            Text(
                text = LanguageHelper.get("about_app", currentLanguage),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = PrimaryBlue
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Versi Aplikasi", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("v1.0.0 (Release)", fontSize = 13.sp, color = TextSecondaryLight)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Status Iklan", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = if (isProUser) "100% Bebas Iklan (PRO)" else "Didukung Iklan Standar",
                            fontSize = 13.sp,
                            color = if (isProUser) Color(0xFF059669) else Color(0xFFD97706),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = LanguageHelper.get("privacy_safe", currentLanguage),
                        fontSize = 12.sp,
                        color = Color(0xFF059669),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Dialog Input / Ganti PIN
        if (showPinDialog) {
            AlertDialog(
                onDismissRequest = { showPinDialog = false },
                title = { Text("Atur PIN Aplikasi (4 Angka)", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Masukkan 4-digit PIN rahasia untuk mengunci ChatRestore:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newPinInput,
                            onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) newPinInput = it },
                            label = { Text("4-Digit PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPinInput.length == 4) {
                                prefs.appLockPin = newPinInput
                                showPinDialog = false
                                Toast.makeText(context, "PIN Berhasil Disimpan!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "PIN harus 4 angka!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Simpan PIN")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showPinDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Dialog Edit Kata Kunci Sensitif
        if (showKeywordsDialog) {
            AlertDialog(
                onDismissRequest = { showKeywordsDialog = false },
                title = { Text("Kata Kunci Pantauan", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Pisahkan dengan tanda koma (cth: transfer, bukti, pinjam, uang):")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tempKeywordsInput,
                            onValueChange = { tempKeywordsInput = it },
                            label = { Text("Kata Kunci") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            prefs.smartAlertKeywords = tempKeywordsInput
                            smartAlertKeywords = tempKeywordsInput
                            showKeywordsDialog = false
                            Toast.makeText(context, "Kata kunci berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showKeywordsDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Dialog Paywall PRO
        if (showPaywallDialog) {
            ProPaywallDialog(
                onDismiss = { showPaywallDialog = false },
                onSuccessPurchase = {
                    isProUser = prefs.isProUser
                }
            )
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick?.invoke() ?: onCheckedChange(!isChecked) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(text = subtitle, fontSize = 11.sp, color = TextSecondaryLight)
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryBlue
            )
        )
    }
}

@Composable
fun ThemeOptionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(text = subtitle, fontSize = 11.sp, color = TextSecondaryLight)
        }
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue)
        )
    }
}

@Composable
fun LanguageOptionRow(
    title: String,
    code: String,
    flag: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = flag, fontSize = 22.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(text = "Code: $code", fontSize = 11.sp, color = TextSecondaryLight)
        }
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue)
        )
    }
}

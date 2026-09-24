package com.notirecover.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notirecover.app.data.preference.AppPreferences
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
            // KARTU PENGATURAN TEMA
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

            Spacer(modifier = Modifier.height(6.dp))

            // ==========================================
            // KARTU PENGATURAN BAHASA
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

            Spacer(modifier = Modifier.height(6.dp))

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
                        Text("Status Lisensi", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "👑 PRO UNLOCKED",
                                color = Color(0xFFB45309),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
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

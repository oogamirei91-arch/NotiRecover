package com.notirecover.app.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.notirecover.app.data.preference.AppPreferences
import com.notirecover.app.ui.theme.PrimaryBlue
import com.notirecover.app.util.BiometricHelper

@Composable
fun AppLockScreen(
    onUnlockSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val prefs = remember { AppPreferences(context) }
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun launchBiometricPrompt() {
        if (activity != null && prefs.useBiometric && BiometricHelper.isBiometricAvailable(activity)) {
            BiometricHelper.authenticate(
                activity = activity,
                title = "Kunci Aplikasi ChatRestore",
                subtitle = "Pindai sidik jari atau gunakan kunci layar HP Anda",
                onSuccess = {
                    onUnlockSuccess()
                },
                onError = { err ->
                    errorMessage = err
                }
            )
        }
    }

    // Luncurkan sensor sidik jari otomatis saat layar terkunci muncul pertama kali
    LaunchedEffect(Unit) {
        launchBiometricPrompt()
    }

    fun handleKeyInput(num: String) {
        if (enteredPin.length < 4) {
            val newPin = enteredPin + num
            enteredPin = newPin
            errorMessage = null

            if (newPin.length == 4) {
                if (newPin == prefs.appLockPin) {
                    onUnlockSuccess()
                } else {
                    errorMessage = "PIN Salah! Silakan coba lagi."
                    enteredPin = ""
                }
            }
        }
    }

    fun handleBackspace() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            errorMessage = null
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFEFF6FF),
                border = androidx.compose.foundation.BorderStroke(2.dp, PrimaryBlue),
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Terkunci",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ChatRestore Terkunci",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Masukkan 4-digit PIN atau gunakan Sidik Jari",
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ==========================================
            // INDIKATOR 4-DIGIT PIN DOTS
            // ==========================================
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val isFilled = index < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (isFilled) PrimaryBlue else Color(0xFFE2E8F0))
                            .border(1.5.dp, if (isFilled) PrimaryBlue else Color(0xFF94A3B8), CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            errorMessage?.let { error ->
                Text(
                    text = error,
                    color = Color(0xFFDC2626),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } ?: Spacer(modifier = Modifier.height(18.dp))

            Spacer(modifier = Modifier.height(24.dp))

            // ==========================================
            // KEYPAD NUMERIK 1-9, 0, BIOMETRIK & BACKSPACE
            // ==========================================
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                keys.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "BIO" -> {
                                    IconButton(
                                        onClick = {
                                            if (prefs.useBiometric) {
                                                launchBiometricPrompt()
                                            } else {
                                                Toast.makeText(context, "Buka dengan PIN Anda", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(68.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = "Sidik Jari",
                                            tint = if (prefs.useBiometric) PrimaryBlue else Color.LightGray,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                                "DEL" -> {
                                    IconButton(
                                        onClick = { handleBackspace() },
                                        modifier = Modifier.size(68.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Hapus",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clickable { handleKeyInput(key) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = key,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

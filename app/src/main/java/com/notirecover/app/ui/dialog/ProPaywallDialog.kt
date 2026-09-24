package com.notirecover.app.ui.dialog

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.notirecover.app.data.preference.AppPreferences

enum class SubscriptionPlan(
    val title: String,
    val price: String,
    val period: String,
    val tag: String? = null,
    val isBestValue: Boolean = false
) {
    MONTHLY("Paket 1 Bulan", "Rp 15.000", "/ bulan"),
    ANNUAL("Paket 1 Tahun", "Rp 89.000", "/ tahun (Rp 7.400/bln)", tag = "HEMAT 50%", isBestValue = true),
    LIFETIME("Sekali Beli Selamanya", "Rp 129.000", "Akses Seumur Hidup", tag = "POPULER")
}

@Composable
fun ProPaywallDialog(
    onDismiss: () -> Unit,
    onSuccessPurchase: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    var selectedPlan by remember { mutableStateOf(SubscriptionPlan.ANNUAL) }
    var isLoading by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // ==========================================
                // HEADER HERO BANNER DENGAN GRADIENT EMAS
                // ==========================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1E293B),
                                    Color(0xFF0F172A)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFBBF24)),
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = "PRO",
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "ChatRestore PRO",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Buka semua fitur rahasia, privasi maksimal & tanpa jeda iklan.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // ==========================================
                // DAFTAR BENEFIT FITUR PRO
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProBenefitItem(
                        icon = Icons.Default.Block,
                        title = "100% Bebas Iklan",
                        desc = "Pengalaman bersih tanpa gangguan iklan apapun"
                    )
                    ProBenefitItem(
                        icon = Icons.Default.DownloadForOffline,
                        title = "Download Semua Status 1-Klik",
                        desc = "Simpan massal status WA & Auto-Save kontak favorit"
                    )
                    ProBenefitItem(
                        icon = Icons.Default.VisibilityOff,
                        title = "Ghost View & Media Vault HD",
                        desc = "Pulihkan foto/video terhapus tanpa batas kuota harian"
                    )
                    ProBenefitItem(
                        icon = Icons.Default.PictureAsPdf,
                        title = "Ekspor Chat Lengkap (PDF/HTML)",
                        desc = "Cetak riwayat pesan rapi lengkap dengan stempel waktu"
                    )
                    ProBenefitItem(
                        icon = Icons.Default.Fingerprint,
                        title = "Kunci Aplikasi & Biometrik",
                        desc = "Amankan chat dengan Sidik Jari & PIN rahasia"
                    )
                    ProBenefitItem(
                        icon = Icons.Default.NotificationsActive,
                        title = "Smart Keyword Alert",
                        desc = "Peringatan kilat jika pesan terhapus berisi kata sensitif"
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Color(0xFFF1F5F9))

                // ==========================================
                // PILIHAN PAKET SUBSCRIPTION
                // ==========================================
                Text(
                    text = "Pilih Paket Langganan:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SubscriptionPlan.values().forEach { plan ->
                        PlanCard(
                            plan = plan,
                            isSelected = selectedPlan == plan,
                            onClick = { selectedPlan = plan }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ==========================================
                // TOMBOL BELI SEKARANG / UPGRADE
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Button(
                        onClick = {
                            isLoading = true
                            // Simulasi Google Play Billing purchase success
                            prefs.isProUser = true
                            Toast.makeText(context, "Selamat! ChatRestore PRO Berhasil Diaktifkan! 👑", Toast.LENGTH_LONG).show()
                            isLoading = false
                            onSuccessPurchase()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD97706)
                        )
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Aktifkan PRO (${selectedPlan.price})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                prefs.isProUser = true
                                Toast.makeText(context, "Pembelian PRO Berhasil Dipulihkan!", Toast.LENGTH_SHORT).show()
                                onSuccessPurchase()
                                onDismiss()
                            }
                        ) {
                            Text("Pulihkan Pembelian", fontSize = 11.sp, color = Color(0xFF64748B))
                        }

                        Text(
                            text = "Aman via Google Play",
                            fontSize = 11.sp,
                            color = Color(0xFF059669),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ProBenefitItem(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFFFEF3C7),
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFB45309),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun PlanCard(
    plan: SubscriptionPlan,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color(0xFFFFFBEB) else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFFF59E0B) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFD97706))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = plan.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        plan.tag?.let { tag ->
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (plan.isBestValue) Color(0xFFDC2626) else Color(0xFF2563EB)
                            ) {
                                Text(
                                    text = tag,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = plan.period,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Text(
                text = plan.price,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = if (isSelected) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

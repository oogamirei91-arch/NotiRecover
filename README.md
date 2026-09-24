# ChatRestore - Universal Social Media Deleted Chat Recovery

Aplikasi Android untuk memantau, mencatat, dan memulihkan pesan media sosial (WhatsApp, Instagram, Telegram, Messenger) yang dihapus oleh pengirim via *Notification Listener* dengan proteksi anti-modifikasi tingkat tinggi.

---

## 🚀 Fitur Utama

1. **Universal Social Media Support:**
   - WhatsApp & WhatsApp Business (`com.whatsapp`, `com.whatsapp.w4b`)
   - Instagram DM (`com.instagram.android`)
   - Telegram & Telegram X (`org.telegram.messenger`, `org.thunderdog.challegram`)
   - Facebook Messenger (`com.facebook.orca`)
   - LINE (`jp.naver.line.android`)

2. **Algoritma Deteksi Pesan Terhapus (*Smart Recovery Engine*):**
   - Mendeteksi notifikasi pemicu (*"This message was deleted"*, *"Pesan ini telah dihapus"*, dll. dalam berbagai bahasa).
   - Menandai pesan di database lokal sebagai `isDeleted = true` tanpa menimpa teks asli.

3. **Keamanan & Proteksi Anti-Mod (NDK C++ & R8):**
   - **Native C++ Layer (`security-core.cpp`):** Deteksi anti-debugging (`TracerPid`), proteksi JNI, dan penyembunyian string sensitif dari Smali.
   - **R8 / ProGuard Agresif:** Obfuscation kelas, pembersihan log debug release, dan penyembunyian nama package.

4. **Modern UI/UX (Jetpack Compose & Material 3):**
   - Filter percakapan per sosmed (*Filter Chips*).
   - Indikator status service & panduan izin otomatis.
   - *Message Bubble* khusus beraksen merah menyala untuk pesan yang ditarik pengirim lengkap dengan waktu diterima dan waktu terhapus.

---

## 🛠️ Struktur Project

```
NotiRecover/
├── gradle/libs.versions.toml             # Version Catalog
├── build.gradle.kts                      # Root Gradle Configuration
├── settings.gradle.kts                   # Project Settings
└── app/
    ├── build.gradle.kts                  # App Module (Room, Compose, NDK, Billing)
    ├── proguard-rules.pro                # Aturan Obfuscation Anti-Mod
    └── src/main/
        ├── AndroidManifest.xml           # Izin Service, Boot, Foreground
        ├── cpp/
        │   ├── CMakeLists.txt            # CMake NDK Build
        │   └── security-core.cpp         # Native Anti-Debug & Anti-Tamper
        └── java/com/notirecover/app/
            ├── NotiRecoverApp.kt         # Application Class
            ├── MainActivity.kt           # Navigasi Compose
            ├── security/
            │   └── NativeSecurity.kt     # JNI Bridge Kotlin-C++
            ├── data/
            │   ├── AppDatabase.kt        # Room Database Singleton
            │   ├── dao/ChatDao.kt        # Room DAO & Recovery Logic
            │   └── model/
            │       ├── ConversationEntity.kt
            │       └── MessageEntity.kt
            ├── service/
            │   └── ChatNotificationListenerService.kt # Engine Utama
            ├── receiver/
            │   └── BootReceiver.kt       # Standby saat Reboot
            ├── util/
            │   ├── DeletedMessageClassifier.kt # Deteksi Pola Dihapus
            │   └── PermissionHelper.kt   # Setup Izin Notifikasi & Baterai
            └── ui/
                ├── theme/ (Color.kt, Theme.kt)
                └── screen/
                    ├── HomeScreen.kt     # Dashboard List Chat
                    └── ChatDetailScreen.kt # Tampilan Percakapan & Terhapus
```

---

## 📦 Cara Membuka & Menjalankan di Android Studio

1. Buka **Android Studio (Ladybug / Iguana atau yang lebih baru)**.
2. Pilih **Open Project** lalu arahkan ke folder:
   `C:\Users\administrator\.gemini\antigravity\scratch\NotiRecover`
3. Pastikan **Android NDK** dan **CMake** sudah terinstal di SDK Manager Android Studio.
4. Klik tombol **Run** (atau `Shift + F10`) pada Emulator Android / Perangkat Fisik (Android 8.0 Oreo ke atas).
5. Pada saat aplikasi pertama kali terbuka, klik tombol **"Aktifkan"** untuk memberikan izin akses notifikasi.

# ==============================================================================
# NotiRecover - ProGuard / R8 Anti-Modding & Obfuscation Rules
# ==============================================================================

# 1. Hapus Baris Debug Logging (Log.d, Log.v, Log.i) di Release Build
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# 2. Obfuscation Agresif
-repackageclasses 'com.notirecover.app.internal'
-allowaccessmodification

# 3. Lindungi Deklarasi Native (JNI) agar tidak hilang saat R8, tetapi tetap samarkan class lain
-keepclasseswithmembernames class * {
    native <methods>;
}

# 4. Room Database Rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# 5. Play Billing & Integrity Rules
-keep class com.android.billingclient.api.** { *; }
-keep class com.google.android.play.core.integrity.** { *; }

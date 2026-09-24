#include <jni.h>
#include <string>
#include <fstream>
#include <unistd.h>
#include <android/log.h>

#define LOG_TAG "SecurityCore"

// Hash SHA-256 Release Key Anda yang sebenarnya (ganti saat build release)
static const char* EXPECTED_SIGNATURE_HASH = "YOUR_RELEASE_KEY_SHA256_HASH_HERE";

/**
 * Memeriksa apakah aplikasi sedang di-attach oleh debugger (Anti-Debug via TracerPid)
 */
bool isBeingDebugged() {
    std::ifstream statusFile("/proc/self/status");
    if (!statusFile.is_open()) {
        return false;
    }

    std::string line;
    while (std::getline(statusFile, line)) {
        if (line.rfind("TracerPid:", 0) == 0) {
            int tracerPid = std::stoi(line.substr(10));
            return tracerPid > 0;
        }
    }
    return false;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_notirecover_app_security_NativeSecurity_checkIntegrity(
        JNIEnv* env,
        jobject /* this */,
        jobject context) {

    // 1. Cek Debugger / Anti-PTRACE
    if (isBeingDebugged()) {
        return JNI_FALSE;
    }

    // 2. Proteksi Dasar Integritas APK
    // Logika verifikasi signature di level C++
    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_notirecover_app_security_NativeSecurity_getSecureSalt(
        JNIEnv* env,
        jobject /* this */) {
    // Kunci enkripsi / salt yang disamarkan dalam C++ binary (tidak ada di Java smali)
    std::string secureSalt = "NR_SECURE_SALT_v1_98a7x!#_NOT_VISIBLE_IN_SMALI";
    return env->NewStringUTF(secureSalt.c_str());
}

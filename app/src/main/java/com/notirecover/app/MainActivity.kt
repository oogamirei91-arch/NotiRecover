package com.notirecover.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.notirecover.app.data.model.ConversationEntity
import com.notirecover.app.data.preference.AppPreferences
import com.notirecover.app.ui.screen.ChatDetailScreen
import com.notirecover.app.ui.screen.DirectChatScreen
import com.notirecover.app.ui.screen.HomeScreen
import com.notirecover.app.ui.screen.MediaGalleryScreen
import com.notirecover.app.ui.screen.SettingsScreen
import com.notirecover.app.ui.screen.SplashScreen
import com.notirecover.app.ui.screen.StatusSaverScreen
import com.notirecover.app.ui.screen.WebScreen
import com.notirecover.app.ui.theme.NotiRecoverTheme
import com.notirecover.app.util.BackupHelper
import com.notirecover.app.util.LanguageHelper
import com.notirecover.app.util.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var prefs: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        BackupHelper.restoreDatabaseIfAvailable(this)

        val app = application as NotiRecoverApp
        val database = app.database
        prefs = AppPreferences(this)

        setContent {
            var isSplashLoading by remember { mutableStateOf(true) }
            var isAppUnlocked by remember { mutableStateOf(!prefs.isAppLockEnabled) }
            var themeMode by remember { mutableStateOf(prefs.themeMode) }
            var currentLanguage by remember { mutableStateOf(prefs.language) }

            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                AppPreferences.THEME_LIGHT -> false
                AppPreferences.THEME_DARK -> true
                else -> systemDark
            }

            if (isSplashLoading) {
                SplashScreen(
                    onLoadingComplete = {
                        isSplashLoading = false
                    }
                )
            } else if (prefs.isAppLockEnabled && !isAppUnlocked) {
                NotiRecoverTheme(darkTheme = isDarkTheme) {
                    com.notirecover.app.ui.screen.AppLockScreen(
                        onUnlockSuccess = { isAppUnlocked = true }
                    )
                }
            } else {
                NotiRecoverTheme(darkTheme = isDarkTheme) {
                    var currentTab by remember { mutableIntStateOf(0) }
                    var isShowingSettings by remember { mutableStateOf(false) }

                    var isServiceEnabled by remember {
                        mutableStateOf(PermissionHelper.isNotificationServiceEnabled(this))
                    }

                    val conversations by database.chatDao().getAllConversations()
                        .collectAsState(initial = emptyList())

                    val mediaMessages by database.chatDao().getAllMediaMessages()
                        .collectAsState(initial = emptyList())

                    var selectedConversation by remember { mutableStateOf<ConversationEntity?>(null) }

                    // =========================================================================
                    // PENANGANAN TOMBOL BACK SISTEM (SMART BACK NAVIGATION)
                    // =========================================================================
                    BackHandler(enabled = isShowingSettings || selectedConversation != null || currentTab != 0) {
                        when {
                            isShowingSettings -> {
                                isShowingSettings = false
                            }
                            selectedConversation != null -> {
                                selectedConversation = null
                            }
                            currentTab != 0 -> {
                                currentTab = 0 // Kembali ke tab Pesan (Utama)
                            }
                        }
                    }

                    val scope = rememberCoroutineScope()

                    if (isShowingSettings) {
                        SettingsScreen(
                            currentTheme = themeMode,
                            currentLanguage = currentLanguage,
                            onThemeChange = { newTheme ->
                                themeMode = newTheme
                                prefs.themeMode = newTheme
                            },
                            onLanguageChange = { newLang ->
                                currentLanguage = newLang
                                prefs.language = newLang
                            },
                            onBackClick = { isShowingSettings = false }
                        )
                    } else if (selectedConversation != null) {
                        val currentConv = selectedConversation!!
                        val messages by database.chatDao().getMessagesForConversation(currentConv.id)
                            .collectAsState(initial = emptyList())

                        ChatDetailScreen(
                            conversation = currentConv,
                            messages = messages,
                            onBackClick = { selectedConversation = null },
                            onDeleteConversation = {
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    database.chatDao().deleteConversation(currentConv.id)
                                }
                                selectedConversation = null
                            },
                            onDeleteMessage = { msg ->
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    msg.mediaUri?.let { path ->
                                        try { java.io.File(path).delete() } catch (ignored: Exception) {}
                                    }
                                    database.chatDao().deleteMessage(msg.id)
                                }
                            }
                        )
                    } else {
                        Scaffold(
                            bottomBar = {
                                NavigationBar {
                                    NavigationBarItem(
                                        selected = currentTab == 0,
                                        onClick = { currentTab = 0 },
                                        icon = { Icon(Icons.Default.Chat, contentDescription = LanguageHelper.get("tab_messages", currentLanguage)) },
                                        label = { Text(LanguageHelper.get("tab_messages", currentLanguage)) }
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == 1,
                                        onClick = { currentTab = 1 },
                                        icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = LanguageHelper.get("tab_gallery", currentLanguage)) },
                                        label = { Text(LanguageHelper.get("tab_gallery", currentLanguage)) }
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == 2,
                                        onClick = { currentTab = 2 },
                                        icon = { Icon(Icons.Default.VisibilityOff, contentDescription = LanguageHelper.get("tab_status", currentLanguage)) },
                                        label = { Text(LanguageHelper.get("tab_status", currentLanguage)) }
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == 3,
                                        onClick = { currentTab = 3 },
                                        icon = { Icon(Icons.Default.Language, contentDescription = LanguageHelper.get("tab_web", currentLanguage)) },
                                        label = { Text(LanguageHelper.get("tab_web", currentLanguage)) }
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == 4,
                                        onClick = { currentTab = 4 },
                                        icon = { Icon(Icons.Default.Send, contentDescription = LanguageHelper.get("tab_direct", currentLanguage)) },
                                        label = { Text(LanguageHelper.get("tab_direct", currentLanguage)) }
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Surface(modifier = Modifier.padding(innerPadding)) {
                                when (currentTab) {
                                    0 -> HomeScreen(
                                        conversations = conversations,
                                        isServiceEnabled = isServiceEnabled,
                                        currentLanguage = currentLanguage,
                                        onEnableServiceClick = {
                                            PermissionHelper.openNotificationAccessSettings(this@MainActivity)
                                            PermissionHelper.requestIgnoreBatteryOptimization(this@MainActivity)
                                        },
                                        onConversationClick = { conv ->
                                            selectedConversation = conv
                                        },
                                        onDeleteConversation = { conv ->
                                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                database.chatDao().deleteConversation(conv.id)
                                            }
                                        },
                                        onDeleteMultipleConversations = { convSet ->
                                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                convSet.forEach { conv ->
                                                    database.chatDao().deleteConversation(conv.id)
                                                }
                                            }
                                        },
                                        onSettingsClick = {
                                            isShowingSettings = true
                                        }
                                    )
                                    1 -> MediaGalleryScreen(
                                        mediaMessages = mediaMessages,
                                        onDeleteMedia = { media ->
                                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                media.mediaUri?.let { path ->
                                                    try { java.io.File(path).delete() } catch (ignored: Exception) {}
                                                }
                                                database.chatDao().deleteMessage(media.id)
                                            }
                                        }
                                    )
                                    2 -> StatusSaverScreen()
                                    3 -> WebScreen()
                                    4 -> DirectChatScreen()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        BackupHelper.backupDatabase(this)
    }
}

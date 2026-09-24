package com.notirecover.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.notirecover.app.ui.screen.ChatDetailScreen
import com.notirecover.app.ui.screen.DirectChatScreen
import com.notirecover.app.ui.screen.HomeScreen
import com.notirecover.app.ui.screen.MediaGalleryScreen
import com.notirecover.app.ui.screen.StatusSaverScreen
import com.notirecover.app.ui.screen.WebScreen
import com.notirecover.app.ui.theme.NotiRecoverTheme
import com.notirecover.app.util.PermissionHelper

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as NotiRecoverApp
        val database = app.database

        setContent {
            NotiRecoverTheme {
                var currentTab by remember { mutableIntStateOf(0) }
                var isServiceEnabled by remember {
                    mutableStateOf(PermissionHelper.isNotificationServiceEnabled(this))
                }

                val conversations by database.chatDao().getAllConversations()
                    .collectAsState(initial = emptyList())

                val mediaMessages by database.chatDao().getAllMediaMessages()
                    .collectAsState(initial = emptyList())

                var selectedConversation by remember { mutableStateOf<ConversationEntity?>(null) }

                if (selectedConversation != null) {
                    val currentConv = selectedConversation!!
                    val messages by database.chatDao().getMessagesForConversation(currentConv.id)
                        .collectAsState(initial = emptyList())

                    ChatDetailScreen(
                        conversation = currentConv,
                        messages = messages,
                        onBackClick = { selectedConversation = null }
                    )
                } else {
                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentTab == 0,
                                    onClick = { currentTab = 0 },
                                    icon = { Icon(Icons.Default.Chat, contentDescription = "Pesan") },
                                    label = { Text("Pesan") }
                                )
                                NavigationBarItem(
                                    selected = currentTab == 1,
                                    onClick = { currentTab = 1 },
                                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = "Galeri") },
                                    label = { Text("Galeri") }
                                )
                                NavigationBarItem(
                                    selected = currentTab == 2,
                                    onClick = { currentTab = 2 },
                                    icon = { Icon(Icons.Default.VisibilityOff, contentDescription = "Status") },
                                    label = { Text("Status") }
                                )
                                NavigationBarItem(
                                    selected = currentTab == 3,
                                    onClick = { currentTab = 3 },
                                    icon = { Icon(Icons.Default.Language, contentDescription = "Web") },
                                    label = { Text("Web") }
                                )
                                NavigationBarItem(
                                    selected = currentTab == 4,
                                    onClick = { currentTab = 4 },
                                    icon = { Icon(Icons.Default.Send, contentDescription = "Direct WA") },
                                    label = { Text("Direct WA") }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Surface(modifier = Modifier.padding(innerPadding)) {
                            when (currentTab) {
                                0 -> HomeScreen(
                                    conversations = conversations,
                                    isServiceEnabled = isServiceEnabled,
                                    onEnableServiceClick = {
                                        PermissionHelper.openNotificationAccessSettings(this)
                                        PermissionHelper.requestIgnoreBatteryOptimization(this)
                                    },
                                    onConversationClick = { conv ->
                                        selectedConversation = conv
                                    }
                                )
                                1 -> MediaGalleryScreen(
                                    mediaMessages = mediaMessages
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

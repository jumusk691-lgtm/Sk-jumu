package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.screens.AudioCleanerScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.LiveMicScreen
import com.example.ui.screens.SettingsSheet
import com.example.ui.screens.VideoAudioScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VoiceCyan
import com.example.ui.theme.VoiceCyanGlow
import com.example.ui.theme.ZeroNoiseGreen
import com.example.ui.theme.ZeroNoiseGreenGlow

data class NavItem(val title: String, val icon: ImageVector, val tag: String)

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val currentTab by viewModel.currentTab.collectAsState()
                val showSettings by viewModel.showSettingsSheet.collectAsState()
                val showUpload by viewModel.showUploadSheet.collectAsState()
                val snackbarMessage by viewModel.snackbarMessage.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(snackbarMessage) {
                    snackbarMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearSnackbar()
                    }
                }

                val navItems = listOf(
                    NavItem("Live Mic", Icons.Default.Mic, "nav_live_mic"),
                    NavItem("Video Audio", Icons.Default.Videocam, "nav_video_audio"),
                    NavItem("Cleaner", Icons.Default.AutoAwesome, "nav_audio_cleaner"),
                    NavItem("Vault", Icons.Default.LibraryMusic, "nav_vault")
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = ObsidianBg,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    floatingActionButton = {
                        androidx.compose.material3.ExtendedFloatingActionButton(
                            onClick = { viewModel.openUploadSheet(true) },
                            containerColor = ZeroNoiseGreen,
                            contentColor = Color.Black,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("floating_upload_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudUpload,
                                contentDescription = "Upload Audio or Video",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "UPLOAD",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = StudioCardBg,
                            contentColor = TextPrimary,
                            tonalElevation = 8.dp,
                            modifier = Modifier
                                .navigationBarsPadding()
                                .border(
                                    width = 1.dp,
                                    color = StudioBorder,
                                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                )
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                .testTag("bottom_navigation_bar")
                        ) {
                            navItems.forEachIndexed { index, item ->
                                val selected = currentTab == index
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(index) },
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = item.title,
                                            fontSize = 10.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = ZeroNoiseGreenGlow,
                                        indicatorColor = ZeroNoiseGreenGlow,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    ),
                                    modifier = Modifier.testTag(item.tag)
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .statusBarsPadding()
                    ) {
                        Crossfade(targetState = currentTab, label = "tab_crossfade") { tab ->
                            when (tab) {
                                0 -> LiveMicScreen(viewModel = viewModel)
                                1 -> VideoAudioScreen(viewModel = viewModel)
                                2 -> AudioCleanerScreen(viewModel = viewModel)
                                3 -> LibraryScreen(viewModel = viewModel)
                            }
                        }

                        if (showSettings) {
                            SettingsSheet(
                                viewModel = viewModel,
                                onDismiss = { viewModel.openSettings(false) }
                            )
                        }

                        if (showUpload) {
                            com.example.ui.screens.UploadMediaSheet(
                                viewModel = viewModel,
                                onDismiss = { viewModel.openUploadSheet(false) }
                            )
                        }
                    }
                }
            }
        }
    }
}

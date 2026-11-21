package com.ranhroi.esp32hidremote.ui.screens.setting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.ranhroi.esp32hidremote.R
import com.ranhroi.esp32hidremote.ui.components.StringOptionsDropdownMenu
import com.ranhroi.esp32hidremote.ui.theme.NeonCyan
import com.ranhroi.esp32hidremote.utils.ConstantsUtils
import com.ranhroi.esp32hidremote.utils.IconsMap
import com.ranhroi.esp32hidremote.utils.getStringSafe
import com.ranhroi.esp32hidremote.utils.playSoundClick
import com.ranhroi.esp32hidremote.utils.showToast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    settingApp: SettingsViewModel = hiltViewModel(),
) {

    val view = LocalView.current
    val context = LocalContext.current
    val config by settingApp.settings.collectAsStateWithLifecycle()
    val isLoaded by settingApp.isLoaded.collectAsStateWithLifecycle()
    val isSave by settingApp.isSave.collectAsStateWithLifecycle()
    val languageMode by settingApp.selectedLanguageMode.collectAsStateWithLifecycle()
    val currentLanguageName = settingApp.getCurrentDisplayLanguageName(LocalContext.current)


    if (!isLoaded) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // ============================================================
    // STATE — Dùng Int cho slider
    // ============================================================
    var state by remember { mutableStateOf(config) }

    // ✅ Int — range 0..100
    var progress by remember { mutableIntStateOf(config.progress) }
    var scroll by remember { mutableIntStateOf(config.scroll) }
    var speedPressed by remember { mutableLongStateOf(config.speedPressed) }
    var delayText by remember { mutableLongStateOf(config.delayText) }
    var isInitialized by remember { mutableStateOf(false) }

    // ✅ Sync 1 lần khi load
    LaunchedEffect(config) {
        if (!isInitialized) {
            state = config
            progress = config.progress
            scroll = config.scroll
            speedPressed = config.speedPressed
            delayText = config.delayText
            isInitialized = true
        }
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            try {
                                view.playSoundClick()
                            } catch (_: Exception) {}

                            if (navController.previousBackStackEntry != null) {
                                navController.popBackStack()
                            }
                        }
                    ) {
                        Icon(
                            painterResource(R.drawable.baseline_arrow_back),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(padding)
                .padding(vertical = 20.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ========== CARD NGÔN NGỮ ==========
            Card {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InstructionItem(stringResource(R.string.instruction_language))

                    StringOptionsDropdownMenu(
                        text = stringResource(R.string.language_select),
                        state = ConstantsUtils.getDisplayLabel(languageMode),
                        optionsList = ConstantsUtils.languageOptionsList,
                        showAutoOption = true,
                        onSelectedOption = { selected ->
                            settingApp.changeLanguage(selected)
                            view.playSoundClick()
                        }
                    )

                    Text(
                        text = if (languageMode == "auto") {
                            stringResource(
                                R.string.language_current_auto,
                                ConstantsUtils.getDisplayLabel(currentLanguageName)
                            )
                        } else {
                            stringResource(
                                R.string.language_current_selected,
                                ConstantsUtils.getDisplayLabel(languageMode)
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }


            // ========== CARD HỆ ĐIỀU HÀNH ==========
            Card {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InstructionItem(stringResource(R.string.instruction_os))

                    Text(
                        text = stringResource(R.string.os_select_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ConstantsUtils.osOptionsList.forEach { option ->
                            val isSelected = state.osIndex == option.index

                            Card(
                                onClick = {
                                    state = state.copy(osIndex = option.index)
                                    view.playSoundClick()
                                },
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        NeonCyan.copy(alpha = 0.15f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                                border = if (isSelected) {
                                    BorderStroke(2.dp, NeonCyan)
                                } else {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = option.flag,
                                        fontSize = 24.sp
                                    )
                                    Text(
                                        text = stringResource(option.labelRes),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) NeonCyan else MaterialTheme.colorScheme.onSurface
                                    )

                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = stringResource(
                            R.string.os_current,
                            ConstantsUtils.getOsLabel(state.osIndex)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Medium
                    )
                }
            }


            // ========== CARD CURSOR ==========
            Card {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InstructionItem(stringResource(R.string.instruction_cursor))

                    // ===== CURSOR SPEED =====
                    Text(stringResource(R.string.cursor_speed, state.progress))

                    Slider(
                        value = progress.toFloat(),
                        onValueChange = {
                            val intValue = it.toInt().coerceIn(0, 100)
                            progress = intValue
                            state = state.copy(progress = intValue)
                        },
                        valueRange = 0f..100f,
                        steps = 0
                    )

                    // ===== SCROLL SPEED =====
                    Text(stringResource(R.string.scroll_speed, state.scroll))

                    Slider(
                        value = scroll.toFloat(),
                        onValueChange = {
                            val intValue = it.toInt().coerceIn(0, 100)
                            scroll = intValue
                            state = state.copy(scroll = intValue)
                        },
                        valueRange = 0f..100f,
                        steps = 0
                    )

                    // ===== DELAY TEXT =====
                    Text(stringResource(R.string.delay_text, state.delayText.toInt()))

                    Slider(
                        value = delayText.toFloat(),
                        onValueChange = {
                            val intValue = it.toLong().coerceIn(1, 100)
                            delayText = intValue
                            state = state.copy(delayText = intValue)
                        },
                        valueRange = 1f..100f,
                        steps = 0
                    )

                    // ===== SPEED_PRESSED =====
                    Text(stringResource(R.string.pressed_speed, state.speedPressed.toInt()))

                    Slider(
                        value = speedPressed.toFloat(),
                        onValueChange = {
                            val intValue = it.toLong().coerceIn(10, 100)
                            speedPressed = intValue
                            state = state.copy(speedPressed = intValue)
                        },
                        valueRange = 10f..100f,
                        steps = 0
                    )

                }
            }

            // ========== CARD MOUSE ==========
            Card {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InstructionItem(stringResource(R.string.instruction_mouse))

                    SettingCheckBox(
                        stringResource(R.string.mouse_left_hand),
                        state.isLeftHand
                    ) {
                        state = state.copy(isLeftHand = it)
                        view.playSoundClick()
                    }

                    SettingCheckBox(
                        stringResource(R.string.mouse_natural_scroll),
                        state.isScrollDirection
                    ) {
                        state = state.copy(isScrollDirection = it)
                        view.playSoundClick()
                    }
                }
            }


            // ========== CARD UI ==========
            Card {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InstructionItem(stringResource(R.string.instruction_ui))

                    SettingCheckBox(stringResource(R.string.ui_show_floating), state.isFloating) {
                        state = state.copy(isFloating = it)
                        view.playSoundClick()
                    }

                    SettingCheckBox(stringResource(R.string.ui_show_button_text), state.isButtonText) {
                        state = state.copy(isButtonText = it)
                        view.playSoundClick()
                    }

                    SettingCheckBox(stringResource(R.string.ui_show_rotation), state.isRotation) {
                        state = state.copy(isRotation = it)
                        view.playSoundClick()
                    }

                    SettingCheckBox(stringResource(R.string.auto_ping), state.isAutoPing) {
                        state = state.copy(isAutoPing = it)
                        view.playSoundClick()
                    }
                }
            }

            // ========== CARD RESET SHORTCUTS ==========
            Card {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InstructionItem("Reset shortkey keys")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.reset_system_shortcuts))
                        IconButton(
                            onClick = {
                                try {
                                    settingApp.resetToShortKeysListDefault()
                                    context.showToast(
                                        context.getStringSafe(R.string.reset_shortkey_success)
                                    )
                                    view.playSoundClick()
                                } catch (_: Exception) {}
                            }
                        ) {
                            Icon(
                                painterResource(R.drawable.baseline_refresh),
                                contentDescription = "Reset cache"
                            )
                        }
                    }
                }
            }

            // ========== CARD RESET QUICK KEY ==========
            Card {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InstructionItem("Reset Quick keys")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.reset_system_quick_key))
                        IconButton(
                            onClick = {
                                try {
                                    settingApp.resetToQuickKeysListDefault()
                                    context.showToast(
                                        context.getStringSafe(R.string.reset_shortkey_success)
                                    )
                                    view.playSoundClick()
                                } catch (_: Exception) {}
                            }
                        ) {
                            Icon(
                                painterResource(R.drawable.baseline_refresh),
                                contentDescription = "Reset cache"
                            )
                        }
                    }
                }
            }

            // ========== CARD ICON CACHE ==========
            Card {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InstructionItem(stringResource(R.string.instruction_icon_cache))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.icon_cache_reset))
                        IconButton(
                            onClick = {
                                try {
                                    IconsMap.resetCache()
                                    context.showToast(
                                        context.getStringSafe(R.string.icon_cache_reset_success)
                                    )
                                    view.playSoundClick()
                                } catch (_: Exception) {}
                            }
                        ) {
                            Icon(
                                painterResource(R.drawable.baseline_refresh),
                                contentDescription = "Reset cache"
                            )
                        }
                    }
                }
            }

            // ========== SAVE BUTTON ==========
            Card(
                onClick = {
                    try {
                        settingApp.updateSettings(state)
                        view.playSoundClick()
                    } catch (_: Exception) {}
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSave) {
                        Icon(painterResource(R.drawable.baseline_save), contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.saved))
                    } else {
                        Text(stringResource(R.string.save_settings))
                    }
                }
            }
        }
    }
}

@Composable
fun SettingCheckBox(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title)
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun InstructionItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "•",
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
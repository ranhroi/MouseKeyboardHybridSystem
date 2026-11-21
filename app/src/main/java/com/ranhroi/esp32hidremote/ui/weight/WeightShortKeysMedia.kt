package com.ranhroi.esp32hidremote.ui.weight

import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ranhroi.esp32hidremote.models.DeviceServerModel
import com.ranhroi.esp32hidremote.models.DropMenuModel
import com.ranhroi.esp32hidremote.models.HybridConnectionModel
import com.ranhroi.esp32hidremote.models.ShortKeyModel
import com.ranhroi.esp32hidremote.ui.components.ButtonWeight
import com.ranhroi.esp32hidremote.ui.components.DotsLoadingIndicator
import com.ranhroi.esp32hidremote.ui.components.DotsOrientation
import com.ranhroi.esp32hidremote.ui.components.MoreVertDropdown
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsButtonText
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsHID
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsRotation
import com.ranhroi.esp32hidremote.ui.keyboards.LocalSpeedPressed
import com.ranhroi.esp32hidremote.ui.screens.server.send.InputManager
import com.ranhroi.esp32hidremote.ui.screens.shortkey.ShortKeySetupViewModel
import com.ranhroi.esp32hidremote.ui.screens.shortkey.ShortKeysRepository
import com.ranhroi.esp32hidremote.utils.playSoundClick
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun WeightShortKeysMedia(
    state: HybridConnectionModel,
    inputManager: InputManager,
    allShortKeys: List<ShortKeyModel>,
    shortKeySetupViewModel: ShortKeySetupViewModel
) {

    val view = LocalView.current
    val isHID = LocalIsHID.current
    val isRotation = LocalIsRotation.current

    val selectedKey by shortKeySetupViewModel.selectedKey.collectAsStateWithLifecycle()
    val detectedAppType by shortKeySetupViewModel.detectedAppType.collectAsStateWithLifecycle()
    val isManual by shortKeySetupViewModel.isManualSelected.collectAsStateWithLifecycle()

    var showSelector by remember { mutableStateOf(false) }

    val effectiveKey = when {
        isManual && selectedKey != null -> selectedKey!!
        detectedAppType != null -> detectedAppType!!
        selectedKey != null -> selectedKey!!
        else -> "media"
    }

    val shortKeys = remember(effectiveKey, allShortKeys) {
        allShortKeys.filter { it.type == effectiveKey }
    }

    LaunchedEffect(Unit) {
        shortKeySetupViewModel.loadSelectedKey()
        if (!isHID) {
            shortKeySetupViewModel.startAppDetection(state)   // ← BẬT READER TRƯỚC
            delay(50.milliseconds)                                     // ← đợi reader sẵn sàng
            inputManager.requestCurrentApp()              // ← GỬI LỆNH SAU
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (!isHID) shortKeySetupViewModel.stopAppDetection()
        }
    }

    val onKeySelected: (String) -> Unit = { newKey ->
        shortKeySetupViewModel.selectedKey(newKey)
        showSelector = false
    }

    Column(
        modifier = if (isRotation) Modifier
            .wrapContentWidth()
            .fillMaxHeight() else Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        if (shortKeys.isNotEmpty()) {
            when (isRotation) {
                true -> MediaDrawerLayout(
                    view = view,
                    shortKeys = shortKeys,
                    selectedKey = effectiveKey,
                    showSelector = showSelector,
                    onToggleSelector = { showSelector = !showSelector },
                    onKeySelected = onKeySelected,
                    inputManager = inputManager,
                    currentServer = state.server,
                    shortKeySetupViewModel = shortKeySetupViewModel
                )

                false -> MediaTopCenterLayout(
                    view = view,
                    shortKeys = shortKeys,
                    selectedKey = effectiveKey,
                    showSelector = showSelector,
                    onToggleSelector = { showSelector = !showSelector },
                    onKeySelected = onKeySelected,
                    inputManager = inputManager,
                    currentServer = state.server,
                    shortKeySetupViewModel = shortKeySetupViewModel
                )
            }
        } else {
            // Chỉ hiện loading khi chưa có key nào (lần đầu), còn lại báo lỗi ngay
            Box(
                modifier = Modifier.wrapContentSize(),
                contentAlignment = Alignment.Center
            ) {
                if (selectedKey == null) {
                    DotsLoadingIndicator(
                        orientation = if (isRotation)
                            DotsOrientation.VERTICAL
                        else DotsOrientation.HORIZONTAL
                    )
                } else {
                    Text("No shortKey for \"$effectiveKey\"")
                }
            }
        }
    }
}


// ============================================================
// LAYOUT 1: Horizontal (rotation) — 14 phím
// ============================================================
@Composable
fun MediaDrawerLayout(
    view: View,
    shortKeys: List<ShortKeyModel>,
    selectedKey: String,
    showSelector: Boolean,
    onToggleSelector: () -> Unit,
    onKeySelected: (String) -> Unit,
    inputManager: InputManager,
    currentServer: DeviceServerModel,
    shortKeySetupViewModel: ShortKeySetupViewModel,
) {
    val isButtonText = LocalIsButtonText.current
    val speedPressed = LocalSpeedPressed.current

    val iconSelector = ShortKeysRepository.shortKeyPreset.find { it.type == selectedKey }?.icon
        ?: "baseline_arrow_drop_down"

    Column(modifier = Modifier.wrapContentSize()) {
        LazyHorizontalGrid(
            rows = GridCells.Fixed(7),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            reverseLayout = true
        ) {
            itemsIndexed(shortKeys) { _, shortKey ->
                ButtonWeight(
                    modifier = Modifier.width(50.dp),
                    colorStroke = Color.Gray.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(3.dp),
                    onClick = {
                        inputManager.executeShortKey(shortKey)
                        view.playSoundClick()
                    },
                    onLongPressRepeat = {
                        if (shortKey.name in listOf("Volume Up", "Volume Down", "Rewind", "Fast Forward")) {
                            inputManager.executeShortKey(shortKey)
                            view.playSoundClick()
                        }
                    },
                    speedPressed = if (currentServer.bluetooth) speedPressed * 2 else speedPressed,
                    iconName = shortKey.icon,
                    text = if (isButtonText) shortKey.name.take(4) else null,
                    rotationDegrees = 90f,
                    aspectRatio = 0.6f
                )
            }

            item {
                ButtonWeight(
                    modifier = Modifier.width(50.dp),
                    colorStroke = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(3.dp),
                    onClick = {
                        onToggleSelector()
                        view.playSoundClick()
                    },
                    iconName = iconSelector,
                    text = if (isButtonText) selectedKey.take(4) else null,
                    rotationDegrees = 90f,
                    aspectRatio = 0.6f
                ) {
                    if (showSelector) {
                        MoreVertDropdown(
                            view = view,
                            expanded = showSelector,
                            onDismiss = { onToggleSelector() },
                            menuItems = listOf(
                                DropMenuModel(
                                    text = "Auto",
                                    onClick = {
                                        shortKeySetupViewModel.resetToAuto()
                                        onToggleSelector()
                                    },
                                    icon = "baseline_autorenew"
                                )
                            ) + ShortKeysRepository.shortKeyPreset.map { preset ->
                                DropMenuModel(
                                    text = preset.label,
                                    onClick = {
                                        onKeySelected(preset.type)   // → selectedKey() → manual = true
                                        onToggleSelector()
                                    },
                                    icon = preset.icon
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}


// ============================================================
// LAYOUT 2: Vertical — 14 phím
// ============================================================
@Composable
private fun MediaTopCenterLayout(
    view: View,
    shortKeys: List<ShortKeyModel>,
    selectedKey: String,
    showSelector: Boolean,
    onToggleSelector: () -> Unit,
    onKeySelected: (String) -> Unit,
    inputManager: InputManager,
    currentServer: DeviceServerModel,
    shortKeySetupViewModel: ShortKeySetupViewModel,
) {
    val isButtonText = LocalIsButtonText.current
    val speedPressed = LocalSpeedPressed.current

    val iconSelector = ShortKeysRepository.shortKeyPreset.find { it.type == selectedKey }?.icon
        ?: "baseline_arrow_drop_down"

    Column(modifier = Modifier.wrapContentSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            itemsIndexed(shortKeys) { index, shortKey ->
                ButtonWeight(
                    modifier = Modifier.wrapContentSize(),
                    colorStroke = Color.Gray.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(3.dp),
                    onClick = {
                        inputManager.executeShortKey(shortKey)
                        view.playSoundClick()
                    },
                    onLongPressRepeat = {
                        if (shortKey.name in listOf("Volume Up", "Volume Down", "Rewind", "Fast Forward")) {
                            inputManager.executeShortKey(shortKey)
                            view.playSoundClick()
                        }
                    },
                    speedPressed = if (currentServer.bluetooth) speedPressed * 2 else speedPressed,
                    iconName = shortKey.icon,
                    text = if (isButtonText) shortKey.name else null
                )
            }

            item {
                ButtonWeight(
                    modifier = Modifier.wrapContentSize(),
                    colorStroke = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(3.dp),
                    onClick = {
                        onToggleSelector()
                        view.playSoundClick()
                    },
                    iconName = iconSelector,
                    text = if (isButtonText) selectedKey else null
                ) {
                    if (showSelector) {
                        MoreVertDropdown(
                            view = view,
                            expanded = showSelector,
                            onDismiss = { onToggleSelector() },
                            menuItems = listOf(
                                DropMenuModel(
                                    text = "Auto",
                                    onClick = {
                                        shortKeySetupViewModel.resetToAuto()
                                        onToggleSelector()
                                    },
                                    icon = "baseline_autorenew"
                                )
                            ) + ShortKeysRepository.shortKeyPreset.map { preset ->
                                DropMenuModel(
                                    text = preset.label,
                                    onClick = {
                                        onKeySelected(preset.type)   // → selectedKey() → manual = true
                                        onToggleSelector()
                                    },
                                    icon = preset.icon
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
package com.ranhroi.esp32hidremote.ui.weight

import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.ranhroi.esp32hidremote.R
import com.ranhroi.esp32hidremote.models.DropMenuModel
import com.ranhroi.esp32hidremote.models.QuickKeyModel
import com.ranhroi.esp32hidremote.ui.components.ButtonWeight
import com.ranhroi.esp32hidremote.ui.components.DotsLoadingIndicator
import com.ranhroi.esp32hidremote.ui.components.DotsOrientation
import com.ranhroi.esp32hidremote.ui.components.MoreVertDropdown
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsButtonText
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsRotation
import com.ranhroi.esp32hidremote.ui.navigation.NavRoute
import com.ranhroi.esp32hidremote.ui.screens.quickkey.QuickKeysSetupViewModel
import com.ranhroi.esp32hidremote.ui.screens.server.send.InputManager
import com.ranhroi.esp32hidremote.utils.playSoundClick
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds


@Composable
fun WeightQuickKeys(
    navController: NavHostController,
    inputManager: InputManager,
    allQuickKeys: List<QuickKeyModel>,
    quickKeysSetupViewModel: QuickKeysSetupViewModel
) {

    val view = LocalView.current
    val isRotation = LocalIsRotation.current

    var isLoading by rememberSaveable { mutableStateOf(true) }
    var showSelector by remember { mutableStateOf(false) }

    val selectedPreset by quickKeysSetupViewModel.selectedPreset.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        delay(5000.milliseconds)
        isLoading = false
    }

    val launchKeys = remember(allQuickKeys) { allQuickKeys.filter { it.openApp } }
    val softwareKeys = remember(allQuickKeys) { allQuickKeys.filter { !it.openApp } }
    val availablePresets = remember(allQuickKeys) {
        quickKeysSetupViewModel.getAvailablePresets(openApp = false)
    }
    val displayedKeys = remember(softwareKeys, selectedPreset) {
        if (selectedPreset == null) softwareKeys
        else softwareKeys.filter { it.preset == selectedPreset }
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
        if (allQuickKeys.isNotEmpty()) {

            if (softwareKeys.isNotEmpty()) {
                when (isRotation) {
                    true -> SoftwareDrawerLayout(
                        view = view,
                        launchKeys = launchKeys,
                        quickKeys = displayedKeys,
                        selectedPreset = selectedPreset,
                        availablePresets = availablePresets,
                        showSelector = showSelector,
                        onToggleSelector = { showSelector = !showSelector },
                        onPresetSelected = { presetName ->
                            quickKeysSetupViewModel.selectPreset(presetName)
                            showSelector = false
                        },
                        onCreate = {
                            navController.navigate(NavRoute.SetQuickKeys.createRoute(0))
                        },
                        onEdit = { quickKey ->
                            navController.navigate(NavRoute.SetQuickKeys.createRoute(quickKey.id))
                        },
                        onDelete = { quickKey ->
                            quickKeysSetupViewModel.removeQuickKey(quickKey)
                        },
                        inputManager = inputManager,
                    )

                    false -> SoftwareTopCenterLayout(
                        view = view,
                        launchKeys = launchKeys,
                        quickKeys = displayedKeys,
                        selectedPreset = selectedPreset,
                        availablePresets = availablePresets,
                        showSelector = showSelector,
                        onToggleSelector = { showSelector = !showSelector },
                        onPresetSelected = { presetName ->
                            quickKeysSetupViewModel.selectPreset(presetName)
                            showSelector = false
                        },
                        onCreate = {
                            navController.navigate(NavRoute.SetQuickKeys.createRoute(0))
                        },
                        onEdit = { quickKey ->
                            navController.navigate(NavRoute.SetQuickKeys.createRoute(quickKey.id))
                        },
                        onDelete = { quickKey ->
                            quickKeysSetupViewModel.removeQuickKey(quickKey)
                        },
                        inputManager = inputManager
                    )
                }
            }

        } else {
            if (isLoading) {
                Box(
                    modifier = Modifier.wrapContentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    DotsLoadingIndicator(
                        orientation = if (isRotation)
                            DotsOrientation.VERTICAL
                        else DotsOrientation.HORIZONTAL
                    )
                }
            } else {
                Box(
                    modifier = Modifier.wrapContentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.quickkey_empty))
                }
            }
        }
    }
}


// ============================================================
// HÀNG 2a — SOFTWARE Drawer (rotation) — Select luôn ở vị trí cuối
// ============================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SoftwareDrawerLayout(
    view: View,
    launchKeys: List<QuickKeyModel>,
    quickKeys: List<QuickKeyModel>,
    selectedPreset: String?,
    availablePresets: List<String>,
    showSelector: Boolean,
    onToggleSelector: () -> Unit,
    onPresetSelected: (String?) -> Unit,
    onCreate: () -> Unit,
    onEdit: (QuickKeyModel) -> Unit,
    onDelete: (QuickKeyModel) -> Unit,
    inputManager: InputManager,
    ) {
    val isButtonText = LocalIsButtonText.current

    Column(modifier = Modifier.wrapContentSize()) {
        LazyHorizontalGrid(
            rows = GridCells.Fixed(6),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            reverseLayout = true,
            modifier = Modifier.wrapContentSize()
        ) {
            itemsIndexed(launchKeys + quickKeys) { _, quickKey ->
                var expanded by remember { mutableStateOf(false) }
                ButtonWeight(
                    modifier = Modifier.width(50.dp),
                    colorStroke = if (quickKey.openApp) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(3.dp),
                    onClick = {
                        inputManager.executeQuickKeys(quickKey)
                        view.playSoundClick()
                    },
                    onLongClick = { expanded = true },
                    iconName = quickKey.icon,
                    text = if (isButtonText) quickKey.name else null,
                    rotationDegrees = 90f,
                    aspectRatio = 0.6f
                ) {
                    MoreVertDropdown(
                        view = view,
                        expanded = expanded,
                        onDismiss = { expanded = false },
                        menuItems = listOf(
                            DropMenuModel(
                                text = stringResource(R.string.quickkey_edit),
                                onClick = { onEdit(quickKey) },
                                icon = "baseline_edit"
                            ),
                            DropMenuModel(
                                text = stringResource(R.string.quickkey_remove),
                                onClick = { onDelete(quickKey) },
                                icon = "baseline_remove",
                                isDestructive = true
                            ),
                        )
                    )
                }
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
                    iconName = "baseline_arrow_drop_down",
                    text = if (isButtonText) {
                        selectedPreset?.take(4) ?: stringResource(R.string.quickkey_preset_all)
                    } else null,
                    rotationDegrees = 90f,
                    aspectRatio = 0.6f
                ) {
                    if (showSelector) {
                        MoreVertDropdown(
                            view = view,
                            expanded = showSelector,
                            onDismiss = onToggleSelector,
                            menuItems = buildList {
                                add(
                                    DropMenuModel(
                                        text = stringResource(R.string.quickkey_preset_all),
                                        onClick = {
                                            onPresetSelected(null)
                                            onToggleSelector()
                                        },
                                        icon = "baseline_list_alt"
                                    )
                                )
                                availablePresets.forEach { presetName ->
                                    add(
                                        DropMenuModel(
                                            text = presetName,
                                            onClick = {
                                                onPresetSelected(presetName)
                                                onToggleSelector()
                                            },
                                            icon = "baseline_apps"
                                        )
                                    )
                                }
                                add(
                                    DropMenuModel(
                                        text = stringResource(R.string.quickkey_create),
                                        onClick = onCreate,
                                        icon = "baseline_create"
                                    )
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
// HÀNG 2b — SOFTWARE TopCenter — Select luôn ở vị trí cuối
// ============================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SoftwareTopCenterLayout(
    view: View,
    launchKeys: List<QuickKeyModel>,
    quickKeys: List<QuickKeyModel>,
    selectedPreset: String?,
    availablePresets: List<String>,
    showSelector: Boolean,
    onToggleSelector: () -> Unit,
    onPresetSelected: (String?) -> Unit,
    onCreate: () -> Unit,
    onEdit: (QuickKeyModel) -> Unit,
    onDelete: (QuickKeyModel) -> Unit,
    inputManager: InputManager,
) {
    val isButtonText = LocalIsButtonText.current

    Column(modifier = Modifier.wrapContentSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            itemsIndexed(launchKeys + quickKeys) { _, quickKey ->
                var expanded by remember { mutableStateOf(false) }
                ButtonWeight(
                    modifier = Modifier.wrapContentSize(),
                    colorStroke = if (quickKey.openApp) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(3.dp),
                    onClick = {
                        inputManager.executeQuickKeys(quickKey)
                        view.playSoundClick()
                    },
                    onLongClick = { expanded = true },
                    iconName = quickKey.icon,
                    text = if (isButtonText) quickKey.name else null
                ) {
                    MoreVertDropdown(
                        view = view,
                        expanded = expanded,
                        onDismiss = { expanded = false },
                        menuItems = listOf(
                            DropMenuModel(
                                text = stringResource(R.string.quickkey_edit),
                                onClick = { onEdit(quickKey) },
                                icon = "baseline_edit"
                            ),
                            DropMenuModel(
                                text = stringResource(R.string.quickkey_remove),
                                onClick = { onDelete(quickKey) },
                                icon = "baseline_remove",
                                isDestructive = true
                            ),
                        )
                    )
                }
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
                    iconName = "baseline_arrow_drop_down",
                    text = if (isButtonText) {
                        selectedPreset?.take(4) ?: stringResource(R.string.quickkey_preset_all)
                    } else null
                ) {
                    if (showSelector) {
                        MoreVertDropdown(
                            view = view,
                            expanded = showSelector,
                            onDismiss = onToggleSelector,
                            menuItems = buildList {
                                add(
                                    DropMenuModel(
                                        text = stringResource(R.string.quickkey_preset_all),
                                        onClick = {
                                            onPresetSelected(null)
                                            onToggleSelector()
                                        },
                                        icon = "baseline_list_alt"
                                    )
                                )
                                availablePresets.forEach { presetName ->
                                    add(
                                        DropMenuModel(
                                            text = presetName,
                                            onClick = {
                                                onPresetSelected(presetName)
                                                onToggleSelector()
                                            },
                                            icon = "baseline_apps"
                                        )
                                    )
                                }
                                add(
                                    DropMenuModel(
                                        text = stringResource(R.string.quickkey_create),
                                        onClick = onCreate,
                                        icon = "baseline_create"
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
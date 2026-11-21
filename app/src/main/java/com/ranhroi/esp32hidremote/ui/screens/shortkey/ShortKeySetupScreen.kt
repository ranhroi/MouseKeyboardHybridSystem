package com.ranhroi.esp32hidremote.ui.screens.shortkey

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.ranhroi.esp32hidremote.R
import com.ranhroi.esp32hidremote.models.DropMenuModel
import com.ranhroi.esp32hidremote.models.ShortKeyModel
import com.ranhroi.esp32hidremote.ui.components.ConfirmDialog
import com.ranhroi.esp32hidremote.ui.components.DotsLoadingIndicator
import com.ranhroi.esp32hidremote.ui.components.MoreVertDropdown
import com.ranhroi.esp32hidremote.ui.dialogs.DialogInsertShortKey
import com.ranhroi.esp32hidremote.ui.dialogs.DialogUpdateShortKey
import com.ranhroi.esp32hidremote.utils.playSoundClick
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortKeySetupScreen(
    navController: NavHostController = rememberNavController(),
    shortKeySetupViewModel: ShortKeySetupViewModel = hiltViewModel()
) {
    val allShortKeys by shortKeySetupViewModel.allShortKeys.collectAsState()
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    val options = remember(allShortKeys) {
        val fromData = allShortKeys.map { it.type }.distinct()
        (fromData).distinct()
    }

    var expanded by remember { mutableStateOf(false) }
    var isDialogInsert by remember { mutableStateOf(false) }

    val textFieldState = rememberTextFieldState("floating")
    var filterShortcuts by remember { mutableStateOf<List<ShortKeyModel>>(emptyList()) }

    LaunchedEffect(allShortKeys, textFieldState.text) {
        val result = allShortKeys.filter { it.type == textFieldState.text.toString() }
        filterShortcuts = result.ifEmpty { allShortKeys }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shortkey_setup_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch {
                            if (navController.previousBackStackEntry != null) {
                                navController.popBackStack()
                            }
                        }
                        view.playSoundClick()
                    }) {
                        Icon(painterResource(R.drawable.baseline_arrow_back), contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isDialogInsert = true
                        view.playSoundClick()
                    }) {
                        Icon(painterResource(R.drawable.baseline_add), contentDescription = "baseline_add")
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(innerPadding)
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                TextField(
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    readOnly = true,
                    value = textFieldState.text.toString(),
                    onValueChange = { textFieldState.setTextAndPlaceCursorAtEnd(it) },
                    label = { Text(stringResource(R.string.shortkey_setup_select_option)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                )

                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                textFieldState.setTextAndPlaceCursorAtEnd(selectionOption)
                                expanded = false
                            }
                        )
                    }
                }
            }
            ShortcutListScreen(shortcuts = filterShortcuts, shortKeySetupViewModel)
        }
    }


    if (isDialogInsert) {
        DialogInsertShortKey(
            type = filterShortcuts[0].type,
            shortKeySetupViewModel = shortKeySetupViewModel,
            onDismiss = {
                isDialogInsert = false
            }
        )
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortcutListScreen(
    shortcuts: List<ShortKeyModel> = emptyList(),
    shortKeySetupViewModel: ShortKeySetupViewModel
) {
    val view = LocalView.current
    var clickShortcutId by remember { mutableStateOf<Int?>(null) }
    var isDialogUpdate by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var selectedShortcut by remember { mutableStateOf<ShortKeyModel?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        if (shortcuts.isNotEmpty()) {
            itemsIndexed(shortcuts) { _, shortcut ->
                var isVisibleDrop by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .clip(RoundedCornerShape(0.dp))
                        .fillMaxWidth()
                        .heightIn(min = 60.dp, max = 120.dp)
                        .combinedClickable(
                            onClick = {
                                // Xử lý khi click
                                clickShortcutId = if (clickShortcutId == shortcut.id) null else shortcut.id
                            },
                            onLongClick = {
                                // Xử lý khi giữ lâu
                                isVisibleDrop = true
                            }
                        ),
                ) {
                    ShortKeyItem(shortcut)
                    MoreVertDropdown(
                        view = view,
                        expanded = isVisibleDrop,
                        onDismiss = { isVisibleDrop = false },
                        menuItems = listOf(
                            DropMenuModel(
                                text = stringResource(R.string.shortkey_setup_edit),
                                onClick = {
                                    clickShortcutId = null
                                    isDialogUpdate = true
                                    isVisibleDrop = false
                                    selectedShortcut = shortcut
                                },
                                icon = "baseline_launch"
                            ),
                            DropMenuModel(
                                text = stringResource(R.string.shortkey_setup_remove),
                                onClick = {
                                    isVisibleDrop = false
                                    selectedShortcut = shortcut
                                    showDeleteConfirm = true
                                },
                                icon = "baseline_remove",
                                isDestructive = true
                            )
                        )
                    )
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }
        } else {
            item {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    DotsLoadingIndicator()
                }
            }
        }
    }

    if (isDialogUpdate && selectedShortcut != null) {
        DialogUpdateShortKey(
            selectedShortcut!!,
            shortKeySetupViewModel,
            onDismiss = {
                isDialogUpdate = false
                selectedShortcut = null
            }
        )
    }

    if (showDeleteConfirm && selectedShortcut != null) {
        ConfirmDialog(
            show = showDeleteConfirm,
            title = stringResource(R.string.server_confirm_delete_title),
            message = stringResource(R.string.server_confirm_delete_message, selectedShortcut?.name ?: ""),
            onConfirm = {
                shortKeySetupViewModel.removeShortKey(selectedShortcut!!)
                showDeleteConfirm = false
                selectedShortcut = null
            },
            onDismiss = {
                showDeleteConfirm = false
                selectedShortcut = null
            }
        )
    }
}
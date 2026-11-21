package com.ranhroi.esp32hidremote.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ranhroi.esp32hidremote.R
import com.ranhroi.esp32hidremote.models.InputErrorsModel
import com.ranhroi.esp32hidremote.models.ShortKeyModel
import com.ranhroi.esp32hidremote.ui.components.IconPickerDropdown
import com.ranhroi.esp32hidremote.ui.screens.shortkey.ShortKeySetupViewModel
import com.ranhroi.esp32hidremote.utils.ValidateUtils
import com.ranhroi.esp32hidremote.utils.playSoundClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogUpdateShortKey(
    selectedShortcut: ShortKeyModel,
    shortKeySetupViewModel: ShortKeySetupViewModel,
    onDismiss: () -> Unit = {},
) {
    val view = LocalView.current
    var isError by remember { mutableStateOf(InputErrorsModel()) }

    var state by remember { mutableStateOf(selectedShortcut) }

    var rawKeysInput by remember {
        mutableStateOf(state.keys.joinToString(", ") { it.joinToString("+") })
    }

    fun parseKeyInput(input: String): List<List<String>> {
        return input.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { combo ->
                combo.split("+").map { it.trim() }
            }
    }

    fun handleAddShortKeys() {
        state = state.copy(keys = parseKeyInput(rawKeysInput))
        ValidateUtils.validateShortKeyState(
            state,
            onDismiss,
            { isError = isError.copy(name = true) },
            { isError = isError.copy(keys = true) },
            { isError = isError.copy(icon = true) },
            onHandler = {
                shortKeySetupViewModel.updateShortKey(state)
            }
        )
        view.playSoundClick()
    }

    AlertDialog(
        modifier = Modifier
            .defaultMinSize()
            .padding(horizontal = 10.dp),
        shape = RoundedCornerShape(8.dp),
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(R.string.dialog_update_path_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IconPickerDropdown(
                    icon = state.icon,
                    onSetIcon = {
                        state = state.copy(icon = it)
                    },
                    onDone = { handleAddShortKeys() }
                )
                DialogBoxShortKey(
                    isError,
                    state,
                    rawKeysInput = rawKeysInput,
                    onValueChangeName = { newName ->
                        state = state.copy(name = newName)
                    },
                    onValueChangeKeys = { rawKeysInput = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { handleAddShortKeys() },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.dialog_path_update))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    view.playSoundClick()
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.dialog_edit_cancel))
            }
        }
    )
}
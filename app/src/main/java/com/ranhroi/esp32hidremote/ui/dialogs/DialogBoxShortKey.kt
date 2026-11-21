package com.ranhroi.esp32hidremote.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ranhroi.esp32hidremote.R
import com.ranhroi.esp32hidremote.models.InputErrorsModel
import com.ranhroi.esp32hidremote.models.ShortKeyModel

@Composable
fun DialogBoxShortKey(
    isError: InputErrorsModel,
    state: ShortKeyModel,
    rawKeysInput: String,
    onValueChangeName: (String) -> Unit,
    onValueChangeKeys: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            isError = isError.name,
            value = state.name,
            onValueChange = { newName ->
                onValueChangeName(newName)
            },
            label = { Text(stringResource(R.string.shortkey_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                if (state.name.isNotEmpty()) {
                    Text("(Mặc định)")
                }
            }
        )

        OutlinedTextField(
            isError = isError.keys,
            value = rawKeysInput,
            onValueChange = onValueChangeKeys,
            label = { Text(stringResource(R.string.shortkey_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 3
        )
    }
}
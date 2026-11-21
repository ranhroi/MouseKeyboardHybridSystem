package com.ranhroi.esp32hidremote.utils

import com.ranhroi.esp32hidremote.models.ShortKeyModel

object ValidateUtils {

    fun validateShortKeyState(
        state: ShortKeyModel,
        onDismiss: () -> Unit,
        onNameError: () -> Unit,
        onShortKeyError: () -> Unit,
        onIconError: () -> Unit,
        onHandler: () -> Unit = {}
    ) {
        val validators = listOf(
            state.name to onNameError,
            state.keys.joinToString(",") to onShortKeyError,
            state.icon to onIconError
        )

        for ((field, errorCallback) in validators) {
            if (field.isBlank()) {
                errorCallback()
                return
            }
        }

        if (state.keys.isNotEmpty()) {
            onHandler()
            onDismiss()
        } else {
            onShortKeyError()
        }
    }

}
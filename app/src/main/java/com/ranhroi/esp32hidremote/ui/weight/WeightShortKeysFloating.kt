package com.ranhroi.esp32hidremote.ui.weight

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.ranhroi.esp32hidremote.models.ShortKeyModel
import com.ranhroi.esp32hidremote.ui.components.ButtonWeight
import com.ranhroi.esp32hidremote.ui.components.SelectAppState
import com.ranhroi.esp32hidremote.ui.screens.server.send.InputManager
import com.ranhroi.esp32hidremote.utils.playSoundClick


@Composable
fun WeightShortKeysFloating(
    view: View,
    width: Int,
    color: Color = MaterialTheme.colorScheme.surface,
    isRotation: Boolean,
    isButtonText: Boolean,
    isBluetooth: Boolean,
    speedPressed: Long,
    state: SelectAppState,
    inputManager: InputManager,
    floatingShortKeys: List<ShortKeyModel>,
) {
    var containerH by remember { mutableStateOf(0.dp) }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .onGloballyPositioned { coordinates ->
                containerH = coordinates.size.height.dp
            }
    ) {
        Surface(
            modifier = Modifier
                .width(width.dp)
                .height(containerH),
            tonalElevation = 3.dp,
            color = color
        ) {
            LazyHorizontalGrid(
                rows = GridCells.Fixed(10),
                modifier = Modifier
                    .height(containerH),
                contentPadding = PaddingValues(vertical = 3.dp),
            ) {
                if (floatingShortKeys.isEmpty()) return@LazyHorizontalGrid

                itemsIndexed(floatingShortKeys) { index, shortKey ->
                    ButtonWeight(
                        modifier = Modifier.size(width.dp),
                        onClick = {
                            view.playSoundClick()
                            if (shortKey.name == "Keyboard") state.isCommandKey =
                                !state.isCommandKey else if (shortKey.keys.isNotEmpty()) inputManager.executeShortKey(
                                shortKey
                            )
                        },
                        onLongPressRepeat = {
                            if (shortKey.name in listOf("Undo", "Redo")) {
                                inputManager.executeShortKey(shortKey)
                                view.playSoundClick()
                            }
                        },
                        speedPressed = if (isBluetooth) speedPressed * 2 else speedPressed,
                        shape = RoundedCornerShape(0.dp),
                        colorStroke = Color.Transparent,
                        iconName = shortKey.icon,
                        iconScale = 0.7f,
                        text = if (isButtonText) shortKey.name.take(4) else null,
                        rotationDegrees = if (isRotation) 90f else 0f,
                        aspectRatio = 1f
                    )
                }
            }
        }
    }
}
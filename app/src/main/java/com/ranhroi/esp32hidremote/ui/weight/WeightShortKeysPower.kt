package com.ranhroi.esp32hidremote.ui.weight

import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ranhroi.esp32hidremote.R
import com.ranhroi.esp32hidremote.models.ShortKeyModel
import com.ranhroi.esp32hidremote.ui.components.ButtonWeight
import com.ranhroi.esp32hidremote.ui.components.ConfirmDialog
import com.ranhroi.esp32hidremote.ui.components.DotsLoadingIndicator
import com.ranhroi.esp32hidremote.ui.components.DotsOrientation
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsBluetooth
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsButtonText
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsRotation
import com.ranhroi.esp32hidremote.ui.keyboards.LocalSpeedPressed
import com.ranhroi.esp32hidremote.ui.screens.server.send.InputManager
import com.ranhroi.esp32hidremote.utils.playSoundClick
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun WeightShortKeysPower(
    inputManager: InputManager,
    powerShortKeys: List<ShortKeyModel>,
) {

    val view = LocalView.current
    val isRotation = LocalIsRotation.current

    var isLoading by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(5000.milliseconds) // chờ 5 giây
        isLoading = false
    }

    // Modifier linh hoạt theo cơ chế
    Column(
        modifier = if (isRotation) Modifier
            .wrapContentWidth()
            .fillMaxHeight() else Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        if (powerShortKeys.isNotEmpty()) {
            if (isRotation) {
                // 🔥 CƠ CHẾ 1: Drawer bên phải (ngang)
                PowerDrawerRightLayout(
                    view = view,
                    shortKeys = powerShortKeys,
                    inputManager = inputManager,
                )
            } else {
                // 🔥 CƠ CHẾ 2: Top center (dọc)
                PowerTopCenterLayout(
                    view = view,
                    shortKeys = powerShortKeys,
                    inputManager = inputManager,
                )
            }
        } else {
            if (isLoading) {
                // Loading với kích thước phù hợp
                Box(
                    modifier = Modifier
                        .wrapContentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    DotsLoadingIndicator(orientation = if (isRotation) DotsOrientation.VERTICAL else DotsOrientation.HORIZONTAL)
                }
            } else {
                //  Empty state với kích thước phù hợp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.power_shortkey_title))
                }
            }
        }
    }
}

// 🔥 CƠ CHẾ 1: Layout cho drawer bên phải (power buttons) - ĐÃ SỬA
@Composable
private fun PowerDrawerRightLayout(
    view: View,
    shortKeys: List<ShortKeyModel>,
    inputManager: InputManager,
) {
    val isBluetooth = LocalIsBluetooth.current
    val isButtonText = LocalIsButtonText.current
    val speedPressed = LocalSpeedPressed.current

    LazyHorizontalGrid(
        rows = GridCells.Fixed(8),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        reverseLayout = true
    ) {
        itemsIndexed(shortKeys) { _, shortKey ->
            var expanded by remember { mutableStateOf(false) }
            ButtonWeight(
                modifier = Modifier.width(50.dp),
                colorStroke = Color.Gray.copy(alpha = 0.6f),
                shape = RoundedCornerShape(3.dp),
                onClick = {
                    if (shortKey.name in listOf("Brightness Up", "Brightness Down", "Screenshot")) {
                        inputManager.executeShortKey(shortKey)
                        view.playSoundClick()
                    } else {
                        expanded = true
                    }
                },
                onLongPressRepeat = {
                    if (shortKey.name in listOf("Brightness Up", "Brightness Down")) {
                        inputManager.executeShortKey(shortKey)
                        view.playSoundClick()
                    }
                },
                speedPressed = if (isBluetooth) speedPressed * 2L else speedPressed,
                iconName = shortKey.icon,
                text = if (isButtonText) shortKey.name.take(4) else null,
                rotationDegrees = 90f,
                aspectRatio = 0.6f
            ) {
                ConfirmDialog(
                    show = expanded,
                    title = stringResource(
                        R.string.power_confirm_title,
                        shortKey.name.uppercase(LocalLocale.current.platformLocale)
                    ),
                    message = stringResource(
                        R.string.power_confirm_message, shortKey.name
                    ),
                    onConfirm = {
                        inputManager.executeShortKey(shortKey)
                        view.playSoundClick()
                    },
                    onDismiss = {
                        expanded = false
                    }
                )
            }
        }
    }
}

// 🔥 CƠ CHẾ 2: Layout cho top center (power buttons) - ĐÃ SỬA
@Composable
private fun PowerTopCenterLayout(
    view: View,
    shortKeys: List<ShortKeyModel>,
    inputManager: InputManager,
) {
    val isBluetooth = LocalIsBluetooth.current
    val isButtonText = LocalIsButtonText.current
    val speedPressed = LocalSpeedPressed.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(8),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        itemsIndexed(shortKeys) { _, shortKey ->
            var expanded by remember { mutableStateOf(false) }
            ButtonWeight(
                modifier = Modifier.wrapContentSize(),
                colorStroke = Color.Gray.copy(alpha = 0.6f),
                shape = RoundedCornerShape(3.dp),
                onClick = {
                    if (shortKey.name in listOf("Brightness Up", "Brightness Down", "Screenshot")) {
                        inputManager.executeShortKey(shortKey)
                        view.playSoundClick()
                    } else {
                        expanded = true
                    }
                },
                onLongPressRepeat = {
                    if (shortKey.name in listOf("Brightness Up", "Brightness Down")) {
                        inputManager.executeShortKey(shortKey)
                        view.playSoundClick()
                    }
                },
                speedPressed = if (isBluetooth) speedPressed * 2 else speedPressed,
                iconName = shortKey.icon,
                text = if (isButtonText) shortKey.name.take(4) else null,
                rotationDegrees = 0f
            ) {
                ConfirmDialog(
                    show = expanded,
                    title = stringResource(
                        R.string.power_confirm_title,
                        shortKey.name.uppercase(LocalLocale.current.platformLocale)
                    ),
                    message = stringResource(
                        R.string.power_confirm_message, shortKey.name
                    ),
                    onConfirm = {
                        inputManager.executeShortKey(shortKey)
                        view.playSoundClick()
                    },
                    onDismiss = {
                        expanded = false
                    }
                )
            }
        }
    }
}

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.ranhroi.esp32hidremote.models.ShortKeyModel
import com.ranhroi.esp32hidremote.ui.components.ButtonWeight
import com.ranhroi.esp32hidremote.ui.components.DotsLoadingIndicator
import com.ranhroi.esp32hidremote.ui.components.DotsOrientation
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsButtonText
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsRotation
import com.ranhroi.esp32hidremote.ui.screens.server.send.InputManager
import com.ranhroi.esp32hidremote.utils.playSoundClick
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds


@Composable
fun WeightShortKeysBrowser(
    inputManager: InputManager,
    browserShortKeys: List<ShortKeyModel>,
) {

    val view = LocalView.current
    val isRotation = LocalIsRotation.current

    var isLoading by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(5000.milliseconds)
        isLoading = false
    }

    DisposableEffect(Unit) {
        onDispose { }
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
        if (browserShortKeys.isNotEmpty()) {
            when (isRotation) {
                true -> BrowserDrawerLayout(
                    view = view,
                    shortKeys = browserShortKeys,
                    inputManager = inputManager
                )

                false -> {
                    BrowserTopCenterLayout(
                        view = view,
                        shortKeys = browserShortKeys,
                        inputManager = inputManager
                    )
                }
            }
        } else {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .wrapContentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    DotsLoadingIndicator(orientation = if (isRotation) DotsOrientation.VERTICAL else DotsOrientation.HORIZONTAL)
                }
            } else {
                Box(
                    modifier = Modifier
                        .wrapContentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No Browser shortKeys")
                }
            }
        }
    }
}

// 🔥 CƠ CHẾ 1: Layout cho drawer bên phải (ngang) - ĐÃ THÊM 2 NÚT SCAN VÀ SELECT
@Composable
fun BrowserDrawerLayout(
    view: View,
    shortKeys: List<ShortKeyModel>,
    inputManager: InputManager,
) {
    val isButtonText = LocalIsButtonText.current

    LazyHorizontalGrid(
        rows = GridCells.Fixed(6),
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
                iconName = shortKey.icon,
                text = if (isButtonText) shortKey.name.take(4) else null,
                rotationDegrees = 90f,
                aspectRatio = 0.6f
            )
        }
    }
}


// 🔥 CƠ CHẾ 2: Layout cho top center (dọc)
@Composable
private fun BrowserTopCenterLayout(
    view: View,
    shortKeys: List<ShortKeyModel>,
    inputManager: InputManager,
) {
    val isButtonText = LocalIsButtonText.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        itemsIndexed(shortKeys) { _, shortKey ->
            ButtonWeight(
                modifier = Modifier.wrapContentSize(),
                colorStroke = Color.Gray.copy(alpha = 0.6f),
                shape = RoundedCornerShape(3.dp),
                onClick = {
                    inputManager.executeShortKey(shortKey)
                    view.playSoundClick()
                },
                iconName = shortKey.icon,
                text = if (isButtonText) shortKey.name.take(4) else null,
            )
        }
    }
}

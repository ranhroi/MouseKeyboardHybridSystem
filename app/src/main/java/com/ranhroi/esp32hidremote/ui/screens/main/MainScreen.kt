package com.ranhroi.esp32hidremote.ui.screens.main

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.ranhroi.esp32hidremote.R
import com.ranhroi.esp32hidremote.models.ConnectionStatus
import com.ranhroi.esp32hidremote.ui.components.IconAny
import com.ranhroi.esp32hidremote.ui.components.SelectAppState
import com.ranhroi.esp32hidremote.ui.dialogs.ConnectionDialogsHost
import com.ranhroi.esp32hidremote.ui.dialogs.DevicePickerDialog
import com.ranhroi.esp32hidremote.ui.dialogs.HorizontalVirtualKeyboardDialog
import com.ranhroi.esp32hidremote.ui.drawer.DrawerContent
import com.ranhroi.esp32hidremote.ui.keyboards.LocalDelayText
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsAutoPing
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsBluetooth
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsButtonText
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsCapsLockOn
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsFloating
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsHID
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsLeftHand
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsNumLockOn
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsRotation
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsScrollDirection
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsScrollLockOn
import com.ranhroi.esp32hidremote.ui.keyboards.LocalLanguageCode
import com.ranhroi.esp32hidremote.ui.keyboards.LocalOsIndex
import com.ranhroi.esp32hidremote.ui.keyboards.LocalSpeedPressed
import com.ranhroi.esp32hidremote.ui.keyboards.VirtualKeyboard
import com.ranhroi.esp32hidremote.ui.navigation.NavRoute
import com.ranhroi.esp32hidremote.ui.screens.main.touch.TouchpadScreen
import com.ranhroi.esp32hidremote.ui.screens.quickkey.QuickKeysSetupViewModel
import com.ranhroi.esp32hidremote.ui.screens.server.ConnectionViewModel
import com.ranhroi.esp32hidremote.ui.screens.server.NetworkStatusViewModel
import com.ranhroi.esp32hidremote.ui.screens.shortkey.ShortKeySetupViewModel
import com.ranhroi.esp32hidremote.ui.theme.ObsidianBg
import com.ranhroi.esp32hidremote.ui.weight.WeightQuickKeys
import com.ranhroi.esp32hidremote.ui.weight.WeightShortKeysBrowser
import com.ranhroi.esp32hidremote.ui.weight.WeightShortKeysMedia
import com.ranhroi.esp32hidremote.ui.weight.WeightShortKeysPower
import com.ranhroi.esp32hidremote.utils.ConstantsUtils.bottomNavRepository
import com.ranhroi.esp32hidremote.utils.IconsMap
import com.ranhroi.esp32hidremote.utils.playSoundClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("UnrememberedGetBackStackEntry")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    connection: ConnectionViewModel,
    networkStatus: NetworkStatusViewModel,
    quickKeysSetupViewModel: QuickKeysSetupViewModel = hiltViewModel(),
    shortKeySetupViewModel: ShortKeySetupViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val config by connection.config.collectAsStateWithLifecycle()
    val hasServers by connection.hasServers.collectAsStateWithLifecycle()
    val isLoadingServers by connection.isLoadingServers.collectAsStateWithLifecycle()

    val connectionStatus by connection.connectionStatus.collectAsStateWithLifecycle()
    val uiState by connection.uiState.collectAsStateWithLifecycle()

    val inputManager by connection.inputState.collectAsStateWithLifecycle()
    val languageCode by inputManager.keyboardLanguageCode.collectAsState()

    var showDevicePicker by rememberSaveable { mutableStateOf(false) }

    // ============================================================
    // KEYBOARD & UI STATE
    // ============================================================
    val keyboardController = LocalSoftwareKeyboardController.current
    val selectState = remember { SelectAppState() }

    val isDrawerMode = config.isRotation && !selectState.isCommandKey
    val isVisible =
        selectState.isCommandKey || (selectState.bottomIndex != -1 && selectState.bottomIndex != 2)

    // ============================================================
    // XỬ LÝ KHI CHƯA CÓ SERVER
    // ============================================================
    if (!hasServers && !isLoadingServers) {
        NoServerScreen(navController)
        return
    }

    val allQuickKeys by quickKeysSetupViewModel.allQuickKeys.collectAsState()

    val allShortKeys by shortKeySetupViewModel.allShortKeys.collectAsState()

    val browserShortKeys = remember(allShortKeys) {
        allShortKeys.filter { it.type == "browser" }
    }
    val powerShortKeys = remember(allShortKeys) {
        allShortKeys.filter { it.type == "power" }
    }
    val floatingShortKeys = remember(allShortKeys) {
        allShortKeys.filter { it.type == "floating" }
    }


    BackHandler {
        scope.launch {
            when {
                drawerState.isOpen -> {
                    drawerState.close()
                    selectState.resetState()
                }

                selectState.bottomIndex == 2 -> {
                    selectState.resetState()
                    keyboardController?.hide()
                }

                else -> selectState.resetState()
            }
        }
    }

    LifecycleResumeEffect(Unit) {
        onPauseOrDispose {
            selectState.resetState()
        }
    }

    LaunchedEffect(selectState.bottomIndex) {
        when (selectState.bottomIndex) {
            2 -> {
                delay(50.milliseconds)
                keyboardController?.show()
            }

            else -> {
                selectState.isCommandKey = false
                keyboardController?.hide()
            }
        }
    }

    DisposableEffect(Unit) {
        inputManager.updateKeyboardLanguage()
        onDispose {
            selectState.resetState()
            scope.launch {
                if (drawerState.isOpen) drawerState.close()
            }
        }
    }

    ConnectionDialogsHost(viewModel = connection)

    // ============================================================
    // COMPOSITION LOCAL PROVIDER — bọc toàn bộ cây UI
    // ============================================================
    CompositionLocalProvider(
        // ── Nhóm 1: Server & kết nối ──
        LocalIsHID provides uiState.state.server.isHID,
        LocalIsBluetooth provides uiState.state.server.bluetooth,

        // ── Nhóm 2: LED status ──
        LocalIsCapsLockOn provides uiState.isCapsLockOn,
        LocalIsNumLockOn provides uiState.isNumLockOn,
        LocalIsScrollLockOn provides uiState.isScrollLockOn,
        LocalLanguageCode provides languageCode,

        // ── Nhóm 3: Cấu hình bàn phím ──
        LocalIsRotation provides config.isRotation,
        LocalIsLeftHand provides config.isLeftHand,
        LocalOsIndex provides config.osIndex,
        LocalIsFloating provides config.isFloating,
        LocalIsAutoPing provides config.isAutoPing,
        LocalIsScrollDirection provides config.isScrollDirection,
        LocalIsButtonText provides config.isButtonText,

        // ── Nhóm 4: Tốc độ & delay ──
        LocalSpeedPressed provides config.speedPressed,
        LocalDelayText provides config.delayText,
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = false,
            drawerContent = {
                DrawerContent(navController, drawerState, networkStatus, connection)
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { },
                        actions = {
                            ConnectionStatusBar(
                                networkStatus = networkStatus,
                                uiState = uiState,
                                status = connectionStatus,
                                statusText = uiState.statusMessage.ifBlank { stringResource(R.string.connection_not_connected) },
                                rssi = null,
                                onToggleScan = {
                                    when (connectionStatus) {
                                        ConnectionStatus.CONNECTED -> connection.disconnect()
                                        else -> connection.pingConnection()
                                    }
                                },
                                onOpenDeviceList = {
                                    showDevicePicker = true
                                },
                                onRequestEnableNetwork = {
                                    networkStatus.checkWifiAndRequest()
                                }
                            )
                        }
                    )
                },
                bottomBar = {
                    NavigationBar {
                        bottomNavRepository.forEachIndexed { index, item ->
                            val icon = remember(item.icon) { IconsMap.getIconOrDefault(item.icon) }
                            NavigationBarItem(
                                selected = selectState.bottomIndex == index,
                                onClick = {
                                    selectState.bottomIndex =
                                        if (selectState.bottomIndex == index) -1 else index
                                    if (selectState.bottomIndex == 0) {
                                        scope.launch {
                                            if (drawerState.isClosed) drawerState.open()
                                            else {
                                                drawerState.close()
                                                selectState.resetState()
                                            }
                                        }
                                    }
                                    view.playSoundClick()
                                },
                                icon = {
                                    IconAny(
                                        text = item.icon,
                                        icon = icon,
                                        modifier = Modifier.size(19.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(item.nameRes),
                                        maxLines = 1,
                                        textAlign = TextAlign.Center,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            )
                        }
                    }

                    if (selectState.bottomIndex == 2) {
                        VirtualKeyboard(view, context, inputManager)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBg),
                containerColor = ObsidianBg,
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    TouchpadScreen(
                        selectState = selectState,
                        inputManager = inputManager,
                        floatingShortKeys = floatingShortKeys,
                        connection = connection
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(5f)
                    ) {
                        AnimatedVisibility(
                            modifier = Modifier
                                .align(
                                    if (isDrawerMode) Alignment.CenterEnd
                                    else Alignment.TopCenter
                                ),
                            visible = isVisible,
                            enter = if (isDrawerMode) {
                                slideInHorizontally(
                                    initialOffsetX = { it },
                                    animationSpec = tween(durationMillis = 400)
                                ) + fadeIn()
                            } else {
                                slideInVertically(
                                    initialOffsetY = { -it },
                                    animationSpec = tween(durationMillis = 400)
                                ) + fadeIn()
                            },
                            exit = if (isDrawerMode) {
                                slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(durationMillis = 400)
                                ) + fadeOut()
                            } else {
                                slideOutVertically(
                                    targetOffsetY = { -it },
                                    animationSpec = tween(durationMillis = 400)
                                ) + fadeOut()
                            }
                        ) {
                            Card(
                                modifier = if (isDrawerMode) {
                                    Modifier
                                        .fillMaxHeight()
                                        .wrapContentWidth()
                                } else {
                                    Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                },
                                shape = if (isDrawerMode) {
                                    RoundedCornerShape(
                                        topStart = 5.dp,
                                        bottomStart = 5.dp
                                    )
                                } else {
                                    RoundedCornerShape(
                                        bottomStart = 5.dp,
                                        bottomEnd = 5.dp
                                    )
                                }
                            ) {
                                when (selectState.bottomIndex) {
                                    1 -> WeightQuickKeys(
                                        navController,
                                        inputManager,
                                        allQuickKeys,
                                        quickKeysSetupViewModel
                                    )

                                    3 -> WeightShortKeysMedia(
                                        uiState.state,
                                        inputManager,
                                        allShortKeys,
                                        shortKeySetupViewModel
                                    )

                                    4 -> WeightShortKeysBrowser(inputManager, browserShortKeys)
                                    5 -> WeightShortKeysPower(inputManager, powerShortKeys)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showDevicePicker) {
            DevicePickerDialog(
                view = view,
                networkStatus = networkStatus,
                connection = connection,
                devices = uiState.servers,
                isLoading = uiState.isLoading,
                onSelectDevice = { device ->
                    connection.connectToServer(device)
                    showDevicePicker = false
                },
                onOpenServerScreen = {
                    navController.navigate(NavRoute.SetServer.route)
                    showDevicePicker = false
                },
                onDismiss = { showDevicePicker = false }
            )
        }

        if (selectState.isCommandKey) {
            HorizontalVirtualKeyboardDialog(
                inputManager = inputManager,
                onDismiss = { selectState.resetState() }
            )
        }
    }
}

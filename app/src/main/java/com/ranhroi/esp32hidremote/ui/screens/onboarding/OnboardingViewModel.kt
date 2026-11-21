package com.ranhroi.esp32hidremote.ui.screens.onboarding

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ranhroi.esp32hidremote.ui.navigation.NavRoute
import com.ranhroi.esp32hidremote.ui.screens.quickkey.QuickKeysRepository
import com.ranhroi.esp32hidremote.ui.screens.shortkey.ShortKeysRepository
import com.ranhroi.esp32hidremote.utils.ConstantsUtils.FIRST_LAUNCH
import com.ranhroi.esp32hidremote.utils.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val dataStore: DataStoreManager,
) : ViewModel() {
    private val _startDestination: MutableState<String> = mutableStateOf(NavRoute.Loading.route)
    val startDestination: State<String> = _startDestination

    init {
        viewModelScope.launch {

            dataStore.get(FIRST_LAUNCH, true).collect { completed ->
                delay(500.milliseconds)
                if (completed) {
                    _startDestination.value = NavRoute.Onboarding.route
                    delay(1000.milliseconds)
                    checkAndInitDefaultShortKeys()
                    delay(1000.milliseconds)
                    checkAndInitDefaultQuickKeys()
                } else {
                    _startDestination.value = NavRoute.Main.route
                }
            }
        }
    }

    fun updateOnboarding(b: Boolean) {
        viewModelScope.launch { dataStore.put(FIRST_LAUNCH, b) }
    }

    // ============================================================
    // INIT DEFAULT SHORTKEYS
    // ============================================================
    private fun checkAndInitDefaultShortKeys() {
        viewModelScope.launch {
            val currentShortKeys = dataStore.getAllShortKeys().first()
            if (currentShortKeys.isEmpty()) {
                dataStore.addShortKeyList(ShortKeysRepository.shortKeysDefault)
            }
        }
    }


    // ============================================================
    // KHỞI TẠO DEFAULT — chỉ khi DataStore rỗng
    // ============================================================
    private fun checkAndInitDefaultQuickKeys() {
        viewModelScope.launch {
            val currentQuickKeys = dataStore.getAllQuickKeys().first()
            if (currentQuickKeys.isEmpty()) {
                dataStore.addQuickKeysList(QuickKeysRepository.quickKeysDefault)
            }
        }
    }

}
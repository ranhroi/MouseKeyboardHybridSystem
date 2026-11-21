package com.ranhroi.esp32hidremote.ui.screens.quickkey

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ranhroi.esp32hidremote.models.CombineSettingModel
import com.ranhroi.esp32hidremote.models.QuickKeyModel
import com.ranhroi.esp32hidremote.utils.ConstantsUtils
import com.ranhroi.esp32hidremote.utils.DataStoreConfig
import com.ranhroi.esp32hidremote.utils.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale.getDefault
import javax.inject.Inject

@HiltViewModel
class QuickKeysSetupViewModel @Inject constructor(
    private val dataStore: DataStoreManager,
    private val dataStoreConfig: DataStoreConfig,
) : ViewModel() {

    // ============================================================
    // CONFIG
    // ============================================================
    val config = dataStoreConfig.combine.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(500),
        CombineSettingModel()
    )

    // ============================================================
    // DANH SÁCH QUICK KEY
    // ============================================================
    val allQuickKeys: StateFlow<List<QuickKeyModel>> = dataStore.getAllQuickKeys()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _editingQuickKey = MutableStateFlow(QuickKeyModel())
    val editingQuickKey: StateFlow<QuickKeyModel> = _editingQuickKey.asStateFlow()

    private val _selectedKeys = MutableStateFlow<List<String>>(emptyList())
    val selectedKeys: StateFlow<List<String>> = _selectedKeys.asStateFlow()

    /**
     * Preset đang chọn — đọc trực tiếp từ DataStore.
     * null = "All" (hiển thị tất cả).
     */
    val selectedPreset: StateFlow<String?> = dataStore
        .get(ConstantsUtils.SELECTED_PRESET_ID, "all")
        .map { saved -> if (saved == "all" || saved.isBlank()) null else saved }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // ============================================================
    // CRUD
    // ============================================================
    fun addQuickKey(quickKey: QuickKeyModel) {
        viewModelScope.launch {
            try {
                dataStore.saveQuickKey(quickKey)
            } catch (e: Exception) {
                Log.e("QuickKeysVM", "Lỗi khi lưu quick key", e)
            }
        }
    }

    fun removeQuickKey(quickKey: QuickKeyModel) {
        viewModelScope.launch {
            try {
                dataStore.deleteQuickKey(quickKey.id)
            } catch (e: Exception) {
                Log.e("QuickKeysVM", "Lỗi khi xóa quick key", e)
            }
        }
    }

    fun updateQuickKey(quickKey: QuickKeyModel) {
        addQuickKey(quickKey)
    }

    // ============================================================
    // EDIT STATE
    // ============================================================
    fun startNewQuickKey(openApp: Boolean = false) {
        _editingQuickKey.value = QuickKeyModel(openApp = openApp)
        _selectedKeys.value = emptyList()
    }

    fun startEditQuickKey(quickKey: QuickKeyModel) {
        _editingQuickKey.value = quickKey
        _selectedKeys.value = quickKey.keys.getOrElse(config.value.osIndex) { emptyList() }
    }

    fun setName(name: String) {
        _editingQuickKey.value = _editingQuickKey.value.copy(name = name)
    }

    fun setIcon(icon: String) {
        _editingQuickKey.value = _editingQuickKey.value.copy(icon = icon)
    }

    fun setType(openApp: Boolean) {
        _editingQuickKey.value = _editingQuickKey.value.copy(openApp = openApp)
    }

    // ============================================================
    // PRESET — chỉ ghi DataStore, UI tự react qua selectedPreset
    // ============================================================
    fun selectPreset(presetName: String?) {
        _editingQuickKey.value = _editingQuickKey.value.copy(preset = presetName ?: "")
        viewModelScope.launch {
            dataStore.put(ConstantsUtils.SELECTED_PRESET_ID, presetName ?: "all")
        }
    }

    fun selectAllPresets() {
        selectPreset(null)
    }

    fun addAndSelectPreset(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return

        val existingNames = allQuickKeys.value
            .map { it.preset.trim().lowercase(getDefault()) }
            .filter { it.isNotBlank() }
            .toSet()

        var finalName = trimmed
        var suffix = 1
        while (existingNames.contains(finalName.trim().lowercase(getDefault()))) {
            finalName = "$trimmed ${suffix++}"
        }

        _editingQuickKey.value = _editingQuickKey.value.copy(preset = finalName)
        viewModelScope.launch {
            dataStore.put(ConstantsUtils.SELECTED_PRESET_ID, finalName)
        }
    }

    /**
     * Danh sách preset distinct (từ data + default), sắp xếp theo tên.
     * @param openApp null = tất cả, true = chỉ launch, false = chỉ shortcut.
     */
    fun getAvailablePresets(openApp: Boolean? = null): List<String> {
        val fromData = allQuickKeys.value
            .filter { it.preset.isNotBlank() }
            .filter { openApp == null || it.openApp == openApp }
            .map { it.preset }

        val fromDefault = QuickKeysRepository.quickKeysDefault
            .filter { it.preset.isNotBlank() }
            .filter { openApp == null || it.openApp == openApp }
            .map { it.preset }

        return (fromData + fromDefault).distinct().sorted()
    }

    // ============================================================
    // KEY SELECTION
    // ============================================================
    fun toggleKey(code: String) {
        val current = _selectedKeys.value
        _selectedKeys.value = if (current.contains(code)) current - code else current + code
    }

    fun isKeySelected(code: String): Boolean = _selectedKeys.value.contains(code)

    fun removeKey(code: String) {
        _selectedKeys.value -= code
    }

    fun clearKeys() {
        _selectedKeys.value = emptyList()
    }

    // ============================================================
    // SAVE / CANCEL
    // ============================================================
    fun saveEditingQuickKey(): Boolean {
        val editing = _editingQuickKey.value
        if (editing.name.isBlank()) return false
        if (_selectedKeys.value.isEmpty()) return false
        if (editing.preset.isBlank()) return false

        val currentKeys = editing.keys.toMutableList()
        while (currentKeys.size < 3) currentKeys.add(emptyList())
        currentKeys[config.value.osIndex] = _selectedKeys.value

        val toSave = editing.copy(keys = currentKeys)

        if (toSave.id == 0) addQuickKey(toSave) else updateQuickKey(toSave)

        _editingQuickKey.value = QuickKeyModel()
        _selectedKeys.value = emptyList()
        return true
    }

    fun cancelEditing() {
        _editingQuickKey.value = QuickKeyModel()
        _selectedKeys.value = emptyList()
    }

    // ============================================================
    // PREVIEW
    // ============================================================
    fun previewKeys(): String = _selectedKeys.value.joinToString(" + ")
}
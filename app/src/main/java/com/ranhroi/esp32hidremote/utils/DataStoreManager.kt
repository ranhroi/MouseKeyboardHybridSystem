package com.ranhroi.esp32hidremote.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ranhroi.esp32hidremote.models.DeviceServerModel
import com.ranhroi.esp32hidremote.models.QuickKeyModel
import com.ranhroi.esp32hidremote.models.ShortKeyModel
import com.ranhroi.esp32hidremote.utils.ConstantsUtils.DEVICE_LIST_KEY
import com.ranhroi.esp32hidremote.utils.ConstantsUtils.QUICK_KEY_LIST_KEY
import com.ranhroi.esp32hidremote.utils.ConstantsUtils.SHORTKEY_LIST_KEY
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "config")


@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore
    private val gson = Gson()

    val data: Flow<Preferences> =
        dataStore.data.catch { e ->
            if (e is IOException) {
                emit(emptyPreferences())
            } else {
                throw e
            }
        }

    // ---------- HÀM XỬ LÝ GENERIC ----------
    private fun <T> getList(key: Preferences.Key<String>, clazz: Class<T>): Flow<List<T>> {
        return dataStore.data.catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }.map { pref ->
            val json = pref[key] ?: ""
            if (json.isEmpty()) {
                emptyList<T>()
            } else {
                val type = TypeToken.getParameterized(List::class.java, clazz).type
                gson.fromJson(json, type) ?: emptyList()
            }
        }
    }

    private suspend fun <T> saveOrUpdateItem(
        key: Preferences.Key<String>,
        item: T,
        clazz: Class<T>,
        getId: (T) -> Int,
        updateId: (T, Int) -> T
    ) {
        val json = dataStore.data.first()[key] ?: ""
        val list: MutableList<T> = if (json.isEmpty()) {
            mutableListOf()
        } else {
            val type = TypeToken.getParameterized(MutableList::class.java, clazz).type
            gson.fromJson(json, type)
        }

        val itemId = getId(item)

        // ✅ Tìm index dựa trên ID (chỉ khi ID > 0)
        val index = if (itemId > 0) {
            list.indexOfFirst { getId(it) == itemId }
        } else {
            -1
        }

        if (index != -1) {
            // ✅ Cập nhật: giữ nguyên ID cũ, không tạo mới
            list[index] = item
        } else {
            // ✅ Thêm mới: tạo ID mới dựa trên maxId + 1
            val maxId = list.maxOfOrNull { getId(it) } ?: 0
            val newItem = updateId(item, maxId + 1)
            list.add(newItem)
        }

        put(key, gson.toJson(list))
    }

    suspend fun deleteItem(key: Preferences.Key<String>, id: Int) {
        val json = dataStore.data.first()[key] ?: ""
        val type = object : TypeToken<MutableList<MutableMap<String, Any>>>() {}.type
        val rawList: MutableList<MutableMap<String, Any>> =
            gson.fromJson(json, type) ?: mutableListOf()
        if (rawList.removeAll { (it["id"] as? Double)?.toInt() == id }) {
            put(key, gson.toJson(rawList))
        }
    }

    // ✅ Xóa danh sách của 1 Key cụ thể - Trả về true nếu thành công
    suspend fun clearAllItems(key: Preferences.Key<String>): Boolean {
        return try {
            dataStore.edit { pref ->
                pref.remove(key)
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    // ✅ Xóa toàn bộ DataStore (Clear sạch) - Trả về true nếu thành công
    suspend fun clearEverything(): Boolean {
        return try {
            dataStore.edit { pref ->
                pref.clear()
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    // ==================== DEVICE ====================
    fun getAllDevices(): Flow<List<DeviceServerModel>> =
        getList(DEVICE_LIST_KEY, DeviceServerModel::class.java)

    suspend fun saveDevice(item: DeviceServerModel) =
        saveOrUpdateItem(
            DEVICE_LIST_KEY,
            item,
            DeviceServerModel::class.java,
            { it.id },
            { obj, newId -> obj.copy(id = newId) })


    suspend fun setSelectedDevice(deviceId: Int) {
        val currentList = getAllDevices().first()
        val updatedList =
            currentList.map { device -> device.copy(selected = (device.id == deviceId)) }
        put(DEVICE_LIST_KEY, gson.toJson(updatedList))
    }


    fun getSelectedDevice(): Flow<DeviceServerModel?> {
        return getAllDevices().map { list -> list.find { it.selected } }
    }

    // ==================== ĐỒNG BỘ OS INDEX ====================
    /**
     * Cập nhật `os` của TẤT CẢ device trong DEVICE_LIST.
     * Gọi khi user đổi OS trong Settings.
     */
    suspend fun updateAllDevicesOs(newOsIndex: Int) {
        val currentList = getAllDevices().first()
        val updatedList = currentList.map { it.copy(os = newOsIndex) }
        put(DEVICE_LIST_KEY, gson.toJson(updatedList))
    }

    suspend fun deleteDevice(id: Int) = deleteItem(DEVICE_LIST_KEY, id)
    suspend fun deleteAllDevices(): Boolean = clearAllItems(DEVICE_LIST_KEY)


    // ==================== QUICK KEY ====================
    fun getAllQuickKeys(): Flow<List<QuickKeyModel>> =
        getList(QUICK_KEY_LIST_KEY, QuickKeyModel::class.java)

    suspend fun addQuickKeysList(newQuickKeys: List<QuickKeyModel>) {
        val currentList = getAllQuickKeys().first().toMutableList()
        var nextId = (currentList.maxOfOrNull { it.id } ?: 0) + 1
        newQuickKeys.forEach { item ->
            if (currentList.none { (it.name == item.name) && (it.preset == item.preset) }) {
                currentList.add(item.copy(id = nextId++))
            }
        }
        put(QUICK_KEY_LIST_KEY, gson.toJson(currentList))
    }

    suspend fun saveQuickKey(item: QuickKeyModel) = saveOrUpdateItem(
        QUICK_KEY_LIST_KEY,
        item,
        QuickKeyModel::class.java,
        { it.id },
        { obj, newId -> obj.copy(id = newId) })

    suspend fun updateQuickKeyOrder(newList: List<QuickKeyModel>) {
        put(QUICK_KEY_LIST_KEY, gson.toJson(newList))
    }

    suspend fun deleteQuickKey(id: Int) = deleteItem(QUICK_KEY_LIST_KEY, id)
    suspend fun deleteQuickKeys(): Boolean = clearAllItems(QUICK_KEY_LIST_KEY)


    // ==================== SHORTCUT ====================
    fun getAllShortKeys(): Flow<List<ShortKeyModel>> =
        getList(SHORTKEY_LIST_KEY, ShortKeyModel::class.java)

    suspend fun addShortKeyList(newShortKeys: List<ShortKeyModel>) {
        val currentList = getAllShortKeys().first().toMutableList()
        var nextId = (currentList.maxOfOrNull { it.id } ?: 0) + 1
        newShortKeys.forEach { item ->
            if (currentList.none { it.type == item.type && it.name == item.name }) {
                currentList.add(item.copy(id = nextId++))
            }
        }
        put(SHORTKEY_LIST_KEY, gson.toJson(currentList))
    }

    suspend fun saveShortKey(item: ShortKeyModel) = saveOrUpdateItem(
        SHORTKEY_LIST_KEY,
        item,
        ShortKeyModel::class.java,
        { it.id },
        { obj, newId -> obj.copy(id = newId) })

    suspend fun updateShortKeyOrder(newList: List<ShortKeyModel>) {
        put(SHORTKEY_LIST_KEY, gson.toJson(newList))
    }

    suspend fun deleteShortKey(id: Int) = deleteItem(SHORTKEY_LIST_KEY, id)

    suspend fun deleteShortKeys(): Boolean = clearAllItems(SHORTKEY_LIST_KEY)

    // ==================== READ / WRITE CƠ BẢN ====================
    fun <T> get(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
        return dataStore.data.map { pref -> pref[key] ?: defaultValue }
    }

    suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        dataStore.edit { pref -> pref[key] = value }
    }

    suspend fun <T> remove(key: Preferences.Key<T>) {
        dataStore.edit { it.remove(key) }
    }


    // ==================== JSON OPERATIONS ====================

    suspend fun <T> putJson(key: String, value: T) {
        val json = gson.toJson(value)
        dataStore.edit { pref ->
            pref[stringPreferencesKey(key)] = json
        }
    }

    suspend fun <T> getJson(key: String, clazz: Class<T>): T? {
        val json = dataStore.data.first()[stringPreferencesKey(key)] ?: return null
        return try {
            gson.fromJson(json, clazz)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun <T> getJson(key: String, typeToken: TypeToken<T>): T? {
        val json = dataStore.data.first()[stringPreferencesKey(key)] ?: return null
        return try {
            gson.fromJson(json, typeToken.type)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun remove(key: String) {
        dataStore.edit { pref ->
            pref.remove(stringPreferencesKey(key))
        }
    }
}

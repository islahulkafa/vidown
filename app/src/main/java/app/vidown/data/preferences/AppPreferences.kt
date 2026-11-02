package app.vidown.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _maxConcurrentDownloads = MutableStateFlow(
        prefs.getInt(KEY_MAX_CONCURRENT_DOWNLOADS, DEFAULT_MAX_CONCURRENT_DOWNLOADS)
    )
    val maxConcurrentDownloads: StateFlow<Int> = _maxConcurrentDownloads.asStateFlow()

    private val _useCookies = MutableStateFlow(
        prefs.getBoolean(KEY_USE_COOKIES, true)
    )
    val useCookies: StateFlow<Boolean> = _useCookies.asStateFlow()

    private val _useCustomStorage = MutableStateFlow(
        prefs.getBoolean(KEY_USE_CUSTOM_STORAGE, false)
    )
    val useCustomStorage: StateFlow<Boolean> = _useCustomStorage.asStateFlow()

    private val _customStorageUri = MutableStateFlow(
        prefs.getString(KEY_CUSTOM_STORAGE_URI, null)
    )
    val customStorageUri: StateFlow<String?> = _customStorageUri.asStateFlow()

    private val _organizeByCreator = MutableStateFlow(
        prefs.getBoolean(KEY_ORGANIZE_BY_CREATOR, false)
    )
    val organizeByCreator: StateFlow<Boolean> = _organizeByCreator.asStateFlow()

    fun setMaxConcurrentDownloads(limit: Int) {
        val clamped = limit.coerceIn(MIN_CONCURRENT_DOWNLOADS, MAX_CONCURRENT_DOWNLOADS)
        prefs.edit().putInt(KEY_MAX_CONCURRENT_DOWNLOADS, clamped).apply()
        _maxConcurrentDownloads.value = clamped
    }

    fun getMaxConcurrentDownloads(): Int {
        return _maxConcurrentDownloads.value
    }

    fun setUseCookies(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_USE_COOKIES, enabled).apply()
        _useCookies.value = enabled
    }

    fun isUseCookies(): Boolean {
        return _useCookies.value
    }

    fun setUseCustomStorage(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_USE_CUSTOM_STORAGE, enabled).apply()
        _useCustomStorage.value = enabled
    }

    fun isUseCustomStorage(): Boolean = _useCustomStorage.value

    fun setCustomStorageUri(uriString: String?) {
        prefs.edit().putString(KEY_CUSTOM_STORAGE_URI, uriString).apply()
        _customStorageUri.value = uriString
    }

    fun getCustomStorageUri(): String? = _customStorageUri.value

    fun setOrganizeByCreator(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ORGANIZE_BY_CREATOR, enabled).apply()
        _organizeByCreator.value = enabled
    }

    fun isOrganizeByCreator(): Boolean = _organizeByCreator.value

    companion object {
        private const val PREFS_NAME = "vidown_preferences"
        private const val KEY_MAX_CONCURRENT_DOWNLOADS = "max_concurrent_downloads"
        private const val KEY_USE_COOKIES = "use_cookies"
        private const val KEY_USE_CUSTOM_STORAGE = "use_custom_storage"
        private const val KEY_CUSTOM_STORAGE_URI = "custom_storage_uri"
        private const val KEY_ORGANIZE_BY_CREATOR = "organize_by_creator"

        const val MIN_CONCURRENT_DOWNLOADS = 1
        const val MAX_CONCURRENT_DOWNLOADS = 4
        const val DEFAULT_MAX_CONCURRENT_DOWNLOADS = 2

        @Volatile
        private var instance: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return instance ?: synchronized(this) {
                instance ?: AppPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}

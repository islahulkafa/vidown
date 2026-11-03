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

    private val _downloadOnlyOnWifi = MutableStateFlow(
        prefs.getBoolean(KEY_DOWNLOAD_ONLY_ON_WIFI, false)
    )
    val downloadOnlyOnWifi: StateFlow<Boolean> = _downloadOnlyOnWifi.asStateFlow()

    private val _downloadSpeedLimitBytes = MutableStateFlow(
        prefs.getLong(KEY_DOWNLOAD_SPEED_LIMIT_BYTES, 0L)
    )
    val downloadSpeedLimitBytes: StateFlow<Long> = _downloadSpeedLimitBytes.asStateFlow()

    private val _defaultSubtitleDeliveryMode = MutableStateFlow(
        prefs.getString(KEY_DEFAULT_SUBTITLE_DELIVERY_MODE, "embed") ?: "embed"
    )
    val defaultSubtitleDeliveryMode: StateFlow<String> = _defaultSubtitleDeliveryMode.asStateFlow()

    private val _defaultSubtitleFormat = MutableStateFlow(
        prefs.getString(KEY_DEFAULT_SUBTITLE_FORMAT, "srt") ?: "srt"
    )
    val defaultSubtitleFormat: StateFlow<String> = _defaultSubtitleFormat.asStateFlow()

    private val _includeAutoCaptions = MutableStateFlow(
        prefs.getBoolean(KEY_INCLUDE_AUTO_CAPTIONS, true)
    )
    val includeAutoCaptions: StateFlow<Boolean> = _includeAutoCaptions.asStateFlow()

    private val _preferredSubtitleLangs = MutableStateFlow(
        prefs.getStringSet(KEY_PREFERRED_SUBTITLE_LANGS, setOf("en")) ?: setOf("en")
    )
    val preferredSubtitleLangs: StateFlow<Set<String>> = _preferredSubtitleLangs.asStateFlow()

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

    fun setDownloadOnlyOnWifi(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DOWNLOAD_ONLY_ON_WIFI, enabled).apply()
        _downloadOnlyOnWifi.value = enabled
    }

    fun isDownloadOnlyOnWifi(): Boolean = _downloadOnlyOnWifi.value

    fun setDownloadSpeedLimitBytes(bytes: Long) {
        val positiveOrZero = bytes.coerceAtLeast(0L)
        prefs.edit().putLong(KEY_DOWNLOAD_SPEED_LIMIT_BYTES, positiveOrZero).apply()
        _downloadSpeedLimitBytes.value = positiveOrZero
    }

    fun getDownloadSpeedLimitBytes(): Long = _downloadSpeedLimitBytes.value

    fun setDefaultSubtitleDeliveryMode(mode: String) {
        val validMode = if (mode.lowercase() == "external") "external" else "embed"
        prefs.edit().putString(KEY_DEFAULT_SUBTITLE_DELIVERY_MODE, validMode).apply()
        _defaultSubtitleDeliveryMode.value = validMode
    }

    fun getDefaultSubtitleDeliveryMode(): String = _defaultSubtitleDeliveryMode.value

    fun setDefaultSubtitleFormat(format: String) {
        val validFormat = if (format.lowercase() == "vtt") "vtt" else "srt"
        prefs.edit().putString(KEY_DEFAULT_SUBTITLE_FORMAT, validFormat).apply()
        _defaultSubtitleFormat.value = validFormat
    }

    fun getDefaultSubtitleFormat(): String = _defaultSubtitleFormat.value

    fun setIncludeAutoCaptions(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_INCLUDE_AUTO_CAPTIONS, enabled).apply()
        _includeAutoCaptions.value = enabled
    }

    fun isIncludeAutoCaptions(): Boolean = _includeAutoCaptions.value

    fun setPreferredSubtitleLangs(langs: Set<String>) {
        prefs.edit().putStringSet(KEY_PREFERRED_SUBTITLE_LANGS, langs).apply()
        _preferredSubtitleLangs.value = langs
    }

    fun getPreferredSubtitleLangs(): Set<String> = _preferredSubtitleLangs.value

    companion object {
        private const val PREFS_NAME = "vidown_preferences"
        private const val KEY_MAX_CONCURRENT_DOWNLOADS = "max_concurrent_downloads"
        private const val KEY_USE_COOKIES = "use_cookies"
        private const val KEY_USE_CUSTOM_STORAGE = "use_custom_storage"
        private const val KEY_CUSTOM_STORAGE_URI = "custom_storage_uri"
        private const val KEY_ORGANIZE_BY_CREATOR = "organize_by_creator"
        private const val KEY_DOWNLOAD_ONLY_ON_WIFI = "download_only_on_wifi"
        private const val KEY_DOWNLOAD_SPEED_LIMIT_BYTES = "download_speed_limit_bytes"
        private const val KEY_DEFAULT_SUBTITLE_DELIVERY_MODE = "default_subtitle_delivery_mode"
        private const val KEY_DEFAULT_SUBTITLE_FORMAT = "default_subtitle_format"
        private const val KEY_INCLUDE_AUTO_CAPTIONS = "include_auto_captions"
        private const val KEY_PREFERRED_SUBTITLE_LANGS = "preferred_subtitle_langs"

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

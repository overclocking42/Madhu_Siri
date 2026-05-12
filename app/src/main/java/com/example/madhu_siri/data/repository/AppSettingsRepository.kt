package com.example.madhu_siri.data.repository

import android.content.Context
import com.example.madhu_siri.data.model.AppThemePreference
import com.example.madhu_siri.ui.localization.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppSettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("madhu_siri_settings", Context.MODE_PRIVATE)

    private val _themePreference = MutableStateFlow(
        AppThemePreference.fromValue(prefs.getString(KEY_THEME, AppThemePreference.SYSTEM.name))
    )
    val themePreference: StateFlow<AppThemePreference> = _themePreference

    private val _language = MutableStateFlow(
        AppLanguage.fromTag(prefs.getString(KEY_LANGUAGE, AppLanguage.ENGLISH.tag))
    )
    val language: StateFlow<AppLanguage> = _language

    fun setThemePreference(preference: AppThemePreference) {
        prefs.edit().putString(KEY_THEME, preference.name).apply()
        _themePreference.value = preference
    }

    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.tag).apply()
        _language.value = language
    }

    fun syncFromProfile(languageTag: String?, themePreference: String?) {
        val parsedLanguage = AppLanguage.fromTag(languageTag)
        val parsedTheme = AppThemePreference.fromValue(themePreference)
        if (_language.value != parsedLanguage) {
            setLanguage(parsedLanguage)
        }
        if (_themePreference.value != parsedTheme) {
            setThemePreference(parsedTheme)
        }
    }

    companion object {
        private const val KEY_THEME = "theme_preference"
        private const val KEY_LANGUAGE = "language_tag"
    }
}

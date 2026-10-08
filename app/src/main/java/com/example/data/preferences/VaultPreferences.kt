package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VaultPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("vault_settings_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false))
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _appLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_APP_LOCK_ENABLED, false))
    val appLockEnabled: StateFlow<Boolean> = _appLockEnabled.asStateFlow()

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
        _onboardingCompleted.value = completed
    }

    fun setAppLock(enabled: Boolean, pin: String? = null) {
        val editor = prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled)
        if (pin != null) {
            editor.putString(KEY_VAULT_PIN, pin)
        } else if (!enabled) {
            editor.remove(KEY_VAULT_PIN)
        }
        editor.apply()
        _appLockEnabled.value = enabled
    }

    fun verifyPin(inputPin: String): Boolean {
        val savedPin = prefs.getString(KEY_VAULT_PIN, "") ?: ""
        return savedPin.isNotEmpty() && savedPin == inputPin
    }

    fun hasPinSet(): Boolean {
        return (prefs.getString(KEY_VAULT_PIN, "") ?: "").isNotEmpty()
    }

    companion object {
        private const val KEY_THEME_MODE = "pref_theme_mode"
        private const val KEY_ONBOARDING_COMPLETED = "pref_onboarding_completed"
        private const val KEY_APP_LOCK_ENABLED = "pref_app_lock_enabled"
        private const val KEY_VAULT_PIN = "pref_vault_pin"
    }
}

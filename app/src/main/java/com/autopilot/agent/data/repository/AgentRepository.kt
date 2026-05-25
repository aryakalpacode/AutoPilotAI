package com.autopilot.agent.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.autopilot.agent.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing agent preferences and settings.
 */
@Singleton
class AgentRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        val THEME_KEY = stringPreferencesKey(Constants.PREF_THEME)
        val FONT_SIZE_KEY = stringPreferencesKey(Constants.PREF_FONT_SIZE)
        val DEFAULT_MODEL_KEY = stringPreferencesKey(Constants.PREF_DEFAULT_MODEL)
        val MAX_ITERATIONS_KEY = intPreferencesKey(Constants.PREF_MAX_ITERATIONS)
        val TEMPERATURE_KEY = floatPreferencesKey(Constants.PREF_TEMPERATURE)
        val PERSONALITY_KEY = stringPreferencesKey(Constants.PREF_PERSONALITY)
        val CUSTOM_PROMPT_KEY = stringPreferencesKey(Constants.PREF_CUSTOM_PROMPT)
        val AUTO_CONFIRM_KEY = booleanPreferencesKey(Constants.PREF_AUTO_CONFIRM)
        val SETUP_COMPLETE_KEY = booleanPreferencesKey(Constants.PREF_SETUP_COMPLETE)
    }

    val theme: Flow<String> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[THEME_KEY] ?: "system" }

    val fontSize: Flow<String> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[FONT_SIZE_KEY] ?: "medium" }

    val defaultModel: Flow<String> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[DEFAULT_MODEL_KEY] ?: "" }

    val maxIterations: Flow<Int> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[MAX_ITERATIONS_KEY] ?: Constants.DEFAULT_MAX_ITERATIONS }

    val temperature: Flow<Float> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[TEMPERATURE_KEY] ?: Constants.DEFAULT_TEMPERATURE.toFloat() }

    val personality: Flow<String> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[PERSONALITY_KEY] ?: "professional" }

    val customPrompt: Flow<String> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[CUSTOM_PROMPT_KEY] ?: "" }

    val autoConfirm: Flow<Boolean> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[AUTO_CONFIRM_KEY] ?: false }

    val isSetupComplete: Flow<Boolean> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[SETUP_COMPLETE_KEY] ?: false }

    suspend fun setTheme(value: String) {
        dataStore.edit { it[THEME_KEY] = value }
    }

    suspend fun setFontSize(value: String) {
        dataStore.edit { it[FONT_SIZE_KEY] = value }
    }

    suspend fun setDefaultModel(value: String) {
        dataStore.edit { it[DEFAULT_MODEL_KEY] = value }
    }

    suspend fun setMaxIterations(value: Int) {
        dataStore.edit { it[MAX_ITERATIONS_KEY] = value }
    }

    suspend fun setTemperature(value: Float) {
        dataStore.edit { it[TEMPERATURE_KEY] = value }
    }

    suspend fun setPersonality(value: String) {
        dataStore.edit { it[PERSONALITY_KEY] = value }
    }

    suspend fun setCustomPrompt(value: String) {
        dataStore.edit { it[CUSTOM_PROMPT_KEY] = value }
    }

    suspend fun setAutoConfirm(value: Boolean) {
        dataStore.edit { it[AUTO_CONFIRM_KEY] = value }
    }

    suspend fun setSetupComplete(value: Boolean) {
        dataStore.edit { it[SETUP_COMPLETE_KEY] = value }
    }
}

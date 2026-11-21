package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.saviiDataStore: DataStore<Preferences> by preferencesDataStore(name = "savii_preferences")

data class PersistedFocusTimerState(
    val mode: FocusMode = FocusMode.STUDY,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isBreak: Boolean = false,
    val targetDurationSec: Int = FocusMode.STUDY.defaultMinutes * 60,
    val startElapsedRealtimeMs: Long = 0L,
    val startEpochMs: Long = 0L,
    val accumulatedActiveMs: Long = 0L,
    val linkedSubjectId: Long? = null,
    val linkedTaskId: Long? = null,
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val TIMER_MODE = stringPreferencesKey("timer_mode")
        val TIMER_RUNNING = booleanPreferencesKey("timer_running")
        val TIMER_PAUSED = booleanPreferencesKey("timer_paused")
        val TIMER_IS_BREAK = booleanPreferencesKey("timer_is_break")
        val TIMER_TARGET_SEC = intPreferencesKey("timer_target_sec")
        val TIMER_START_ELAPSED_MS = longPreferencesKey("timer_start_elapsed_ms")
        val TIMER_START_EPOCH_MS = longPreferencesKey("timer_start_epoch_ms")
        val TIMER_ACCUMULATED_MS = longPreferencesKey("timer_accumulated_ms")
        val TIMER_SUBJECT_ID = longPreferencesKey("timer_subject_id")
        val TIMER_TASK_ID = longPreferencesKey("timer_task_id")
    }

    val themeMode: Flow<ThemeMode> = context.saviiDataStore.data.map { prefs ->
        val raw = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.SYSTEM)
    }

    val focusTimerState: Flow<PersistedFocusTimerState> = context.saviiDataStore.data.map { prefs ->
        val modeName = prefs[Keys.TIMER_MODE] ?: FocusMode.STUDY.name
        val mode = runCatching { FocusMode.valueOf(modeName) }.getOrDefault(FocusMode.STUDY)
        val subjectId = prefs[Keys.TIMER_SUBJECT_ID]?.takeIf { it > 0L }
        val taskId = prefs[Keys.TIMER_TASK_ID]?.takeIf { it > 0L }
        PersistedFocusTimerState(
            mode = mode,
            isRunning = prefs[Keys.TIMER_RUNNING] ?: false,
            isPaused = prefs[Keys.TIMER_PAUSED] ?: false,
            isBreak = prefs[Keys.TIMER_IS_BREAK] ?: false,
            targetDurationSec = prefs[Keys.TIMER_TARGET_SEC] ?: (mode.defaultMinutes * 60),
            startElapsedRealtimeMs = prefs[Keys.TIMER_START_ELAPSED_MS] ?: 0L,
            startEpochMs = prefs[Keys.TIMER_START_EPOCH_MS] ?: 0L,
            accumulatedActiveMs = prefs[Keys.TIMER_ACCUMULATED_MS] ?: 0L,
            linkedSubjectId = subjectId,
            linkedTaskId = taskId,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.saviiDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun saveFocusTimerState(state: PersistedFocusTimerState) {
        context.saviiDataStore.edit { prefs ->
            prefs[Keys.TIMER_MODE] = state.mode.name
            prefs[Keys.TIMER_RUNNING] = state.isRunning
            prefs[Keys.TIMER_PAUSED] = state.isPaused
            prefs[Keys.TIMER_IS_BREAK] = state.isBreak
            prefs[Keys.TIMER_TARGET_SEC] = state.targetDurationSec
            prefs[Keys.TIMER_START_ELAPSED_MS] = state.startElapsedRealtimeMs
            prefs[Keys.TIMER_START_EPOCH_MS] = state.startEpochMs
            prefs[Keys.TIMER_ACCUMULATED_MS] = state.accumulatedActiveMs
            prefs[Keys.TIMER_SUBJECT_ID] = state.linkedSubjectId ?: -1L
            prefs[Keys.TIMER_TASK_ID] = state.linkedTaskId ?: -1L
        }
    }
}

package com.example.focus

import com.example.data.PersistedFocusTimerState
import kotlin.math.max

data class FocusTimerSnapshot(
    val elapsedSeconds: Int,
    val remainingSeconds: Int,
    val progressFraction: Float,
    val isFinished: Boolean,
)

/**
 * Pure monotonic clock computation for the Focus Timer.
 * Uses SystemClock.elapsedRealtime() relative to a persisted start timestamp so
 * backgrounding, screen-off, and process death never drift or lose elapsed time.
 * Falls back to wall-clock epoch if a device reboot reset elapsedRealtime().
 */
object FocusTimerEngine {

    fun computeElapsedActiveMs(
        state: PersistedFocusTimerState,
        nowElapsedRealtimeMs: Long,
        nowEpochMs: Long,
    ): Long {
        if (!state.isRunning || state.isPaused) {
            return state.accumulatedActiveMs.coerceAtLeast(0L)
        }
        val deltaMs = if (state.startElapsedRealtimeMs > 0L && nowElapsedRealtimeMs >= state.startElapsedRealtimeMs) {
            nowElapsedRealtimeMs - state.startElapsedRealtimeMs
        } else if (state.startEpochMs > 0L && nowEpochMs >= state.startEpochMs) {
            nowEpochMs - state.startEpochMs
        } else {
            0L
        }
        return (state.accumulatedActiveMs + deltaMs).coerceAtLeast(0L)
    }

    fun snapshot(
        state: PersistedFocusTimerState,
        nowElapsedRealtimeMs: Long,
        nowEpochMs: Long,
    ): FocusTimerSnapshot {
        val targetSec = state.targetDurationSec.coerceAtLeast(1)
        val elapsedMs = computeElapsedActiveMs(state, nowElapsedRealtimeMs, nowEpochMs)
        val elapsedSec = (elapsedMs / 1000L).toInt().coerceIn(0, targetSec)
        val remainingSec = max(0, targetSec - elapsedSec)
        val progress = (elapsedSec.toFloat() / targetSec.toFloat()).coerceIn(0f, 1f)
        val finished = state.isRunning && !state.isPaused && remainingSec == 0
        return FocusTimerSnapshot(
            elapsedSeconds = elapsedSec,
            remainingSeconds = remainingSec,
            progressFraction = progress,
            isFinished = finished,
        )
    }

    fun formatMmSs(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val mins = safe / 60
        val secs = safe % 60
        return "%02d:%02d".format(mins, secs)
    }
}

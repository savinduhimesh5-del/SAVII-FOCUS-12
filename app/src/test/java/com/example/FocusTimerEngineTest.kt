package com.example

import com.example.data.FocusMode
import com.example.data.PersistedFocusTimerState
import com.example.focus.FocusTimerEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusTimerEngineTest {

    @Test
    fun `running timer tracks elapsedRealtime accurately across background`() {
        val state = PersistedFocusTimerState(
            mode = FocusMode.STUDY,
            isRunning = true,
            isPaused = false,
            targetDurationSec = 45 * 60,
            startElapsedRealtimeMs = 100_000L,
            startEpochMs = 1_700_000_000_000L,
            accumulatedActiveMs = 0L,
        )
        val snap = FocusTimerEngine.snapshot(
            state = state,
            nowElapsedRealtimeMs = 100_000L + 600_000L, // +10 min
            nowEpochMs = 1_700_000_600_000L,
        )
        assertEquals(600, snap.elapsedSeconds)
        assertEquals(35 * 60, snap.remainingSeconds)
        assertFalse(snap.isFinished)
    }

    @Test
    fun `paused timer freezes accumulated time`() {
        val state = PersistedFocusTimerState(
            mode = FocusMode.NORMAL,
            isRunning = true,
            isPaused = true,
            targetDurationSec = 25 * 60,
            startElapsedRealtimeMs = 0L,
            accumulatedActiveMs = 300_000L, // 5 min
        )
        val snap = FocusTimerEngine.snapshot(
            state = state,
            nowElapsedRealtimeMs = 9_999_999L,
            nowEpochMs = 1_700_009_999_000L,
        )
        assertEquals(300, snap.elapsedSeconds)
        assertEquals(20 * 60, snap.remainingSeconds)
        assertFalse(snap.isFinished)
    }

    @Test
    fun `reboot fallback uses epoch delta when elapsedRealtime resets`() {
        val state = PersistedFocusTimerState(
            mode = FocusMode.NORMAL,
            isRunning = true,
            isPaused = false,
            targetDurationSec = 25 * 60,
            startElapsedRealtimeMs = 500_000L,
            startEpochMs = 1_700_000_000_000L,
            accumulatedActiveMs = 60_000L,
        )
        // Simulated reboot: nowElapsedRealtimeMs < startElapsedRealtimeMs
        val snap = FocusTimerEngine.snapshot(
            state = state,
            nowElapsedRealtimeMs = 20_000L,
            nowEpochMs = 1_700_000_240_000L, // +4 min since start
        )
        assertEquals(300, snap.elapsedSeconds) // 1m accumulated + 4m wall-clock
        assertEquals(20 * 60, snap.remainingSeconds)
    }

    @Test
    fun `timer marks finished when remaining reaches zero`() {
        val state = PersistedFocusTimerState(
            mode = FocusMode.NORMAL,
            isRunning = true,
            isPaused = false,
            targetDurationSec = 60,
            startElapsedRealtimeMs = 1_000L,
            startEpochMs = 1_700_000_000_000L,
        )
        val snap = FocusTimerEngine.snapshot(
            state = state,
            nowElapsedRealtimeMs = 65_000L,
            nowEpochMs = 1_700_000_064_000L,
        )
        assertEquals(0, snap.remainingSeconds)
        assertTrue(snap.isFinished)
    }
}

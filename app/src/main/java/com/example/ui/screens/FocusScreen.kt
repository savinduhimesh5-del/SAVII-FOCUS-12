package com.example.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.FocusMode
import com.example.data.FocusSessionEntity
import com.example.focus.FocusTimerEngine
import com.example.ui.SaviiUiState
import com.example.ui.components.SaviiEmptyState
import com.example.ui.components.SaviiGlassCard
import com.example.ui.components.SaviiModuleHeader
import com.example.ui.components.SaviiStatusPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalSaviiColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FocusScreen(
    uiState: SaviiUiState,
    onSelectConfig: (FocusMode, Int, Long?, Long?) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onFinishSession: (Boolean) -> Unit,
    onStartBreak: (Int) -> Unit,
    onReconcileTimer: () -> Unit,
    onDeleteSession: (FocusSessionEntity) -> Unit,
) {
    var nowElapsedMs by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var nowEpochMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(uiState.timerState.isRunning, uiState.timerState.isPaused) {
        nowElapsedMs = SystemClock.elapsedRealtime()
        nowEpochMs = System.currentTimeMillis()
        while (uiState.timerState.isRunning && !uiState.timerState.isPaused) {
            onReconcileTimer()
            delay(500L)
            nowElapsedMs = SystemClock.elapsedRealtime()
            nowEpochMs = System.currentTimeMillis()
        }
    }

    val snapshot = remember(uiState.timerState, nowElapsedMs, nowEpochMs) {
        FocusTimerEngine.snapshot(uiState.timerState, nowElapsedMs, nowEpochMs)
    }
    val timerState = uiState.timerState
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val saviiColors = LocalSaviiColors.current
    val dateFormat = remember { SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("focus_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "focus_header") {
            SaviiModuleHeader(
                title = "Focus Timer",
                subtitle = "Monotonic clock • Survives background & process death",
            )
        }

        // Mode Selection
        item(key = "mode_selector") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FocusMode.entries) { mode ->
                    FilterChip(
                        selected = timerState.mode == mode,
                        onClick = {
                            if (!timerState.isRunning) {
                                onSelectConfig(mode, mode.defaultMinutes, timerState.linkedSubjectId, timerState.linkedTaskId)
                            }
                        },
                        enabled = !timerState.isRunning,
                        label = { Text("${mode.label} (${mode.defaultMinutes}m)") },
                    )
                }
            }
        }

        // Subject Link Pills
        item(key = "subject_link") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = timerState.linkedSubjectId == null,
                        onClick = {
                            if (!timerState.isRunning) {
                                onSelectConfig(timerState.mode, timerState.targetDurationSec / 60, null, null)
                            }
                        },
                        enabled = !timerState.isRunning,
                        label = { Text("General") },
                    )
                }
                items(uiState.subjects, key = { it.id }) { sub ->
                    FilterChip(
                        selected = timerState.linkedSubjectId == sub.id,
                        onClick = {
                            if (!timerState.isRunning) {
                                onSelectConfig(timerState.mode, timerState.targetDurationSec / 60, sub.id, null)
                            }
                        },
                        enabled = !timerState.isRunning,
                        label = { Text(sub.name.substringBefore(" (")) },
                    )
                }
            }
        }

        // Main Timer Ring Card
        item(key = "timer_ring") {
            SaviiGlassCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SaviiStatusPill(
                        label = when {
                            timerState.isBreak -> "Break Time"
                            timerState.isRunning && timerState.isPaused -> "Paused"
                            timerState.isRunning -> "${timerState.mode.label} Active"
                            else -> "${timerState.mode.label} Ready"
                        },
                        color = if (timerState.isBreak) saviiColors.statusCompleted else primaryColor,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier.size(210.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 14.dp.toPx()
                            drawArc(
                                color = trackColor,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            )
                            val sweep = (1f - snapshot.progressFraction) * 360f
                            drawArc(
                                brush = Brush.sweepGradient(listOf(primaryColor, secondaryColor, primaryColor)),
                                startAngle = -90f,
                                sweepAngle = sweep.coerceAtLeast(2f),
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = FocusTimerEngine.formatMmSs(snapshot.remainingSeconds),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("focus_timer_display"),
                            )
                            Text(
                                text = "${timerState.targetDurationSec / 60} min target",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Duration quick presets when idle
                    if (!timerState.isRunning) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(15, 25, 45, 60, 90).forEach { mins ->
                                FilterChip(
                                    selected = timerState.targetDurationSec == mins * 60,
                                    onClick = {
                                        onSelectConfig(timerState.mode, mins, timerState.linkedSubjectId, timerState.linkedTaskId)
                                    },
                                    label = { Text("${mins}m") },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Primary Timer Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    ) {
                        if (!timerState.isRunning) {
                            Button(
                                onClick = onStart,
                                modifier = Modifier.testTag("focus_start_btn"),
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start Focus")
                            }
                            FilledTonalButton(
                                onClick = { onStartBreak(timerState.mode.defaultBreakMinutes.coerceAtLeast(5)) },
                                modifier = Modifier.testTag("focus_break_btn"),
                            ) {
                                Icon(Icons.Filled.Coffee, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Break 5m")
                            }
                        } else {
                            if (timerState.isPaused) {
                                Button(
                                    onClick = onResume,
                                    modifier = Modifier.testTag("focus_resume_btn"),
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Resume")
                                }
                            } else {
                                FilledTonalButton(
                                    onClick = onPause,
                                    modifier = Modifier.testTag("focus_pause_btn"),
                                ) {
                                    Icon(Icons.Filled.Pause, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pause")
                                }
                            }
                            OutlinedButton(
                                onClick = { onFinishSession(false) },
                                modifier = Modifier.testTag("focus_save_stop_btn"),
                            ) {
                                Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Finish")
                            }
                            IconButton(
                                onClick = onReset,
                                modifier = Modifier.testTag("focus_reset_btn"),
                            ) {
                                Icon(Icons.Filled.RestartAlt, contentDescription = "Reset timer")
                            }
                        }
                    }
                }
            }
        }

        item(key = "history_title") {
            Text(
                text = "Focus History (${uiState.focusSessions.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        if (uiState.focusSessions.isEmpty()) {
            item(key = "history_empty") {
                SaviiEmptyState(
                    icon = Icons.Filled.Timer,
                    title = "No sessions recorded yet",
                    message = "Start and finish a session to log real focus duration into your local Insights.",
                )
            }
        } else {
            items(uiState.focusSessions, key = { it.id }) { session ->
                SaviiGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${session.mode.label}${if (session.isBreak) " (Break)" else ""}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${session.actualSeconds / 60}m ${session.actualSeconds % 60}s • ${dateFormat.format(Date(session.startedAtEpochMs))}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        SaviiStatusPill(
                            label = if (session.wasCompleted) "Done" else "Logged",
                            color = if (session.wasCompleted) saviiColors.statusCompleted else saviiColors.statusInProgress,
                        )
                        IconButton(onClick = { onDeleteSession(session) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete session")
                        }
                    }
                }
            }
        }
    }
}

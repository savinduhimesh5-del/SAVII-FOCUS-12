package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.data.TaskStatus
import com.example.ui.SaviiUiState
import com.example.ui.components.SaviiEmptyState
import com.example.ui.components.SaviiGlassCard
import com.example.ui.components.SaviiModuleHeader
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalSaviiColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class DayFocusBar(val label: String, val minutes: Int)

@Composable
fun InsightsScreen(
    uiState: SaviiUiState,
    onBack: () -> Unit,
) {
    val studySessions = remember(uiState.focusSessions) {
        uiState.focusSessions.filter { !it.isBreak }
    }
    val totalMinutes = remember(studySessions) {
        studySessions.sumOf { it.actualSeconds } / 60
    }
    val completedTasks = remember(uiState.tasks) {
        uiState.tasks.count { it.status == TaskStatus.COMPLETED }
    }
    val totalTasks = uiState.tasks.size

    val last7Days = remember(studySessions) {
        val dayFmt = SimpleDateFormat("EEE", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        (6 downTo 0).map { daysAgo ->
            val dayStart = cal.timeInMillis - daysAgo * 86_400_000L
            val dayEnd = dayStart + 86_400_000L
            val mins = studySessions
                .filter { it.startedAtEpochMs in dayStart until dayEnd }
                .sumOf { it.actualSeconds } / 60
            DayFocusBar(dayFmt.format(dayStart), mins)
        }
    }

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val saviiColors = LocalSaviiColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "insights_header") {
            SaviiModuleHeader(
                title = "Insights",
                subtitle = "100% real stored telemetry • Zero fake scores",
                onBack = onBack,
            )
        }

        if (studySessions.isEmpty() && uiState.tasks.isEmpty() && uiState.syllabusNodes.isEmpty()) {
            item(key = "insights_empty") {
                SaviiEmptyState(
                    icon = Icons.Filled.BarChart,
                    title = "Not enough data yet",
                    message = "Complete Focus sessions, Planner tasks, or Syllabus topics to see real weekly charts and distribution breakdowns.",
                )
            }
        } else {
            // Real Summary Metrics
            item(key = "summary_metrics") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SaviiGlassCard(modifier = Modifier.weight(1f)) {
                        Text("Focus Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${totalMinutes}m",
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = primary,
                        )
                        Text("${studySessions.size} sessions", style = MaterialTheme.typography.bodyMedium)
                    }
                    SaviiGlassCard(modifier = Modifier.weight(1f)) {
                        Text("Tasks Done", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (totalTasks == 0) "0/0" else "$completedTasks/$totalTasks",
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = saviiColors.statusCompleted,
                        )
                        Text("${uiState.syllabusNodes.count { it.isCompleted }} topics done", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Weekly Focus Bar Chart (Compose Canvas)
            item(key = "weekly_chart") {
                SaviiGlassCard {
                    Text(
                        text = "Weekly Focus Minutes",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    val maxMins = (last7Days.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(30)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                    ) {
                        val barCount = last7Days.size.coerceAtLeast(1)
                        val slotWidth = size.width / barCount
                        val barWidth = (slotWidth * 0.42f).coerceAtMost(28.dp.toPx())
                        last7Days.forEachIndexed { idx, bar ->
                            val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                            drawRoundRect(
                                color = track,
                                topLeft = Offset(x, 0f),
                                size = Size(barWidth, size.height),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                            )
                            val fillHeight = (bar.minutes.toFloat() / maxMins.toFloat()) * size.height
                            if (fillHeight > 0f) {
                                drawRoundRect(
                                    brush = Brush.verticalGradient(listOf(secondary, primary)),
                                    topLeft = Offset(x, size.height - fillHeight),
                                    size = Size(barWidth, fillHeight),
                                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        last7Days.forEach { day ->
                            Text(
                                text = day.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Task Status Distribution Ring (Real Data)
            if (totalTasks > 0) {
                item(key = "task_distribution") {
                    val slices = listOf(
                        Triple("Completed", uiState.tasks.count { it.status == TaskStatus.COMPLETED }, saviiColors.statusCompleted),
                        Triple("In Progress", uiState.tasks.count { it.status == TaskStatus.IN_PROGRESS }, saviiColors.statusInProgress),
                        Triple("Pending", uiState.tasks.count { it.status == TaskStatus.PENDING }, saviiColors.statusPending),
                        Triple("Important", uiState.tasks.count { it.status == TaskStatus.IMPORTANT }, saviiColors.statusImportant),
                        Triple("Incomplete/Overdue", uiState.tasks.count { it.status == TaskStatus.INCOMPLETE || it.status == TaskStatus.OVERDUE }, saviiColors.statusIncomplete),
                    ).filter { it.second > 0 }

                    SaviiGlassCard {
                        Text(
                            text = "Task Status Distribution",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Canvas(modifier = Modifier.size(110.dp)) {
                                val stroke = 16.dp.toPx()
                                var start = -90f
                                slices.forEach { (_, count, color) ->
                                    val sweep = (count.toFloat() / totalTasks.toFloat()) * 360f
                                    drawArc(
                                        color = color,
                                        startAngle = start,
                                        sweepAngle = (sweep - 4f).coerceAtLeast(2f),
                                        useCenter = false,
                                        style = Stroke(width = stroke, cap = StrokeCap.Butt),
                                    )
                                    start += sweep
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                slices.forEach { (label, count, color) ->
                                    val pct = (count * 100) / totalTasks
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Canvas(modifier = Modifier.size(8.dp)) { drawCircle(color = color) }
                                        Text(
                                            text = "$label: $count ($pct%)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

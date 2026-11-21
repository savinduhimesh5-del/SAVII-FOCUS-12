package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.ExamCategory
import com.example.data.ExamEntity
import com.example.data.ExamReadinessCalculator
import com.example.data.SubjectEntity
import com.example.ui.SaviiUiState
import com.example.ui.components.SaviiEmptyState
import com.example.ui.components.SaviiGlassCard
import com.example.ui.components.SaviiModuleHeader
import com.example.ui.components.SaviiStatusPill
import com.example.ui.theme.LocalSaviiColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun ExamsScreen(
    uiState: SaviiUiState,
    onAddExam: (title: String, category: ExamCategory, subjectId: Long?, dateEpochMs: Long, prepDays: Int, weakNote: String) -> Unit,
    onUpdateRevision: (ExamEntity, Int) -> Unit,
    onDeleteExam: (ExamEntity) -> Unit,
    onBack: () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val nowMs = remember { System.currentTimeMillis() }
    val summaries = remember(uiState.exams, uiState.subjects, uiState.syllabusNodes, nowMs) {
        uiState.exams.map { ExamReadinessCalculator.evaluate(it, uiState.subjects, uiState.syllabusNodes, nowMs) }
    }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val saviiColors = LocalSaviiColors.current

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("exams_add_fab"),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add exam")
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "exams_header") {
                SaviiModuleHeader(
                    title = "Exams & Readiness",
                    subtitle = "A/L, AAT, Term Tests & Class Exams",
                    onBack = onBack,
                )
            }

            if (summaries.isEmpty()) {
                item(key = "exams_empty") {
                    SaviiEmptyState(
                        icon = Icons.Filled.School,
                        title = "No exams tracked yet",
                        message = "Add your A/L, AAT, Term Test, or Class Exam dates. Readiness combines real syllabus completion, weak topics, and revision progress.",
                        actionLabel = "Add Exam",
                        actionTag = "exams_empty_add_btn",
                        onAction = { showAddDialog = true },
                    )
                }
            } else {
                items(summaries, key = { it.exam.id }) { summary ->
                    val exam = summary.exam
                    val badgeColor = when {
                        summary.daysRemaining < 0 -> saviiColors.statusIncomplete
                        summary.isUrgent -> saviiColors.statusOverdue
                        summary.isInPrepWindow -> saviiColors.statusImportant
                        else -> saviiColors.statusPending
                    }
                    SaviiGlassCard(accentBorderColor = if (summary.isUrgent) badgeColor.copy(alpha = 0.7f) else null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${exam.category.label} • ${dateFormatter.format(Date(exam.examDateEpochMs))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                                Text(
                                    text = exam.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                summary.subjectName?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            SaviiStatusPill(
                                label = if (summary.daysRemaining >= 0) "${summary.daysRemaining}d left" else "Past",
                                color = badgeColor,
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = buildString {
                                append("Prep window: ${exam.prepWindowDays}d")
                                summary.syllabusCompletionPercent?.let { append(" • Syllabus: $it%") }
                                summary.readinessPercent?.let { append(" • Readiness: $it%") }
                                    ?: append(" • Add syllabus nodes or update revision below")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (exam.weakTopicsNote.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Weak focus: ${exam.weakTopicsNote}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = saviiColors.statusInProgress,
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Revision: ${exam.revisionProgressPercent}%",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { onDeleteExam(exam) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete exam")
                            }
                        }
                        Slider(
                            value = exam.revisionProgressPercent.toFloat(),
                            onValueChange = { onUpdateRevision(exam, it.toInt()) },
                            valueRange = 0f..100f,
                        )
                    }
                }
            }
            item(key = "exams_bottom_pad") { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showAddDialog) {
        AddExamDialog(
            subjects = uiState.subjects,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, category, subjectId, daysOut, prepDays, weakNote ->
                val targetMs = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(daysOut.toLong())
                onAddExam(title, category, subjectId, targetMs, prepDays, weakNote)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun AddExamDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, ExamCategory, Long?, Int, Int, String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExamCategory.AAT) }
    var selectedSubjectId by remember { mutableStateOf<Long?>(subjects.firstOrNull()?.id) }
    var daysOut by remember { mutableIntStateOf(30) }
    var prepDays by remember { mutableIntStateOf(ExamCategory.AAT.defaultPrepDays) }
    var weakNote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Exam") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Exam title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Category", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ExamCategory.entries) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = {
                                category = cat
                                prepDays = cat.defaultPrepDays
                            },
                            label = { Text(cat.label) },
                        )
                    }
                }
                Text("Days from today: $daysOut d (Prep window: $prepDays d)", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(7, 14, 30, 60, 120).forEach { d ->
                        FilterChip(
                            selected = daysOut == d,
                            onClick = { daysOut = d },
                            label = { Text("${d}d") },
                        )
                    }
                }
                Text("Linked Subject", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(subjects, key = { it.id }) { sub ->
                        FilterChip(
                            selected = selectedSubjectId == sub.id,
                            onClick = { selectedSubjectId = sub.id },
                            label = { Text(sub.name.substringBefore(" (")) },
                        )
                    }
                }
                OutlinedTextField(
                    value = weakNote,
                    onValueChange = { weakNote = it },
                    label = { Text("Weak topics (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, category, selectedSubjectId, daysOut, prepDays, weakNote) },
                enabled = title.trim().isNotEmpty(),
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.CurriculumType
import com.example.data.DifficultyLevel
import com.example.data.SubjectStatus
import com.example.data.SyllabusNodeEntity
import com.example.data.SyllabusNodeType
import com.example.data.UnderstandingLevel
import com.example.ui.SaviiUiState
import com.example.ui.components.SaviiEmptyState
import com.example.ui.components.SaviiGlassCard
import com.example.ui.components.SaviiModuleHeader
import com.example.ui.components.SaviiStatusPill
import com.example.ui.theme.LocalSaviiColors

@Composable
fun LearnScreen(
    uiState: SaviiUiState,
    onAddNode: (subjectId: Long, parentId: Long?, type: SyllabusNodeType, title: String, difficulty: DifficultyLevel) -> Unit,
    onToggleComplete: (SyllabusNodeEntity) -> Unit,
    onCycleUnderstanding: (SyllabusNodeEntity) -> Unit,
    onToggleWeak: (SyllabusNodeEntity) -> Unit,
    onIncrementRevision: (SyllabusNodeEntity) -> Unit,
    onDeleteNode: (SyllabusNodeEntity) -> Unit,
    onUpdateSubjectStatus: (com.example.data.SubjectEntity, SubjectStatus) -> Unit,
    onOpenExams: () -> Unit,
) {
    var selectedCurriculum by remember { mutableStateOf<CurriculumType?>(null) }
    var selectedSubjectId by remember { mutableLongStateOf(-1L) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredSubjects = remember(uiState.subjects, selectedCurriculum) {
        if (selectedCurriculum == null) uiState.subjects
        else uiState.subjects.filter { it.curriculum == selectedCurriculum }
    }

    val activeSubject = remember(filteredSubjects, selectedSubjectId) {
        filteredSubjects.firstOrNull { it.id == selectedSubjectId } ?: filteredSubjects.firstOrNull()
    }

    val subjectNodes = remember(uiState.syllabusNodes, activeSubject) {
        if (activeSubject == null) emptyList()
        else uiState.syllabusNodes.filter { it.subjectId == activeSubject.id }
    }

    val completedCount = remember(subjectNodes) { subjectNodes.count { it.isCompleted } }
    val weakCount = remember(subjectNodes) { subjectNodes.count { it.isWeakTopic } }
    val saviiColors = LocalSaviiColors.current

    Scaffold(
        floatingActionButton = {
            if (activeSubject != null) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("learn_add_node_fab"),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add unit or lesson")
                }
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
            item(key = "learn_header") {
                SaviiModuleHeader(
                    title = "Learn",
                    subtitle = "A/L (Sinhala Medium) & AAT Syllabus Tracker",
                    trailingAction = {
                        OutlinedButton(onClick = onOpenExams) {
                            Icon(Icons.Filled.School, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exams")
                        }
                    },
                )
            }

            item(key = "curriculum_filter") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedCurriculum == null,
                        onClick = { selectedCurriculum = null },
                        label = { Text("All") },
                    )
                    CurriculumType.entries.forEach { curriculum ->
                        FilterChip(
                            selected = selectedCurriculum == curriculum,
                            onClick = { selectedCurriculum = curriculum },
                            label = { Text(curriculum.label) },
                        )
                    }
                }
            }

            item(key = "subject_selector") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredSubjects, key = { it.id }) { subject ->
                        FilterChip(
                            selected = activeSubject?.id == subject.id,
                            onClick = { selectedSubjectId = subject.id },
                            label = { Text(subject.name) },
                        )
                    }
                }
            }

            if (activeSubject != null) {
                item(key = "active_subject_card") {
                    val progress = if (subjectNodes.isEmpty()) 0f else completedCount.toFloat() / subjectNodes.size.toFloat()
                    SaviiGlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeSubject.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "${activeSubject.levelOrStream} • ${activeSubject.medium.label}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            val statusColor = when (activeSubject.status) {
                                SubjectStatus.COMPLETED -> saviiColors.statusCompleted
                                SubjectStatus.IN_PROGRESS -> saviiColors.statusInProgress
                                SubjectStatus.UPCOMING -> saviiColors.statusPending
                            }
                            SaviiStatusPill(
                                label = activeSubject.status.label,
                                color = statusColor,
                                modifier = Modifier.clickable {
                                    val next = SubjectStatus.entries[(activeSubject.status.ordinal + 1) % SubjectStatus.entries.size]
                                    onUpdateSubjectStatus(activeSubject, next)
                                },
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (subjectNodes.isEmpty()) {
                                "No units/lessons added yet • Tap + to build your syllabus"
                            } else {
                                "$completedCount/${subjectNodes.size} completed • $weakCount weak topic(s)"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (subjectNodes.isEmpty()) {
                item(key = "learn_empty") {
                    SaviiEmptyState(
                        icon = Icons.Filled.MenuBook,
                        title = "Start building this subject",
                        message = "Add Units, Lessons, Subtopics, or Past Paper drills. Progress, difficulty, weak topics, and revision history stay offline in Room.",
                        actionLabel = "Add First Unit / Lesson",
                        actionTag = "learn_empty_add_btn",
                        onAction = { showAddDialog = true },
                    )
                }
            } else {
                items(subjectNodes, key = { it.id }) { node ->
                    SaviiGlassCard(
                        accentBorderColor = if (node.isWeakTopic) saviiColors.statusIncomplete.copy(alpha = 0.65f) else null,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = node.isCompleted,
                                onCheckedChange = { onToggleComplete(node) },
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${node.nodeType.label} • ${node.difficulty.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                                Text(
                                    text = node.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            IconButton(onClick = { onDeleteNode(node) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete topic")
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val understandingColor = when (node.understanding) {
                                UnderstandingLevel.MASTERED -> saviiColors.statusCompleted
                                UnderstandingLevel.CONFIDENT -> saviiColors.statusPending
                                UnderstandingLevel.NEEDS_WORK -> saviiColors.statusIncomplete
                                UnderstandingLevel.NOT_ASSESSED -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            SaviiStatusPill(
                                label = node.understanding.label,
                                color = understandingColor,
                                modifier = Modifier.clickable { onCycleUnderstanding(node) },
                            )
                            FilterChip(
                                selected = node.isWeakTopic,
                                onClick = { onToggleWeak(node) },
                                label = { Text("Weak") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.WarningAmber,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                            )
                            OutlinedButton(
                                onClick = { onIncrementRevision(node) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rev ${node.revisionCount}")
                            }
                        }
                    }
                }
            }
            item(key = "bottom_pad") { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showAddDialog && activeSubject != null) {
        AddSyllabusNodeDialog(
            subjectName = activeSubject.name,
            onDismiss = { showAddDialog = false },
            onConfirm = { type, title, difficulty ->
                onAddNode(activeSubject.id, null, type, title, difficulty)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun AddSyllabusNodeDialog(
    subjectName: String,
    onDismiss: () -> Unit,
    onConfirm: (SyllabusNodeType, String, DifficultyLevel) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(SyllabusNodeType.UNIT) }
    var selectedDifficulty by remember { mutableStateOf(DifficultyLevel.MEDIUM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to $subjectName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (Unit / Lesson / Past Paper)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("learn_node_title_input"),
                )
                Text("Type", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(SyllabusNodeType.entries) { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.label) },
                        )
                    }
                }
                Text("Difficulty", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DifficultyLevel.entries.forEach { diff ->
                        FilterChip(
                            selected = selectedDifficulty == diff,
                            onClick = { selectedDifficulty = diff },
                            label = { Text(diff.label) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedType, title, selectedDifficulty) },
                enabled = title.trim().isNotEmpty(),
                modifier = Modifier.testTag("learn_node_save_btn"),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

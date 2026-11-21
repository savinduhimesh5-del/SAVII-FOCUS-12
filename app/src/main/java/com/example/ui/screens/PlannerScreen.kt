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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.example.data.RepeatRule
import com.example.data.SubjectEntity
import com.example.data.SubtaskEntity
import com.example.data.TaskEntity
import com.example.data.TaskPriority
import com.example.data.TaskStatus
import com.example.ui.SaviiUiState
import com.example.ui.components.SaviiEmptyState
import com.example.ui.components.SaviiGlassCard
import com.example.ui.components.SaviiModuleHeader
import com.example.ui.components.SaviiStatusPill
import com.example.ui.components.statusColorFor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun PlannerScreen(
    uiState: SaviiUiState,
    onAddTask: (
        title: String,
        category: String,
        priority: TaskPriority,
        status: TaskStatus,
        estimatedMinutes: Int,
        deadlineEpochMs: Long?,
        repeatRule: RepeatRule,
        notes: String,
        subjectId: Long?,
    ) -> Unit,
    onUpdateTaskStatus: (TaskEntity, TaskStatus) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onAddSubtask: (taskId: Long, title: String) -> Unit,
    onToggleSubtask: (SubtaskEntity) -> Unit,
) {
    var statusFilter by remember { mutableStateOf<TaskStatus?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var subtaskTargetTaskId by remember { mutableStateOf<Long?>(null) }

    val filteredTasks = remember(uiState.tasks, statusFilter) {
        if (statusFilter == null) uiState.tasks
        else uiState.tasks.filter { it.status == statusFilter }
    }
    val dateFormatter = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.testTag("planner_add_fab"),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create task")
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
            item(key = "planner_header") {
                SaviiModuleHeader(
                    title = "Planner",
                    subtitle = "${uiState.tasks.size} task(s) • Color-coded statuses",
                )
            }

            item(key = "status_filters") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = statusFilter == null,
                            onClick = { statusFilter = null },
                            label = { Text("All (${uiState.tasks.size})") },
                        )
                    }
                    items(TaskStatus.entries) { status ->
                        val count = uiState.tasks.count { it.status == status }
                        FilterChip(
                            selected = statusFilter == status,
                            onClick = { statusFilter = status },
                            label = { Text("${status.label} ($count)") },
                        )
                    }
                }
            }

            if (filteredTasks.isEmpty()) {
                item(key = "planner_empty") {
                    SaviiEmptyState(
                        icon = Icons.Filled.PlaylistAddCheck,
                        title = "No tasks in this view",
                        message = "Create tasks with deadlines, durations, repeats, and subtasks. AI weekly plans will also require your approval first.",
                        actionLabel = "Add Task",
                        actionTag = "planner_empty_add_btn",
                        onAction = { showCreateDialog = true },
                    )
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
                    val taskSubtasks = remember(uiState.subtasks, task.id) {
                        uiState.subtasks.filter { it.taskId == task.id }
                    }
                    val statusColor = statusColorFor(task.status)
                    SaviiGlassCard(accentBorderColor = statusColor.copy(alpha = 0.5f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = buildString {
                                        append("${task.category} • ${task.priority.label} • ${task.estimatedMinutes}m")
                                        task.deadlineEpochMs?.let { append(" • Due ${dateFormatter.format(Date(it))}") }
                                        if (task.repeatRule != RepeatRule.NONE) append(" • ${task.repeatRule.label}")
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { onDeleteTask(task) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete task")
                            }
                        }

                        if (task.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = task.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (task.progressPercent.coerceIn(0, 100)) / 100f },
                            color = statusColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(TaskStatus.entries) { st ->
                                val stColor = statusColorFor(st)
                                val selected = task.status == st
                                SaviiStatusPill(
                                    label = st.label,
                                    color = if (selected) stColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.clickable { onUpdateTaskStatus(task, st) },
                                )
                            }
                        }

                        if (taskSubtasks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            taskSubtasks.forEach { sub ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Checkbox(
                                        checked = sub.isCompleted,
                                        onCheckedChange = { onToggleSubtask(sub) },
                                    )
                                    Text(
                                        text = sub.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }

                        TextButton(
                            onClick = { subtaskTargetTaskId = task.id },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        ) {
                            Text("+ Add Subtask")
                        }
                    }
                }
            }
            item(key = "planner_bottom_pad") { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showCreateDialog) {
        CreateTaskDialog(
            subjects = uiState.subjects,
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, cat, prio, st, mins, dueDays, repeat, notes, subId ->
                val deadlineMs = dueDays?.let { System.currentTimeMillis() + TimeUnit.DAYS.toMillis(it.toLong()) }
                onAddTask(title, cat, prio, st, mins, deadlineMs, repeat, notes, subId)
                showCreateDialog = false
            },
        )
    }

    subtaskTargetTaskId?.let { targetId ->
        var subTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { subtaskTargetTaskId = null },
            title = { Text("Add Subtask") },
            text = {
                OutlinedTextField(
                    value = subTitle,
                    onValueChange = { subTitle = it },
                    label = { Text("Subtask title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddSubtask(targetId, subTitle)
                        subtaskTargetTaskId = null
                    },
                    enabled = subTitle.trim().isNotEmpty(),
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { subtaskTargetTaskId = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun CreateTaskDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, TaskPriority, TaskStatus, Int, Int?, RepeatRule, String, Long?) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Study") }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var status by remember { mutableStateOf(TaskStatus.PENDING) }
    var minutes by remember { mutableIntStateOf(45) }
    var dueDays by remember { mutableStateOf<Int?>(1) }
    var repeatRule by remember { mutableStateOf(RepeatRule.NONE) }
    var notes by remember { mutableStateOf("") }
    var subjectId by remember { mutableStateOf<Long?>(subjects.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("planner_task_title_input"),
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (Study, Past Paper, Revision)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Status", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf(TaskStatus.PENDING, TaskStatus.IN_PROGRESS, TaskStatus.IMPORTANT)) { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st.label) },
                        )
                    }
                }
                Text("Priority & Duration", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TaskPriority.entries) { pr ->
                        FilterChip(
                            selected = priority == pr,
                            onClick = { priority = pr },
                            label = { Text(pr.label) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(25, 45, 60, 90).forEach { m ->
                        FilterChip(
                            selected = minutes == m,
                            onClick = { minutes = m },
                            label = { Text("${m}m") },
                        )
                    }
                }
                Text("Deadline", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to "Today", 1 to "1d", 3 to "3d", 7 to "7d").forEach { (d, lbl) ->
                        FilterChip(
                            selected = dueDays == d,
                            onClick = { dueDays = d },
                            label = { Text(lbl) },
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(title, category, priority, status, minutes, dueDays, repeatRule, notes, subjectId)
                },
                enabled = title.trim().isNotEmpty(),
                modifier = Modifier.testTag("planner_save_task_btn"),
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

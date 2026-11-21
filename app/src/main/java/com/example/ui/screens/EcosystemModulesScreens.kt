package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.ThemeMode
import com.example.ui.SaviiUiState
import com.example.ui.components.SaviiGlassCard
import com.example.ui.components.SaviiLogoMark
import com.example.ui.components.SaviiModuleHeader

@Composable
fun SettingsScreen(
    uiState: SaviiUiState,
    onSelectTheme: (ThemeMode) -> Unit,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "settings_header") {
            SaviiModuleHeader(
                title = "Settings",
                subtitle = "Appearance & local Room storage",
                onBack = onBack,
            )
        }

        item(key = "theme_card") {
            SaviiGlassCard {
                Text(
                    text = "Theme Mode",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Choose Obsidian Dark, Clean Light, or follow System.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = uiState.themeMode == mode,
                            onClick = { onSelectTheme(mode) },
                            label = { Text(mode.label) },
                            modifier = Modifier.testTag("theme_chip_${mode.name.lowercase()}"),
                        )
                    }
                }
            }
        }

        item(key = "storage_card") {
            SaviiGlassCard {
                Text(
                    text = "Offline Storage Overview (Room + DataStore)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Subjects: ${uiState.subjects.size}\n" +
                        "• Syllabus nodes: ${uiState.syllabusNodes.size}\n" +
                        "• Tasks: ${uiState.tasks.size} (Subtasks: ${uiState.subtasks.size})\n" +
                        "• Exams: ${uiState.exams.size}\n" +
                        "• Focus sessions: ${uiState.focusSessions.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item(key = "about_card") {
            SaviiGlassCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SaviiLogoMark(size = 36.dp)
                    Column {
                        Text("SAVII FOCUS", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Your Personal Intelligence Ecosystem • Offline-First Foundation",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeasibilityInfoScreen(
    title: String,
    subtitle: String,
    architectureNote: String,
    hyperOsNote: String,
    nextPhaseNote: String,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header") {
            SaviiModuleHeader(
                title = title,
                subtitle = subtitle,
                onBack = onBack,
            )
        }
        item(key = "arch") {
            SaviiGlassCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Phase 2 Scope & Architecture", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(architectureNote, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item(key = "hyperos") {
            SaviiGlassCard {
                Text("Redmi Note 14 / HyperOS Reality Check", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Text(hyperOsNote, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item(key = "next_phase") {
            SaviiGlassCard {
                Text("Planned Implementation Order", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Text(nextPhaseNote, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

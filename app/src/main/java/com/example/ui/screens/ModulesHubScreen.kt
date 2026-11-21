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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.SaviiGlassCard
import com.example.ui.components.SaviiModuleHeader
import com.example.ui.components.SaviiStatusPill
import com.example.ui.navigation.SaviiRoute
import com.example.ui.theme.LocalSaviiColors

private data class HubModuleItem(
    val route: String,
    val title: String,
    val subtitle: String,
    val status: String,
    val isActivePhase: Boolean,
    val icon: ImageVector,
)

@Composable
fun ModulesHubScreen(
    onNavigate: (String) -> Unit,
) {
    val saviiColors = LocalSaviiColors.current
    val items = listOf(
        HubModuleItem(SaviiRoute.EXAMS, "Exams & Readiness", "A/L, AAT, Term Test & Class Exams", "Active", true, Icons.Filled.School),
        HubModuleItem(SaviiRoute.CALENDAR, "Calendar", "Linked Planner + Exam conflict check", "Active", true, Icons.Filled.CalendarMonth),
        HubModuleItem(SaviiRoute.INSIGHTS, "Insights", "Real charts from local Room telemetry", "Active", true, Icons.Filled.BarChart),
        HubModuleItem(SaviiRoute.SETTINGS, "Settings", "Dark / Light / System theme & DB info", "Active", true, Icons.Filled.Settings),
        HubModuleItem(SaviiRoute.SAVII_AI, "SAVII AI", "Approval-first AI insights (Next Phase)", "Planned", false, Icons.Filled.AutoAwesome),
        HubModuleItem(SaviiRoute.PROTOCOL, "SAVII Focus Protocol", "Focus Bar & HyperOS feasibility guide", "Planned", false, Icons.Filled.Lock),
        HubModuleItem(SaviiRoute.SHIELD, "SAVII Shield", "Biometric lock & Keystore (Next Phase)", "Planned", false, Icons.Filled.Shield),
        HubModuleItem(SaviiRoute.CONNECT, "SAVII Connect", "Calendar & official integrations (Future)", "Future", false, Icons.Filled.Hub),
        HubModuleItem(SaviiRoute.CLOUD, "SAVII Cloud", "Optional encrypted backup (Future)", "Future", false, Icons.Filled.CloudOff),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "hub_header") {
            SaviiModuleHeader(
                title = "Ecosystem Hub",
                subtitle = "All 12 SAVII FOCUS modules",
            )
        }
        items(items, key = { it.route }) { mod ->
            SaviiGlassCard(
                modifier = Modifier
                    .clickable { onNavigate(mod.route) }
                    .testTag("hub_item_${mod.route}"),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = mod.icon,
                        contentDescription = mod.title,
                        tint = if (mod.isActivePhase) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(26.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mod.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = mod.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    SaviiStatusPill(
                        label = mod.status,
                        color = if (mod.isActivePhase) saviiColors.statusCompleted else saviiColors.statusPending,
                    )
                }
            }
        }
    }
}

package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.SaviiRoute
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.ExamsScreen
import com.example.ui.screens.FeasibilityInfoScreen
import com.example.ui.screens.FocusScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.ModulesHubScreen
import com.example.ui.screens.PlannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.SaviiFocusTheme

private data class TopLevelNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val TopLevelItems = listOf(
    TopLevelNavItem(SaviiRoute.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    TopLevelNavItem(SaviiRoute.LEARN, "Learn", Icons.Filled.MenuBook, Icons.Outlined.MenuBook),
    TopLevelNavItem(SaviiRoute.PLANNER, "Planner", Icons.Filled.Checklist, Icons.Outlined.Checklist),
    TopLevelNavItem(SaviiRoute.FOCUS, "Focus", Icons.Filled.Timer, Icons.Outlined.Timer),
    TopLevelNavItem(SaviiRoute.HUB, "Hub", Icons.Filled.Apps, Icons.Outlined.Apps),
)

@Composable
fun SaviiApp(
    viewModel: SaviiViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: SaviiRoute.HOME

    fun navigateToTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    SaviiFocusTheme(themeMode = uiState.themeMode) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            val useNavRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!useNavRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ) {
                            TopLevelItems.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTopLevel(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label,
                                        )
                                    },
                                    label = { Text(item.label) },
                                    modifier = Modifier.testTag("nav_${item.route}"),
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    if (useNavRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ) {
                            TopLevelItems.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTopLevel(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label,
                                        )
                                    },
                                    label = { Text(item.label) },
                                    modifier = Modifier.testTag("rail_${item.route}"),
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        NavHost(
                            navController = navController,
                            startDestination = SaviiRoute.HOME,
                        ) {
                            composable(SaviiRoute.HOME) {
                                HomeScreen(
                                    uiState = uiState,
                                    onNavigate = { route ->
                                        if (TopLevelItems.any { it.route == route }) {
                                            navigateToTopLevel(route)
                                        } else {
                                            navController.navigate(route) { launchSingleTop = true }
                                        }
                                    },
                                    onReconcileTimer = viewModel::reconcileCompletedTimerIfNeeded,
                                )
                            }
                            composable(SaviiRoute.LEARN) {
                                LearnScreen(
                                    uiState = uiState,
                                    onAddNode = viewModel::addSyllabusNode,
                                    onToggleComplete = viewModel::toggleSyllabusNodeComplete,
                                    onCycleUnderstanding = viewModel::cycleNodeUnderstanding,
                                    onToggleWeak = viewModel::toggleWeakTopic,
                                    onIncrementRevision = viewModel::incrementRevision,
                                    onDeleteNode = viewModel::deleteSyllabusNode,
                                    onUpdateSubjectStatus = viewModel::updateSubjectStatus,
                                    onOpenExams = { navController.navigate(SaviiRoute.EXAMS) { launchSingleTop = true } },
                                )
                            }
                            composable(SaviiRoute.PLANNER) {
                                PlannerScreen(
                                    uiState = uiState,
                                    onAddTask = viewModel::addTask,
                                    onUpdateTaskStatus = viewModel::updateTaskStatus,
                                    onDeleteTask = viewModel::deleteTask,
                                    onAddSubtask = viewModel::addSubtask,
                                    onToggleSubtask = viewModel::toggleSubtask,
                                )
                            }
                            composable(SaviiRoute.FOCUS) {
                                FocusScreen(
                                    uiState = uiState,
                                    onSelectConfig = viewModel::selectFocusConfig,
                                    onStart = viewModel::startFocus,
                                    onPause = viewModel::pauseFocus,
                                    onResume = viewModel::resumeFocus,
                                    onReset = viewModel::resetFocus,
                                    onFinishSession = viewModel::finishFocusSession,
                                    onStartBreak = viewModel::startBreakTimer,
                                    onReconcileTimer = viewModel::reconcileCompletedTimerIfNeeded,
                                    onDeleteSession = viewModel::deleteFocusSession,
                                )
                            }
                            composable(SaviiRoute.HUB) {
                                ModulesHubScreen(
                                    onNavigate = { route ->
                                        navController.navigate(route) { launchSingleTop = true }
                                    },
                                )
                            }
                            composable(SaviiRoute.EXAMS) {
                                ExamsScreen(
                                    uiState = uiState,
                                    onAddExam = viewModel::addExam,
                                    onUpdateRevision = viewModel::updateExamRevision,
                                    onDeleteExam = viewModel::deleteExam,
                                    onBack = { navController.popBackStack() },
                                )
                            }
                            composable(SaviiRoute.CALENDAR) {
                                CalendarScreen(
                                    uiState = uiState,
                                    onBack = { navController.popBackStack() },
                                )
                            }
                            composable(SaviiRoute.INSIGHTS) {
                                InsightsScreen(
                                    uiState = uiState,
                                    onBack = { navController.popBackStack() },
                                )
                            }
                            composable(SaviiRoute.SETTINGS) {
                                SettingsScreen(
                                    uiState = uiState,
                                    onSelectTheme = viewModel::setThemeMode,
                                    onBack = { navController.popBackStack() },
                                )
                            }
                            composable(SaviiRoute.SAVII_AI) {
                                FeasibilityInfoScreen(
                                    title = "SAVII AI",
                                    subtitle = "Approval-first study intelligence",
                                    architectureNote = "Not active in Phase 2 foundation. When enabled later, SAVII AI will summarize authorized local Room stats into compact prompts, require explicit manual trigger, cache results, and never auto-activate tasks or study plans without your tap to approve.",
                                    hyperOsNote = "On Redmi Note 14 (HyperOS), keeping AI strictly manual and local-first avoids background battery drain and avoids uploading sensitive study logs or notifications.",
                                    nextPhaseNote = "Recommended in Phase 4 (after Focus notification bar and Biometric Shield): implement local-only heuristic advisor first, then optional Firebase AI Logic behind explicit user consent.",
                                    onBack = { navController.popBackStack() },
                                )
                            }
                            composable(SaviiRoute.PROTOCOL) {
                                FeasibilityInfoScreen(
                                    title = "SAVII Focus Protocol",
                                    subtitle = "Focus Bar & distraction rules feasibility",
                                    architectureNote = "Intentionally not enforced in Phase 2 so no risky permissions (UsageStats, Overlay, NotificationListener, Accessibility) are added prematurely. User allowlists will always take priority over auto-classification.",
                                    hyperOsNote = "HyperOS / Xiaomi limits: Standard apps CANNOT force-stop other apps or guarantee an iOS-style system Dynamic Island. HyperOS kills background services aggressively unless the user enables Autostart + 'No restrictions' battery saver. Reliable Focus Bar = Android 14+ Foreground Service (specialUse/shortService/chronometer notification) + optional permission-based overlay with an always-accessible Off/Emergency Override button.",
                                    nextPhaseNote = "Recommended as the VERY NEXT phase: 1) Ongoing Focus Notification + Foreground Service (POST_NOTIFICATIONS + FOREGROUND_SERVICE_SPECIAL_USE), 2) Optional UsageStatsManager + user-defined allowlist, 3) Optional NotificationListenerService with strict privacy filter.",
                                    onBack = { navController.popBackStack() },
                                )
                            }
                            composable(SaviiRoute.SHIELD) {
                                FeasibilityInfoScreen(
                                    title = "SAVII Shield",
                                    subtitle = "App lock & privacy overview",
                                    architectureNote = "Not implemented in Phase 2. Planned scope: BiometricPrompt app lock, Android Keystore key wrapping, encrypted sensitive preferences, and a live permission audit page. Shield is a privacy lock—NOT an antivirus and does not replace Google Play Protect.",
                                    hyperOsNote = "BiometricPrompt works natively on Redmi Note 14 (fingerprint + device PIN fallback). HyperOS already has its own App Lock; SAVII Shield secures SAVII FOCUS itself without clashing with system locks.",
                                    nextPhaseNote = "Recommended after Focus Protocol: add androidx.biometric:biometric and a permission-status overview screen.",
                                    onBack = { navController.popBackStack() },
                                )
                            }
                            composable(SaviiRoute.CONNECT) {
                                FeasibilityInfoScreen(
                                    title = "SAVII Connect",
                                    subtitle = "Official external integrations",
                                    architectureNote = "Future module. Will only use official APIs (e.g., Google Calendar API via OAuth2) after core offline scheduling is stable.",
                                    hyperOsNote = "Works cleanly via Credential Manager / OAuth2 browser intent without special Xiaomi OEM permissions.",
                                    nextPhaseNote = "Schedule after Shield & AI approval flows are verified.",
                                    onBack = { navController.popBackStack() },
                                )
                            }
                            composable(SaviiRoute.CLOUD) {
                                FeasibilityInfoScreen(
                                    title = "SAVII Cloud",
                                    subtitle = "Optional backup & sync",
                                    architectureNote = "Future module. The app remains 100% offline-first using Room + DataStore. Cloud sync (e.g., Firebase Firestore free tier vs manual encrypted JSON export) will be evaluated against free quotas before enabling.",
                                    hyperOsNote = "Works seamlessly via WorkManager periodic/manual sync constraints (unmetered network, battery not low) to respect HyperOS power limits.",
                                    nextPhaseNote = "Evaluate local encrypted export/import first (zero cost, zero quota), then optional Firebase backup.",
                                    onBack = { navController.popBackStack() },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

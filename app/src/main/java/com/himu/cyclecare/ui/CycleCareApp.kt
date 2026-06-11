package com.himu.cyclecare.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.himu.cyclecare.domain.CyclePhase
import com.himu.cyclecare.domain.CyclePrediction
import com.himu.cyclecare.domain.CycleSettings
import com.himu.cyclecare.domain.DailySymptomLog
import com.himu.cyclecare.domain.FlowLevel
import com.himu.cyclecare.domain.PeriodEntry
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
private val shortFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")
private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

private data class Destination(val route: String, val label: String, val icon: ImageVector)

private val destinations = listOf(
    Destination("home", "Today", Icons.Default.Home),
    Destination("calendar", "Calendar", Icons.Default.CalendarMonth),
    Destination("log", "Log", Icons.Default.WaterDrop),
    Destination("relief", "Relief", Icons.Default.LocalHospital),
    Destination("settings", "Settings", Icons.Default.Settings),
)

@Composable
fun CycleCareApp(
    onRequestNotifications: () -> Unit,
    viewModel: CycleViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.periods.isEmpty()) {
        OnboardingScreen(
            settings = state.settings,
            onSave = { date, length ->
                viewModel.completeOnboarding(date, length)
                onRequestNotifications()
            },
        )
        return
    }

    val navController = rememberNavController()
    var logDate by remember { mutableStateOf(LocalDate.now()) }
    var logReturnRoute by remember { mutableStateOf("home") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val backStack by navController.currentBackStackEntryAsState()
    val openLog: (LocalDate) -> Unit = { date ->
        logDate = date
        logReturnRoute = backStack?.destination?.route?.takeIf { it != "log" } ?: "home"
        navController.navigate("log") {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val finishLogAction: (String) -> Unit = { message ->
        if (logReturnRoute != "log") {
            navController.navigate(logReturnRoute) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = backStack?.destination?.route == destination.route,
                        onClick = {
                            if (destination.route == "log") {
                                logDate = LocalDate.now()
                                logReturnRoute = "home"
                            }
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding),
        ) {
            composable("home") { DashboardScreen(state, onCheckIn = { openLog(LocalDate.now()) }) }
            composable("calendar") { CalendarScreen(state, onSelectDate = openLog) }
            composable("log") {
                LogScreen(
                    state = state,
                    initialDate = logDate,
                    onSaveLog = { log ->
                        viewModel.saveLog(log) { finishLogAction("Check-in saved") }
                    },
                    onAddPeriod = { period ->
                        viewModel.addPeriod(period) { finishLogAction("Period day 1 recorded") }
                    },
                    onEditPeriod = { period ->
                        viewModel.addPeriod(period) {
                            scope.launch { snackbarHostState.showSnackbar("Period date updated") }
                        }
                    },
                    onDeletePeriod = { period ->
                        viewModel.deletePeriod(period) {
                            scope.launch { snackbarHostState.showSnackbar("Period entry deleted") }
                        }
                    },
                )
            }
            composable("relief") { ReliefScreen() }
            composable("settings") {
                SettingsScreen(state.settings, viewModel::updateSettings, onRequestNotifications)
            }
        }
    }
}

@Composable
private fun OnboardingScreen(settings: CycleSettings, onSave: (LocalDate, Int) -> Unit) {
    var date by remember { mutableStateOf(LocalDate.now()) }
    var periodLength by remember { mutableStateOf(settings.periodLength) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Text("Cycle Care", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text("Private cycle awareness on your device", color = MaterialTheme.colorScheme.secondary)
        }
        item {
            InfoCard("Start with your most recent period", "Predictions begin with a 28-day estimate and adapt after at least two completed cycle intervals.")
        }
        item {
            Text("First day of your last period", fontWeight = FontWeight.SemiBold)
            DateButton(date) { date = it }
        }
        item {
            Stepper("Usual bleeding duration", periodLength, "days", 1..10) { periodLength = it }
        }
        item {
            Button(onClick = { onSave(date, periodLength) }, modifier = Modifier.fillMaxWidth()) {
                Text("Create my cycle")
            }
        }
        item {
            Text(
                "Cycle dates are estimates, cannot confirm ovulation, and must not be used as contraception.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun DashboardScreen(state: CycleUiState, onCheckIn: () -> Unit) {
    val prediction = state.prediction
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Today", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(LocalDate.now().format(dateFormatter), color = MaterialTheme.colorScheme.secondary)
        }
        if (prediction != null) {
            val today = LocalDate.now()
            val days = ChronoUnit.DAYS.between(today, prediction.nextPeriod)
            val overdueDays = prediction.daysOverdue(today)
            item {
                HeroCard(
                    title = when {
                        overdueDays > 0 -> "Period may be $overdueDays ${if (overdueDays == 1L) "day" else "days"} late"
                        days == 0L -> "Period expected today"
                        else -> "$days days until your period"
                    },
                    subtitle = if (overdueDays > 0) {
                        "Expected ${prediction.nextPeriod.format(dateFormatter)} · Add a new start date when it arrives"
                    } else {
                        "Estimated ${prediction.nextPeriod.format(dateFormatter)}"
                    },
                )
            }
            if (overdueDays == 0L) {
                item {
                    val phase = prediction.phaseOn(today)
                    PhaseCard(phase, prediction.isPremenstrual(today))
                }
            } else {
                item {
                    InfoCard(
                        "Awaiting your update",
                        "Cycle timing can vary. Cycle Care will not assume another period occurred until you record it.",
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("Estimated ovulation", prediction.ovulationDate.format(shortFormatter), Modifier.weight(1f))
                    MetricCard("Cycle length", "${prediction.cycleLength} days", Modifier.weight(1f))
                }
            }
            item {
                InfoCard(
                    "Prediction quality",
                    if (prediction.learnedFromHistory) "Based on the median of your recent cycles."
                    else "Using your 28-day starting estimate until more history is available.",
                )
            }
        }
        val todayLog = state.logs.firstOrNull { it.date == LocalDate.now() }
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Today's check-in", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        todayLog?.let { "Pain ${it.pain}/10 · ${it.flow.name.lowercase()} flow" }
                            ?: "Track flow, pain, symptoms, medicine, or a quick note.",
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onCheckIn) {
                        Text(if (todayLog == null) "Check in now" else "Edit today's check-in")
                    }
                }
            }
        }
        val recentLengths = state.periods.map { it.startDate }.distinct().sorted().zipWithNext { first, second ->
            ChronoUnit.DAYS.between(first, second)
        }.takeLast(6)
        if (recentLengths.any { it !in 24L..38L }) {
            item {
                AlertCard(
                    "Cycle variation detected",
                    "A recent recorded cycle was outside 24–38 days. If this persists or concerns you, discuss the history with a clinician.",
                )
            }
        }
        item {
            Text(
                "Severe pain or bleeding that stops normal activities is not something the app should normalize. Use the Relief tab for care guidance.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun CalendarScreen(state: CycleUiState, onSelectDate: (LocalDate) -> Unit) {
    val prediction = state.prediction ?: return
    var visibleMonth by remember { mutableStateOf(YearMonth.now()) }
    val firstDay = visibleMonth.atDay(1)
    val gridStart = firstDay.minusDays((firstDay.dayOfWeek.value - 1).toLong())
    val lastDay = visibleMonth.atEndOfMonth()
    val gridEnd = lastDay.plusDays((7 - lastDay.dayOfWeek.value).toLong())
    val dayCount = ChronoUnit.DAYS.between(gridStart, gridEnd) + 1
    val days = remember(visibleMonth) { (0L until dayCount).map(gridStart::plusDays) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Cycle calendar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Tap any day to view or edit its check-in", color = MaterialTheme.colorScheme.secondary)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = { visibleMonth = visibleMonth.minusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                }
                Text(visibleMonth.format(monthFormatter), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = { visibleMonth = visibleMonth.plusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth()) {
                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { label ->
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
        items(days.chunked(7)) { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { date ->
                    CalendarDay(
                        date = date,
                        inVisibleMonth = YearMonth.from(date) == visibleMonth,
                        isRecordedPeriod = state.periods.any { period ->
                            date >= period.startDate && date <= (period.endDate ?: period.startDate)
                        },
                        isEstimatedPeriod = isEstimatedPeriodDate(date, prediction),
                        hasLog = state.logs.any { it.date == date },
                        onClick = { onSelectDate(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item {
            Card {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("What the calendar means", fontWeight = FontWeight.Bold)
                    CalendarLegend(
                        title = "Recorded period",
                        detail = "Pink day background",
                        color = MaterialTheme.colorScheme.primaryContainer,
                    )
                    CalendarLegend(
                        title = "Estimated period",
                        detail = "Beige day background",
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f),
                    )
                    CalendarLegend(
                        title = "Daily check-in saved",
                        detail = "Small dark dot inside the day",
                        color = MaterialTheme.colorScheme.secondary,
                        isDot = true,
                    )
                }
            }
        }
        item {
            Text(
                if (prediction.isOverdue(LocalDate.now())) {
                    "The expected date has passed, so later cycles are not projected until a new period is recorded."
                } else {
                    "Future period dates are estimates. Ovulation estimates cannot confirm fertility and are not birth control."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate,
    inVisibleMonth: Boolean,
    isRecordedPeriod: Boolean,
    isEstimatedPeriod: Boolean,
    hasLog: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = when {
        isRecordedPeriod -> MaterialTheme.colorScheme.primaryContainer
        isEstimatedPeriod -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
    Card(
        modifier = modifier.aspectRatio(0.82f).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Box(Modifier.fillMaxSize().padding(6.dp)) {
            Text(
                date.dayOfMonth.toString(),
                color = when {
                    date == LocalDate.now() -> MaterialTheme.colorScheme.primary
                    inVisibleMonth -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                },
                fontWeight = if (date == LocalDate.now()) FontWeight.Bold else FontWeight.Normal,
            )
            if (hasLog) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .size(6.dp)
                        .background(MaterialTheme.colorScheme.secondary, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun CalendarLegend(title: String, detail: String, color: Color, isDot: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .size(if (isDot) 8.dp else 22.dp)
                .background(color, if (isDot) CircleShape else CardDefaults.shape),
        )
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

private fun isEstimatedPeriodDate(date: LocalDate, prediction: CyclePrediction): Boolean {
    if (date < prediction.nextPeriod) return false
    val offset = ChronoUnit.DAYS.between(prediction.nextPeriod, date)
    val cycleIndex = offset / prediction.cycleLength
    if (prediction.isOverdue(LocalDate.now()) && cycleIndex > 0) return false
    val estimatedStart = prediction.nextPeriod.plusDays(cycleIndex * prediction.cycleLength)
    return date < estimatedStart.plusDays(prediction.periodLength.toLong())
}

@Composable
private fun LogScreen(
    state: CycleUiState,
    initialDate: LocalDate,
    onSaveLog: (DailySymptomLog) -> Unit,
    onAddPeriod: (PeriodEntry) -> Unit,
    onEditPeriod: (PeriodEntry) -> Unit,
    onDeletePeriod: (PeriodEntry) -> Unit,
) {
    var date by remember { mutableStateOf(initialDate) }
    var pain by remember { mutableStateOf(0f) }
    var flow by remember { mutableStateOf(FlowLevel.NONE) }
    var symptoms by remember { mutableStateOf(setOf<String>()) }
    var medicine by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val symptomOptions = listOf("Cramps", "Headache", "Bloating", "Fatigue", "Mood changes", "Breast tenderness")
    val existingLog = state.logs.firstOrNull { it.date == date }

    LaunchedEffect(initialDate) {
        date = initialDate
    }
    LaunchedEffect(date, existingLog) {
        pain = existingLog?.pain?.toFloat() ?: 0f
        flow = existingLog?.flow ?: FlowLevel.NONE
        symptoms = existingLog?.symptoms ?: emptySet()
        medicine = existingLog?.medicine.orEmpty()
        notes = existingLog?.notes.orEmpty()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(if (existingLog == null) "Daily check-in" else "Edit check-in", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                if (existingLog == null) "A quick record is enough. Add only what is useful today."
                else "Your saved details are ready to review or update.",
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        item { DateButton(date) { selected -> date = selected } }
        item {
            Text("Pain: ${pain.toInt()}/10", fontWeight = FontWeight.SemiBold)
            Slider(value = pain, onValueChange = { pain = it }, valueRange = 0f..10f, steps = 9)
            if (pain >= 7) Text("Severe pain deserves medical attention, especially if it is new or disrupts normal activity.", color = MaterialTheme.colorScheme.error)
        }
        item {
            Text("Flow", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FlowLevel.entries.forEach { option ->
                    FilterChip(selected = flow == option, onClick = { flow = option }, label = { Text(option.name.lowercase()) })
                }
            }
        }
        item {
            Text("Symptoms", fontWeight = FontWeight.SemiBold)
            Column {
                symptomOptions.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { symptom ->
                            FilterChip(
                                selected = symptom in symptoms,
                                onClick = { symptoms = if (symptom in symptoms) symptoms - symptom else symptoms + symptom },
                                label = { Text(symptom) },
                            )
                        }
                    }
                }
            }
        }
        item { OutlinedTextField(medicine, { medicine = it }, label = { Text("Medicine taken") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
        item {
            Button(
                onClick = {
                    onSaveLog(DailySymptomLog(date, flow, pain.toInt(), symptoms, medicine.trim(), notes.trim()))
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (existingLog == null) "Save check-in" else "Update check-in") }
        }
        item {
            OutlinedButton(
                onClick = {
                    val length = state.settings.periodLength.toLong()
                    onAddPeriod(PeriodEntry(startDate = date, endDate = date.plusDays(length - 1)))
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Record this as period day 1") }
        }
        item { HorizontalDivider(); Text("Period history", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp)) }
        items(state.periods.sortedByDescending { it.startDate }) { period ->
            val context = LocalContext.current
            Card {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(period.startDate.format(dateFormatter), fontWeight = FontWeight.SemiBold)
                        Text("Started on cycle day 1", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val revisedStart = LocalDate.of(year, month + 1, day)
                                onEditPeriod(
                                    period.copy(
                                        startDate = revisedStart,
                                        endDate = revisedStart.plusDays(state.settings.periodLength.toLong() - 1),
                                    ),
                                )
                            },
                            period.startDate.year,
                            period.startDate.monthValue - 1,
                            period.startDate.dayOfMonth,
                        ).show()
                    }) { Text("Edit") }
                    IconButton(onClick = { onDeletePeriod(period) }) { Icon(Icons.Default.Delete, "Delete period") }
                }
            }
        }
    }
}

@Composable
private fun ReliefScreen() {
    val uriHandler = LocalUriHandler.current
    val contraindications = listOf(
        "Possible pregnancy",
        "Stomach ulcer or bleeding",
        "Kidney or heart disease",
        "Blood thinner use",
        "NSAID allergy or asthma reaction",
    )
    var selected by remember { mutableStateOf(setOf<String>()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("Pain relief", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item {
            AlertCard(
                "Get urgent medical help",
                "For sudden or extreme pain, fainting, fever, possible pregnancy with significant pain, soaking a pad or tampon every hour, or rapidly worsening symptoms.",
            )
        }
        item {
            InfoCard("Home measures", "Use a wrapped heat pad or hot-water bottle, take a warm bath or shower, gently massage the abdomen or back, and try light walking or stretching if comfortable.")
        }
        item {
            Text("Medicine safety check", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("Select anything that applies before viewing OTC options.")
        }
        items(contraindications) { item ->
            Row(
                Modifier.fillMaxWidth().clickable {
                    selected = if (item in selected) selected - item else selected + item
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = item in selected,
                    onCheckedChange = { checked ->
                        selected = if (checked) selected + item else selected - item
                    },
                )
                Text(item)
            }
        }
        item {
            if (selected.isEmpty()) {
                InfoCard(
                    "Safety result: OTC options available",
                    "Ibuprofen or naproxen may help period cramps. Acetaminophen/paracetamol is another pain-relief option. Follow your local package label or ask a pharmacist; do not combine ibuprofen with naproxen, and check combination products to avoid duplicate acetaminophen/paracetamol.",
                )
            } else {
                AlertCard(
                    "Safety result: ask a pharmacist or clinician first",
                    "${selected.size} ${if (selected.size == 1) "selected condition can" else "selected conditions can"} change which pain medicines are safe. Do not rely on generalized app guidance.",
                )
            }
        }
        item {
            InfoCard("Arrange a routine appointment", "Seek clinical advice when pain disrupts work, school, sleep, or normal activities; repeatedly lasts beyond the period; becomes progressively worse; or cycles stay outside 24–38 days.")
        }
        item {
            Text("Sources", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = { uriHandler.openUri("https://womenshealth.gov/menstrual-cycle/your-menstrual-cycle") }) { Text("HHS menstrual cycle guidance") }
            TextButton(onClick = { uriHandler.openUri("https://www.nhs.uk/symptoms/period-pain/") }) { Text("NHS period pain guidance") }
            TextButton(onClick = { uriHandler.openUri("https://www.fda.gov/drugs/postmarket-drug-safety-information-patients-and-providers/nonsteroidal-anti-inflammatory-drugs-nsaids") }) { Text("FDA NSAID safety information") }
            Text("Educational information only · Adults 18+ · References checked: 11 June 2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun SettingsScreen(
    settings: CycleSettings,
    onUpdate: (CycleSettings) -> Unit,
    onRequestNotifications: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { Stepper("Starting cycle length", settings.cycleLength, "days", 21..45) { onUpdate(settings.copy(cycleLength = it)) } }
        item { Stepper("Period length", settings.periodLength, "days", 1..10) { onUpdate(settings.copy(periodLength = it)) } }
        item { Stepper("Reminder time", settings.reminderHour, ":00", 0..23) { onUpdate(settings.copy(reminderHour = it)) } }
        item { SettingSwitch("Period reminders", "3 days before, 1 day before, and expected day", settings.periodReminders) { onUpdate(settings.copy(periodReminders = it)) } }
        item { SettingSwitch("Ovulation reminder", "Estimated day only", settings.ovulationReminders) { onUpdate(settings.copy(ovulationReminders = it)) } }
        item { SettingSwitch("Premenstrual reminder", "Five days before expected period", settings.premenstrualReminders) { onUpdate(settings.copy(premenstrualReminders = it)) } }
        item { SettingSwitch("Private notification text", "Hide cycle details on the lock screen", settings.privateNotifications) { onUpdate(settings.copy(privateNotifications = it)) } }
        item { OutlinedButton(onClick = onRequestNotifications, modifier = Modifier.fillMaxWidth()) { Text("Allow notification reminders") } }
        item {
            InfoCard("Privacy", "Cycle records remain in this app's local storage. Cloud backup, accounts, analytics, and data sharing are disabled.")
        }
        item {
            Text("Changing an earlier period date recalculates predictions and replaces scheduled reminders.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DateButton(date: LocalDate, onDate: (LocalDate) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            DatePickerDialog(context, { _, year, month, day -> onDate(LocalDate.of(year, month + 1, day)) }, date.year, date.monthValue - 1, date.dayOfMonth).show()
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text(date.format(dateFormatter)) }
}

@Composable
private fun Stepper(label: String, value: Int, suffix: String, range: IntRange, onValue: (Int) -> Unit) {
    Card {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            OutlinedButton(onClick = { onValue((value - 1).coerceAtLeast(range.first)) }, enabled = value > range.first) { Text("−") }
            Text("$value$suffix", Modifier.padding(horizontal = 12.dp), textAlign = TextAlign.Center)
            OutlinedButton(onClick = { onValue((value + 1).coerceAtMost(range.last)) }, enabled = value < range.last) { Text("+") }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Card {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            Switch(checked, onChecked)
        }
    }
}

@Composable
private fun HeroCard(title: String, subtitle: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(22.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(subtitle)
        }
    }
}

@Composable
private fun PhaseCard(phase: CyclePhase, premenstrual: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = phaseColor(phase).copy(alpha = 0.18f))) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Current phase", style = MaterialTheme.typography.labelLarge)
            Text(phase.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (premenstrual) AssistChip(onClick = {}, label = { Text("Premenstrual window") })
            Text("Estimated from recorded dates", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) { Column(Modifier.padding(14.dp)) { Text(title, style = MaterialTheme.typography.bodySmall); Text(value, fontWeight = FontWeight.Bold) } }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card { Column(Modifier.fillMaxWidth().padding(16.dp)) { Text(title, fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp)); Text(body) } }
}

@Composable
private fun AlertCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f))) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) { Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(4.dp)); Text(body) }
    }
}

private fun phaseColor(phase: CyclePhase): Color = when (phase) {
    CyclePhase.MENSTRUAL -> Color(0xFFC94C70)
    CyclePhase.FOLLICULAR -> Color(0xFF4E8E77)
    CyclePhase.OVULATION -> Color(0xFFD38C27)
    CyclePhase.LUTEAL -> Color(0xFF7455A5)
}

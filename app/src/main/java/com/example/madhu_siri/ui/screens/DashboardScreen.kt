package com.example.madhu_siri.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.madhu_siri.data.model.HealthLog
import com.example.madhu_siri.data.model.HealthLogType
import com.example.madhu_siri.data.model.Hive
import com.example.madhu_siri.data.model.SprayEvent
import com.example.madhu_siri.ui.components.EmptyState
import com.example.madhu_siri.ui.components.InlineMessage
import com.example.madhu_siri.ui.components.LoadingBlock
import com.example.madhu_siri.ui.components.SectionCard
import com.example.madhu_siri.ui.localization.LocalAppText
import com.example.madhu_siri.ui.localization.TextKey
import com.example.madhu_siri.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(viewModel: MainViewModel, role: String) {
    val uiState by viewModel.uiState.collectAsState()
    val healthLogState by viewModel.healthLogState.collectAsState()

    if (uiState.isLoading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            LoadingBlock("Loading dashboard…")
        }
        return
    }

    if (role == "BEEKEEPER") {
        BeekeeperDashboard(
            viewModel = viewModel,
            hives = uiState.myHives,
            alerts = uiState.nearbySprayAlerts,
            hivesInAlertRadius = uiState.beekeeperHivesInAlertRadius,
            logs = uiState.healthLogs,
            notificationCount = uiState.notifications.size,
            logStateSuccess = healthLogState.successMessage,
            logStateError = healthLogState.errorMessage
        )
    } else {
        FarmerDashboard(
            sprayHistory = uiState.sprayHistory,
            visibleSprays = uiState.activeSprayEvents,
            nearbyHiveCount = uiState.farmerNearbyHiveCount,
            nearbyVisibleSprayCount = uiState.farmerNearbyVisibleSprayCount,
            farmerName = uiState.currentUser?.fullName.orEmpty(),
            hasReferenceLocation = uiState.lastFarmerReferenceSpray != null
        )
    }
}

@Composable
private fun FarmerDashboard(
    sprayHistory: List<SprayEvent>,
    visibleSprays: List<SprayEvent>,
    nearbyHiveCount: Int,
    nearbyVisibleSprayCount: Int,
    farmerName: String,
    hasReferenceLocation: Boolean
) {
    val tr = LocalAppText.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DashboardHeroCard(
                title = if (farmerName.isBlank()) "Farmer dashboard" else "Welcome, $farmerName",
                subtitle = tr(TextKey.FARMER_DASHBOARD_SUBTITLE),
                icon = Icons.Default.Agriculture,
                accent = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiaryContainer)
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardStatCard(
                    stat = DashboardStat(
                        label = "Visible spray zones",
                        value = nearbyVisibleSprayCount.toString(),
                        caption = if (hasReferenceLocation) "Within 2 km of your last spray point" else "Based on current saved spray history",
                        icon = Icons.Default.Warning
                    ),
                    modifier = Modifier.weight(1f)
                )
                DashboardStatCard(
                    stat = DashboardStat(
                        label = "Hive markers",
                        value = nearbyHiveCount.toString(),
                        caption = if (hasReferenceLocation) "Within 2 km of your last spray point" else "Visible around your latest known work area",
                        icon = Icons.Default.Eco
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            DashboardStatCard(
                stat = DashboardStat(
                    label = "History",
                    value = sprayHistory.size.toString(),
                    caption = "Past spray alerts saved for this account",
                    icon = Icons.Default.History
                )
            )
        }
        item {
            InsightStrip(
                title = "Field readiness",
                detail = if (nearbyVisibleSprayCount == 0) {
                    "No active red safety zones right now. If the map is clear, nearby beekeepers can usually release bees safely."
                } else {
                    "$nearbyVisibleSprayCount spray zone(s) are still within the protection window near your work area. Wait for those red markers to disappear before nearby bee release."
                }
            )
        }
        item {
            Text(tr(TextKey.SPRAY_HISTORY), style = MaterialTheme.typography.titleLarge)
        }
        if (sprayHistory.isEmpty()) {
            item {
                EmptyState(
                    title = tr(TextKey.NO_SPRAY_HISTORY),
                    message = tr(TextKey.NO_SPRAY_HISTORY_HELP)
                )
            }
        } else {
            items(sprayHistory, key = { it.id }) { event ->
                SprayEventCard(event)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BeekeeperDashboard(
    viewModel: MainViewModel,
    hives: List<Hive>,
    alerts: List<SprayEvent>,
    hivesInAlertRadius: Int,
    logs: List<HealthLog>,
    notificationCount: Int,
    logStateSuccess: String?,
    logStateError: String?
) {
    val tr = LocalAppText.current
    var selectedType by rememberSaveable { mutableStateOf(HealthLogType.HEALTH) }
    var selectedHiveId by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("") }
    var metric by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var hiveExpanded by remember { mutableStateOf(false) }

    val selectedHive = hives.firstOrNull { it.id == selectedHiveId } ?: hives.firstOrNull()
    LaunchedEffect(hives, selectedHiveId) {
        if (selectedHiveId.isBlank() && hives.isNotEmpty()) {
            selectedHiveId = hives.first().id
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DashboardHeroCard(
                title = "Beekeeper dashboard",
                subtitle = tr(TextKey.BEEKEEPER_DASHBOARD_SUBTITLE),
                icon = Icons.Default.Eco,
                accent = listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.secondaryContainer)
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardStatCard(
                    stat = DashboardStat(
                        label = "Your hives",
                        value = hivesInAlertRadius.toString(),
                        caption = "Your hives currently inside the 2 km alert zone",
                        icon = Icons.Default.Home
                    ),
                    modifier = Modifier.weight(1f)
                )
                DashboardStatCard(
                    stat = DashboardStat(
                        label = "Nearby alerts",
                        value = alerts.size.toString(),
                        caption = "Visible until bee release is safe",
                        icon = Icons.Default.Warning
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            DashboardStatCard(
                stat = DashboardStat(
                    label = "Notifications",
                    value = notificationCount.toString(),
                    caption = "Saved alert records for your account",
                    icon = Icons.Default.Notifications
                )
            )
        }
        item {
            Text(tr(TextKey.ACTIVE_ALERTS_NEAR_HIVES), style = MaterialTheme.typography.titleLarge)
        }
        if (alerts.isEmpty()) {
            item {
                EmptyState(
                    title = tr(TextKey.NO_ALERTS),
                    message = "No visible red spray markers remain near your hives. Bees can be released when your local conditions are safe."
                )
            }
        } else {
            items(alerts, key = { it.id }) { event ->
                SprayEventCard(event)
            }
        }
        item {
            SectionCard(title = tr(TextKey.HIVE_TRACKER)) {
                if (hives.isEmpty()) {
                    EmptyState(
                        title = "Add a hive first",
                        message = "Open the Map tab and pin at least one hive before tracking health or honey."
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { selectedType = HealthLogType.HEALTH }, modifier = Modifier.weight(1f)) {
                                Text("Health")
                            }
                            Button(onClick = { selectedType = HealthLogType.HONEY }, modifier = Modifier.weight(1f)) {
                                Text("Honey")
                            }
                        }
                        ExposedDropdownMenuBox(
                            expanded = hiveExpanded,
                            onExpandedChange = { hiveExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedHive?.name.orEmpty(),
                                onValueChange = {},
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                readOnly = true,
                                label = { Text("Hive") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = hiveExpanded) }
                            )
                            ExposedDropdownMenu(expanded = hiveExpanded, onDismissRequest = { hiveExpanded = false }) {
                                hives.forEach { hive ->
                                    DropdownMenuItem(
                                        text = { Text(hive.name) },
                                        onClick = {
                                            selectedHiveId = hive.id
                                            hiveExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = status,
                            onValueChange = { status = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(if (selectedType == HealthLogType.HEALTH) "Health status" else "Production label")
                            }
                        )
                        OutlinedTextField(
                            value = metric,
                            onValueChange = { metric = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(if (selectedType == HealthLogType.HEALTH) "Condition score" else "Honey amount (kg)")
                            }
                        )
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Notes") }
                        )
                        InlineMessage(
                            successMessage = logStateSuccess,
                            errorMessage = logStateError,
                            onDismissed = viewModel::clearHealthLogMessage
                        )
                        Button(
                            onClick = {
                                val hive = selectedHive ?: return@Button
                                viewModel.addHealthLog(
                                    hive = hive,
                                    type = selectedType,
                                    status = status,
                                    metricValue = metric.toDoubleOrNull() ?: 0.0,
                                    notes = notes
                                )
                                status = ""
                                metric = ""
                                notes = ""
                            },
                            enabled = selectedHive != null && status.isNotBlank() && metric.toDoubleOrNull() != null
                        ) {
                            Text("Save tracker entry")
                        }
                    }
                }
            }
        }
        item {
            Text(tr(TextKey.RECENT_TRACKER_ENTRIES), style = MaterialTheme.typography.titleLarge)
        }
        if (logs.isEmpty()) {
            item {
                EmptyState(
                    title = tr(TextKey.NO_LOGS),
                    message = tr(TextKey.NO_LOGS_HELP)
                )
            }
        } else {
            items(logs.take(10), key = { it.id }) { log ->
                HealthLogCard(log)
            }
        }
    }
}

private data class DashboardStat(
    val label: String,
    val value: String,
    val caption: String,
    val icon: ImageVector
)

@Composable
private fun DashboardHeroCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: List<Color>
) {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(accent.map { it.copy(alpha = 0.22f) }))
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .padding(16.dp)
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Updated ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun DashboardStatCard(stat: DashboardStat, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 148.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                    .padding(12.dp)
            ) {
                Icon(stat.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Text(stat.value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stat.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(stat.caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InsightStrip(title: String, detail: String) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun SprayEventCard(event: SprayEvent) {
    val formatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val remainingMinutes = event.remainingSafetyMillis() / 60000L
    Card(colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    )) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "${event.sprayType.lowercase().replace('_', ' ')} • ${event.chemicalName}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Start: ${formatter.format(event.scheduledAt.toDate())} • Duration: ${event.durationHours}h",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                if (remainingMinutes > 0) {
                    "Keep bees inside for about ${remainingMinutes / 60}h ${remainingMinutes % 60}m more."
                } else {
                    "Bee-safe waiting window has ended."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                "Location: ${"%.4f".format(event.lat)}, ${"%.4f".format(event.lng)}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (event.notes.isNotBlank()) {
                Text(event.notes, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun HealthLogCard(log: HealthLog) {
    val formatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    Card(colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    )) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${log.hiveName} • ${log.logType.lowercase()}", style = MaterialTheme.typography.titleMedium)
            Text(
                "${log.status} • ${log.metricValue} ${log.metricUnit}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (log.notes.isNotBlank()) {
                Text(log.notes, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                formatter.format(log.createdAt.toDate()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

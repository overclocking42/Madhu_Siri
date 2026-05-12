package com.example.madhu_siri.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.madhu_siri.data.model.Hive
import com.example.madhu_siri.data.model.SprayEvent
import com.example.madhu_siri.data.model.UserRole
import com.example.madhu_siri.ui.components.InlineMessage
import com.example.madhu_siri.ui.localization.LocalAppText
import com.example.madhu_siri.ui.localization.TextKey
import com.example.madhu_siri.viewmodel.MainViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun MapScreen(viewModel: MainViewModel) {
    val tr = LocalAppText.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val hiveActionState by viewModel.hiveActionState.collectAsState()
    val currentRole = uiState.currentUser?.role
    val isBeekeeper = currentRole == UserRole.BEEKEEPER.name
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(20.5937, 78.9629), 5.5f)
    }

    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var addHiveMode by rememberSaveable { mutableStateOf(false) }
    var draftPosition by remember { mutableStateOf<LatLng?>(null) }
    var selectedHive by remember { mutableStateOf<Hive?>(null) }
    var selectedSpray by remember { mutableStateOf<SprayEvent?>(null) }
    var editingHive by remember { mutableStateOf<Hive?>(null) }
    var relocatingHive by remember { mutableStateOf<Hive?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationPermissionGranted = granted
    }
    val mapsReady = remember(context) {
        runCatching {
            MapsInitializer.initialize(context)
        }.isSuccess
    }
    val myHiveMarker = remember(mapsReady) {
        if (mapsReady) {
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
        } else {
            null
        }
    }
    val nearbyHiveMarker = remember(mapsReady) {
        if (mapsReady) {
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
        } else {
            null
        }
    }
    val sprayMarker = remember(mapsReady) {
        if (mapsReady) {
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
        } else {
            null
        }
    }

    val focusOnCurrentLocation: () -> Unit = {
        if (!locationPermissionGranted) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            val token = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val latLng = LatLng(location.latitude, location.longitude)
                        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                    }
                }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(compassEnabled = true, myLocationButtonEnabled = false),
            onMapClick = { latLng ->
                selectedHive = null
                selectedSpray = null
                when {
                    relocatingHive != null -> {
                        draftPosition = latLng
                        editingHive = relocatingHive
                        relocatingHive = null
                    }
                    isBeekeeper && addHiveMode -> {
                        draftPosition = latLng
                        editingHive = null
                    }
                }
            }
        ) {
            uiState.allHives.forEach { hive ->
                Marker(
                    state = MarkerState(LatLng(hive.lat, hive.lng)),
                    icon = if (hive.ownerId == viewModel.getCurrentUserId()) myHiveMarker else nearbyHiveMarker,
                    title = "Hive: ${hive.name.ifBlank { "Bee Colony" }}",
                    snippet = if (hive.ownerId == viewModel.getCurrentUserId()) "Your hive location" else hive.contactNumber.ifBlank { "Beekeeper hive location" },
                    onClick = {
                        selectedHive = hive
                        selectedSpray = null
                        true
                    }
                )
            }
            uiState.activeSprayEvents.forEach { event ->
                val center = LatLng(event.lat, event.lng)
                Marker(
                    state = MarkerState(center),
                    icon = sprayMarker,
                    title = "Spray alert: ${event.chemicalName}",
                    snippet = event.sprayType.lowercase().replace('_', ' '),
                    onClick = {
                        selectedSpray = event
                        selectedHive = null
                        true
                    }
                )
                Circle(
                    center = center,
                    radius = event.radiusKm * 1000,
                    fillColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    strokeColor = MaterialTheme.colorScheme.error.copy(alpha = 0.65f),
                    strokeWidth = 2f
                )
            }
            draftPosition?.let { pending ->
                Marker(
                    state = MarkerState(pending),
                    title = if (editingHive == null) tr(TextKey.MAP_NEW_HIVE_PIN) else tr(TextKey.MAP_UPDATED_HIVE_PIN)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.70f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (isBeekeeper) tr(TextKey.MAP_SUMMARY_BEEKEEPER) else tr(TextKey.MAP_SUMMARY_FARMER),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        if (isBeekeeper) {
                            tr(TextKey.MAP_GUIDE_BEEKEEPER)
                        } else {
                            tr(TextKey.MAP_GUIDE_FARMER)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${tr(TextKey.MAP_HIVE_COUNT, uiState.allHives.size)} • ${tr(TextKey.MAP_SPRAY_COUNT, uiState.activeSprayEvents.size)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LegendRow(
                            label = "Your hives",
                            tint = Color(0xFFF4C542)
                        )
                        LegendRow(
                            label = "Nearby beekeeper hives",
                            tint = Color(0xFF2D8CFF)
                        )
                        LegendRow(
                            label = "Active spray alerts",
                            tint = Color(0xFFD93025)
                        )
                    }
                    InlineMessage(
                        successMessage = hiveActionState.successMessage,
                        errorMessage = hiveActionState.errorMessage,
                        onDismissed = viewModel::clearHiveActionMessage
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            FloatingActionButton(onClick = focusOnCurrentLocation) {
                Icon(Icons.Default.GpsFixed, contentDescription = "Current location")
            }
            if (isBeekeeper) {
                FloatingActionButton(
                    onClick = { addHiveMode = !addHiveMode },
                    containerColor = if (addHiveMode) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.AddLocationAlt, contentDescription = "Add hive")
                }
                FloatingActionButton(
                    onClick = {
                        if (!locationPermissionGranted) {
                             permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        } else {
                            val token = CancellationTokenSource()
                            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
                                .addOnSuccessListener {
                                    if (it != null) {
                                        val latLng = LatLng(it.latitude, it.longitude)
                                        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                                        draftPosition = latLng
                                        editingHive = null
                                    }
                                }
                        }
                    }
                ) {
                    Icon(Icons.Default.GpsFixed, contentDescription = "Add hive at current location")
                }
            }
        }

        when {
            selectedHive != null -> {
                val hive = selectedHive!!
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(hive.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${"%.5f".format(hive.lat)}, ${"%.5f".format(hive.lng)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (hive.notes.isNotBlank()) {
                            Text(hive.notes, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            tr(TextKey.MAP_BEES_ACTIVE, hive.activeStartTime, hive.activeEndTime),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (hive.contactNumber.isNotBlank()) {
                            Text(
                                tr(TextKey.MAP_BEEKEEPER_CONTACT, hive.contactNumber),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        if (hive.ownerId == viewModel.getCurrentUserId()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = { editingHive = hive; draftPosition = LatLng(hive.lat, hive.lng) }) {
                                    Icon(Icons.Default.EditLocationAlt, contentDescription = null)
                                    Text(" Edit")
                                }
                                Button(
                                    onClick = {
                                        viewModel.deleteHive(hive.id)
                                        selectedHive = null
                                    },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null)
                                    Text(" Remove")
                                }
                            }
                        } else {
                            Text(tr(TextKey.MAP_PROTECTED_HIVE), color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
            selectedSpray != null -> {
                val spray = selectedSpray!!
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(tr(TextKey.MAP_SPRAY_ALERT), style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${spray.sprayType.lowercase().replace('_', ' ')} • ${spray.chemicalName}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "2 km warning radius • ${spray.durationHours} hour duration",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val remainingMinutes = (spray.remainingSafetyMillis() / 60000L).coerceAtLeast(0L)
                        val remainingHours = remainingMinutes / 60
                        val remainingMinsPart = remainingMinutes % 60
                        Text(
                            if (remainingMinutes == 0L) {
                                "Bee-safe waiting window has ended."
                            } else {
                                "Bees should stay inside for another ${remainingHours}h ${remainingMinsPart}m."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        if (spray.notes.isNotBlank()) {
                            Text(spray.notes, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    if (draftPosition != null) {
        HiveEditorDialog(
            title = if (editingHive == null) tr(TextKey.MAP_SAVE_HIVE_PIN) else tr(TextKey.MAP_EDIT_HIVE),
            initialName = editingHive?.name.orEmpty(),
            initialNotes = editingHive?.notes.orEmpty(),
            initialContactNumber = editingHive?.contactNumber.orEmpty(),
            initialActiveStart = editingHive?.activeStartTime ?: "08:00",
            initialActiveEnd = editingHive?.activeEndTime ?: "18:00",
            onDismiss = {
                draftPosition = null
                editingHive = null
                addHiveMode = false
                relocatingHive = null
            },
            onMoveRequest = if (editingHive != null) {
                {
                    relocatingHive = editingHive
                    draftPosition = null
                }
            } else {
                null
            },
            onConfirm = { name, notes, activeStartTime, activeEndTime ->
                val latLng = draftPosition ?: return@HiveEditorDialog
                if (editingHive == null) {
                    viewModel.addHive(
                        name = name.ifBlank { "Hive ${uiState.myHives.size + 1}" },
                        lat = latLng.latitude,
                        lng = latLng.longitude,
                        notes = notes,
                        activeStartTime = activeStartTime,
                        activeEndTime = activeEndTime
                    )
                } else {
                    viewModel.updateHive(
                        hiveId = editingHive!!.id,
                        name = name,
                        lat = latLng.latitude,
                        lng = latLng.longitude,
                        notes = notes,
                        activeStartTime = activeStartTime,
                        activeEndTime = activeEndTime
                    )
                    selectedHive = editingHive!!.copy(
                        name = name,
                        notes = notes,
                        activeStartTime = activeStartTime,
                        activeEndTime = activeEndTime,
                        lat = latLng.latitude,
                        lng = latLng.longitude
                    )
                }
                draftPosition = null
                editingHive = null
                addHiveMode = false
                relocatingHive = null
            }
        )
    }
}

@Composable
private fun LegendRow(label: String, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = tint
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun HiveEditorDialog(
    title: String,
    initialName: String,
    initialNotes: String,
    initialContactNumber: String,
    initialActiveStart: String,
    initialActiveEnd: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit,
    onMoveRequest: (() -> Unit)? = null
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    var notes by rememberSaveable(initialNotes) { mutableStateOf(initialNotes) }
    var contactNumber by rememberSaveable(initialContactNumber) { mutableStateOf(initialContactNumber) }
    var activeStart by rememberSaveable(initialActiveStart) { mutableStateOf(initialActiveStart) }
    var activeEnd by rememberSaveable(initialActiveEnd) { mutableStateOf(initialActiveEnd) }
    val tr = LocalAppText.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Pins update in real time for all users.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr(TextKey.MAP_HIVE_NAME)) }
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr(TextKey.MAP_NOTES)) }
                )
                OutlinedTextField(
                    value = contactNumber,
                    onValueChange = { contactNumber = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr(TextKey.PHONE_NUMBER)) },
                    enabled = false
                )
                OutlinedTextField(
                    value = activeStart,
                    onValueChange = { activeStart = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Bee active start (HH:mm)") }
                )
                OutlinedTextField(
                    value = activeEnd,
                    onValueChange = { activeEnd = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Bee active end (HH:mm)") }
                )
                if (onMoveRequest != null) {
                    TextButton(onClick = onMoveRequest) {
                        Text(tr(TextKey.MAP_MOVE_PIN))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name.ifBlank { "Bee Hive" }, notes, activeStart, activeEnd) }) {
                Text(tr(TextKey.MAP_SAVE))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr(TextKey.MAP_CANCEL))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    )
}

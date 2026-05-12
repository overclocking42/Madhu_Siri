package com.example.madhu_siri.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioManager
import android.media.ToneGenerator
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.madhu_siri.data.model.SprayAlertDraft
import com.example.madhu_siri.data.model.SprayType
import com.example.madhu_siri.ui.components.BrandHeader
import com.example.madhu_siri.ui.components.EmptyState
import com.example.madhu_siri.ui.components.InlineMessage
import com.example.madhu_siri.ui.components.LoadingBlock
import com.example.madhu_siri.ui.components.SectionCard
import com.example.madhu_siri.ui.localization.LocalAppText
import com.example.madhu_siri.ui.localization.TextKey
import com.example.madhu_siri.utils.BeeTimingUtil
import com.example.madhu_siri.utils.HaversineUtil
import com.example.madhu_siri.viewmodel.MainViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@SuppressLint("MissingPermission")
@Composable
fun SprayScreen(viewModel: MainViewModel) {
    val tr = LocalAppText.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val submissionState by viewModel.spraySubmissionState.collectAsState()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var chemicalName by rememberSaveable { mutableStateOf("") }
    var durationHours by rememberSaveable { mutableStateOf("4") }
    var startDelayMinutes by rememberSaveable { mutableStateOf("60") }
    var notes by rememberSaveable { mutableStateOf("") }
    var sprayType by rememberSaveable { mutableStateOf(SprayType.PESTICIDE) }
    var selectedLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var selectedLng by rememberSaveable { mutableStateOf<Double?>(null) }
    var typeExpanded by remember { mutableStateOf(false) }
    var scheduledAtMillis by rememberSaveable { mutableLongStateOf(System.currentTimeMillis() + 60L * 60L * 1000L) }
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(20.5937, 78.9629), 5.5f)
    }

    val permissionGranted = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted.value = granted
    }
    val mapsReady = remember(context) {
        runCatching {
            MapsInitializer.initialize(context)
        }.isSuccess
    }
    val myHiveMarker = remember(mapsReady) {
        if (mapsReady) BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW) else null
    }
    val nearbyHiveMarker = remember(mapsReady) {
        if (mapsReady) BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE) else null
    }
    val sprayMarker = remember(mapsReady) {
        if (mapsReady) BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED) else null
    }

    fun refreshSchedule(minutes: Int) {
        startDelayMinutes = minutes.toString()
        scheduledAtMillis = System.currentTimeMillis() + minutes * 60L * 1000L
    }

    fun pickCurrentLocation() {
        if (!permissionGranted.value) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }
        val token = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    selectedLat = location.latitude
                    selectedLng = location.longitude
                    cameraPositionState.move(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(location.latitude, location.longitude),
                            16f
                        )
                    )
                }
            }
    }

    val canSubmit = chemicalName.isNotBlank() &&
        durationHours.toIntOrNull()?.let { it > 0 } == true &&
        startDelayMinutes.toIntOrNull()?.let { it >= 0 } == true &&
        selectedLat != null &&
        selectedLng != null

    val nearbyHives = remember(uiState.allHives, selectedLat, selectedLng) {
        if (selectedLat == null || selectedLng == null) {
            emptyList()
        } else {
            uiState.allHives.filter { hive ->
                HaversineUtil.calculateDistance(selectedLat!!, selectedLng!!, hive.lat, hive.lng) <= 2.0
            }
        }
    }
    val conflictingNearbyHives = remember(nearbyHives, scheduledAtMillis) {
        nearbyHives.filter { BeeTimingUtil.isSprayBlockedByBeeWindow(it, scheduledAtMillis) }
    }
    val proactiveSafeTimes = remember(conflictingNearbyHives, scheduledAtMillis) {
        BeeTimingUtil.suggestedSafeSprayTimes(conflictingNearbyHives, scheduledAtMillis)
    }
    val displayedSafeTimes = (submissionState.suggestedSafeTimes + proactiveSafeTimes).distinct().sorted()

    LaunchedEffect(submissionState.successMessage, submissionState.errorMessage) {
        if (submissionState.successMessage != null && submissionState.errorMessage == null) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 180)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            BrandHeader(
                title = tr(TextKey.SPRAY_CONTROL_TITLE),
                subtitle = tr(TextKey.SPRAY_CONTROL_SUBTITLE)
            )
        }
        item {
            SectionCard(title = tr(TextKey.SPRAY_TIMING_TYPE)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = sprayType.name.lowercase().replace('_', ' '),
                            onValueChange = {},
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            readOnly = true,
                            label = { Text(tr(TextKey.SPRAY_TYPE)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) }
                        )
                        ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                            SprayType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name.lowercase().replace('_', ' ')) },
                                    onClick = {
                                        sprayType = type
                                        typeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = chemicalName,
                        onValueChange = { chemicalName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(tr(TextKey.CHEMICAL_NAME)) }
                    )
                    OutlinedTextField(
                        value = durationHours,
                        onValueChange = { durationHours = it.filter(Char::isDigit) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(tr(TextKey.SPRAY_DURATION)) },
                        supportingText = { Text(tr(TextKey.SPRAY_DURATION_HELP)) }
                    )
                    OutlinedTextField(
                        value = startDelayMinutes,
                        onValueChange = {
                            startDelayMinutes = it.filter(Char::isDigit)
                            val mins = startDelayMinutes.toIntOrNull()
                            if (mins != null) {
                                scheduledAtMillis = System.currentTimeMillis() + mins * 60L * 1000L
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(tr(TextKey.SPRAY_START_IN_MINUTES)) }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0, 30, 60, 120, 240).forEach { preset ->
                            FilterChip(
                                selected = startDelayMinutes == preset.toString(),
                                onClick = { refreshSchedule(preset) },
                                label = { Text(if (preset == 0) "Now" else "${preset}m") }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(tr(TextKey.FARMER_NOTE)) },
                        minLines = 3
                    )
                }
            }
        }
        item {
            SectionCard(title = tr(TextKey.SPRAY_POINT_SELECTION)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { pickCurrentLocation() }) {
                            Text(tr(TextKey.SPRAY_USE_CURRENT_LOCATION))
                        }
                        Button(onClick = {
                            selectedLat = null
                            selectedLng = null
                        }) {
                            Text(tr(TextKey.SPRAY_CLEAR_PIN))
                        }
                    }
                    GoogleMap(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        cameraPositionState = cameraPositionState,
                        onMapClick = { latLng ->
                            selectedLat = latLng.latitude
                            selectedLng = latLng.longitude
                        }
                    ) {
                        uiState.allHives.forEach { hive ->
                            Marker(
                                state = MarkerState(LatLng(hive.lat, hive.lng)),
                                icon = if (hive.ownerId == viewModel.getCurrentUserId()) myHiveMarker else nearbyHiveMarker,
                                title = "Hive: ${hive.name.ifBlank { "Bee Colony" }}",
                                snippet = listOf(
                                    "Bees out ${hive.activeStartTime} - ${hive.activeEndTime}",
                                    hive.contactNumber.takeIf { it.isNotBlank() }?.let { "Call $it" }
                                ).filterNotNull().joinToString(" • ")
                            )
                        }
                        if (selectedLat != null && selectedLng != null) {
                            val position = LatLng(selectedLat!!, selectedLng!!)
                            Marker(
                                state = MarkerState(position),
                                icon = sprayMarker,
                                title = "Spray point",
                                snippet = chemicalName.ifBlank { "Selected field" }
                            )
                            Circle(
                                center = position,
                                radius = 2000.0,
                                fillColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
                                strokeColor = MaterialTheme.colorScheme.tertiary,
                                strokeWidth = 2f
                            )
                        }
                    }
                    Text(
                        if (selectedLat != null && selectedLng != null) {
                            tr(TextKey.SPRAY_SELECTED_POINT, selectedLat ?: 0.0, selectedLng ?: 0.0)
                        } else {
                            tr(TextKey.SPRAY_TAP_MAP_HELP)
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Use 2 fingers to move and adjust the map smoothly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (conflictingNearbyHives.isNotEmpty()) {
                        Text(
                            "That time conflicts with bee-out hours nearby. Please choose one of these safer spray times instead.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            displayedSafeTimes.forEach { safeTime ->
                                FilterChip(
                                    selected = scheduledAtMillis == safeTime,
                                    onClick = {
                                        scheduledAtMillis = safeTime
                                        startDelayMinutes = (((safeTime - System.currentTimeMillis()) / 60000L).coerceAtLeast(0L)).toString()
                                    },
                                    label = {
                                        Text(SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(safeTime)))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            AnimatedVisibility(
                visible = submissionState.successMessage != null && submissionState.errorMessage == null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut()
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            "Spray alert created successfully and shared with nearby beekeepers.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        item {
            InlineMessage(
                successMessage = submissionState.successMessage,
                errorMessage = submissionState.errorMessage,
                onDismissed = viewModel::clearSpraySubmissionMessage,
                autoDismissMillis = 6500L
            )
        }
        item {
            if (submissionState.isLoading) {
                LoadingBlock("Sending spray alert in real time…")
            } else {
                Button(
                    onClick = {
                        viewModel.submitSprayAlert(
                            SprayAlertDraft(
                                sprayType = sprayType,
                                chemicalName = chemicalName,
                                durationHours = durationHours.toInt(),
                                scheduledAtMillis = scheduledAtMillis,
                                latitude = selectedLat,
                                longitude = selectedLng,
                                notes = notes
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canSubmit
                ) {
                    Text(tr(TextKey.SPRAY_ACTION))
                }
            }
        }
        item {
            Text(
                tr(TextKey.ALERT_START, SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(scheduledAtMillis))),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        item {
            Text(tr(TextKey.NEARBY_HIVES), style = MaterialTheme.typography.titleLarge)
        }
        if (nearbyHives.isEmpty()) {
            item {
                EmptyState(
                    title = "No nearby hives in 2 km",
                    message = "Once you mark a spray point, only hives within 2 km are shown here."
                )
            }
        } else {
            items(nearbyHives.take(6), key = { it.id }) { hive ->
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(hive.name.ifBlank { "Hive" }, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Lat ${"%.4f".format(hive.lat)} • Lng ${"%.4f".format(hive.lng)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Bee active: ${hive.activeStartTime} - ${hive.activeEndTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        if (hive.contactNumber.isNotBlank()) {
                            Text(
                                "Beekeeper contact: ${hive.contactNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

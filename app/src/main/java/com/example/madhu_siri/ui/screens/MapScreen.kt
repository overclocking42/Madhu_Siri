package com.example.madhu_siri.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.madhu_siri.viewmodel.MainViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MapScreen(viewModel: MainViewModel) {
    val hives by viewModel.hives.collectAsState()
    val currentUserId = viewModel.getCurrentUserId()
    
    val singapore = LatLng(1.35, 103.87) // Default center
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(singapore, 10f)
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* Could add instruction to long press */ },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                text = { Text("Long Press Map to Add Hive") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapLongClick = { latLng ->
                viewModel.addHive(latLng.latitude, latLng.longitude)
            }
        ) {
            hives.forEach { hive ->
                val isOwner = hive.ownerId == currentUserId
                Marker(
                    state = MarkerState(position = LatLng(hive.lat, hive.lng)),
                    title = if (isOwner) "Your Hive" else "Beekeeper's Hive",
                    snippet = if (isOwner) "Long click marker to delete" else null,
                    onInfoWindowLongClick = {
                        if (isOwner) viewModel.deleteHive(hive.id)
                    }
                )
            }
        }
    }
}

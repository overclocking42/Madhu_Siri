package com.example.madhu_siri.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.madhu_siri.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(viewModel: MainViewModel, role: String) {
    val hives by viewModel.hives.collectAsState()
    val sprayEvents by viewModel.sprayEvents.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = if (role == "BEEKEEPER") "Beekeeper Dashboard" else "Farmer Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (role == "BEEKEEPER") {
            Text("Recent Spray Alerts Near You", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(sprayEvents) { event ->
                    AlertItem(event.timestamp.toDate())
                }
            }
        } else {
            Text("Your Hives", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text("You have ${hives.size} hives pinned.", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Your Spray History", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(sprayEvents) { event ->
                    AlertItem(event.timestamp.toDate())
                }
            }
        }
    }
}

@Composable
fun AlertItem(date: Date) {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Spray Event", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            Text("Time: ${sdf.format(date)}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

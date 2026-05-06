package com.example.madhu_siri.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Tip(val title: String, val content: String)

@Composable
fun TipsScreen() {
    val tips = listOf(
        Tip("Spray in the Evening", "Bees are most active during the day. Spraying after sunset significantly reduces bee mortality."),
        Tip("Use Bee-Safe Pesticides", "Look for labels that indicate low toxicity to pollinators. Avoid neonicotinoids if possible."),
        Tip("Avoid Spraying on Flowers", "Try to spray before or after the blooming period to avoid direct contact with foraging bees."),
        Tip("Check Wind Speed", "Do not spray when it's windy to prevent pesticide drift into nearby hive areas."),
        Tip("Communicate", "Always use the Madhu-Siri app to notify beekeepers at least 2 hours before you start.")
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Bee-Friendly Tips",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn {
            items(tips) { tip ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(tip.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(tip.content, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

package com.example.madhu_siri.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.madhu_siri.ui.components.BrandHeader
import com.example.madhu_siri.ui.localization.LocalAppText
import com.example.madhu_siri.ui.localization.TextKey

data class Tip(val title: String, val content: String)

@Composable
fun TipsScreen() {
    val tr = LocalAppText.current
    val tips = listOf(
        Tip(tr(TextKey.TIP_1_TITLE), tr(TextKey.TIP_1_BODY)),
        Tip(tr(TextKey.TIP_2_TITLE), tr(TextKey.TIP_2_BODY)),
        Tip(tr(TextKey.TIP_3_TITLE), tr(TextKey.TIP_3_BODY)),
        Tip(tr(TextKey.TIP_4_TITLE), tr(TextKey.TIP_4_BODY)),
        Tip(tr(TextKey.TIP_5_TITLE), tr(TextKey.TIP_5_BODY))
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            BrandHeader(
                title = tr(TextKey.TIPS_TITLE),
                subtitle = tr(TextKey.TIPS_SUBTITLE)
            )
        }
        items(tips) { tip ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                androidx.compose.foundation.layout.Column(modifier = Modifier.padding(18.dp)) {
                    Text(tip.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        tip.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

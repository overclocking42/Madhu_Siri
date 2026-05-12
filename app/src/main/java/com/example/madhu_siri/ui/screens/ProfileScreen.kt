package com.example.madhu_siri.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.madhu_siri.data.model.AppThemePreference
import com.example.madhu_siri.data.model.UserRole
import com.example.madhu_siri.data.repository.AppSettingsRepository
import com.example.madhu_siri.ui.components.EmptyState
import com.example.madhu_siri.ui.components.InlineMessage
import com.example.madhu_siri.ui.components.SectionCard
import com.example.madhu_siri.ui.localization.AppLanguage
import com.example.madhu_siri.ui.localization.LocalAppText
import com.example.madhu_siri.ui.localization.TextKey
import com.example.madhu_siri.viewmodel.AuthViewModel
import com.example.madhu_siri.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    mainViewModel: MainViewModel,
    settingsRepository: AppSettingsRepository
) {
    val tr = LocalAppText.current
    val uiState by mainViewModel.uiState.collectAsState()
    val profileState by mainViewModel.profileState.collectAsState()
    val language by settingsRepository.language.collectAsState()
    val themePreference by settingsRepository.themePreference.collectAsState()
    val user = uiState.currentUser
    val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val role = UserRole.fromValue(user?.role) ?: UserRole.FARMER
    var fullName by rememberSaveable(user?.fullName) { mutableStateOf(user?.fullName.orEmpty()) }
    var phoneNumber by rememberSaveable(user?.phoneNumber) { mutableStateOf(user?.phoneNumber.orEmpty()) }
    var selectedLanguage by rememberSaveable(language) { mutableStateOf(language) }
    var selectedTheme by rememberSaveable(themePreference) { mutableStateOf(themePreference) }

    LaunchedEffect(user?.fullName, user?.phoneNumber) {
        fullName = user?.fullName.orEmpty()
        phoneNumber = user?.phoneNumber.orEmpty()
    }
    LaunchedEffect(language, themePreference) {
        selectedLanguage = language
        selectedTheme = themePreference
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
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
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                                )
                            )
                        )
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(user?.fullName.orEmpty(), style = MaterialTheme.typography.headlineMedium)
                        Text(user?.email.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                        if (user?.phoneNumber?.isNotBlank() == true) {
                            Text(user.phoneNumber, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            "Role: ${if (role == UserRole.FARMER) tr(TextKey.ROLE_FARMER) else tr(TextKey.ROLE_BEEKEEPER)}",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }
        item {
            SectionCard(title = tr(TextKey.PROFILE_ACCOUNT_ACTIONS), accent = MaterialTheme.colorScheme.secondaryContainer) {
                androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(tr(TextKey.FULL_NAME)) }
                    )
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(tr(TextKey.PHONE_NUMBER)) }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text(tr(TextKey.APP_LANGUAGE), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppLanguage.entries.forEach { appLanguage ->
                            FilterChip(
                                selected = selectedLanguage == appLanguage,
                                onClick = {
                                    selectedLanguage = appLanguage
                                    settingsRepository.setLanguage(appLanguage)
                                },
                                label = { Text(appLanguage.nativeLabel) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text(tr(TextKey.PROFILE_THEME), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            AppThemePreference.SYSTEM to tr(TextKey.THEME_SYSTEM),
                            AppThemePreference.LIGHT to tr(TextKey.THEME_LIGHT),
                            AppThemePreference.DARK to tr(TextKey.THEME_DARK)
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = selectedTheme == mode,
                                onClick = {
                                    selectedTheme = mode
                                    settingsRepository.setThemePreference(mode)
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                    InlineMessage(
                        successMessage = profileState.successMessage,
                        errorMessage = profileState.errorMessage,
                        onDismissed = mainViewModel::clearProfileMessage,
                        autoDismissMillis = 4200L
                    )
                    Button(
                        onClick = {
                            mainViewModel.updateProfileSettings(
                                fullName = fullName,
                                phoneNumber = phoneNumber,
                                preferredLanguage = selectedLanguage,
                                themePreference = selectedTheme
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = fullName.isNotBlank() && phoneNumber.filter(Char::isDigit).length >= 10
                    ) {
                        Text(tr(TextKey.PROFILE_SAVE))
                    }
                    Button(
                        onClick = {
                            val nextRole = if (role == UserRole.FARMER) UserRole.BEEKEEPER else UserRole.FARMER
                            authViewModel.setRole(nextRole.name)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text(tr(TextKey.PROFILE_SWITCH_TO, if (role == UserRole.FARMER) tr(TextKey.ROLE_BEEKEEPER) else tr(TextKey.ROLE_FARMER)))
                    }
                    Button(onClick = { authViewModel.signOut() }, modifier = Modifier.fillMaxWidth()) {
                        Text(tr(TextKey.PROFILE_SWITCH_ACCOUNT))
                    }
                    Button(
                        onClick = { authViewModel.signOut() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text(tr(TextKey.PROFILE_LOG_OUT))
                    }
                }
            }
        }
        if (role == UserRole.FARMER) {
            item {
                SectionCard(title = tr(TextKey.PROFILE_SPRAY_HISTORY), accent = MaterialTheme.colorScheme.primaryContainer) {
                    Button(
                        onClick = mainViewModel::clearOldSprayHistory,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                        Text(" Clear records until yesterday")
                    }
                }
            }
            if (uiState.sprayHistory.isEmpty()) {
                item {
                    EmptyState(
                        title = tr(TextKey.NO_SPRAY_HISTORY),
                        message = tr(TextKey.NO_SPRAY_HISTORY_HELP)
                    )
                }
            } else {
                items(uiState.sprayHistory, key = { it.id }) { event ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("${event.chemicalName} • ${event.sprayType.lowercase()}", style = MaterialTheme.typography.titleMedium)
                            Text(formatter.format(event.scheduledAt.toDate()), style = MaterialTheme.typography.bodyMedium)
                            Text("${event.durationHours} hour alert", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        } else {
            item {
                SectionCard(title = tr(TextKey.PROFILE_NOTIFICATION_HISTORY), accent = MaterialTheme.colorScheme.primaryContainer) {
                    Button(
                        onClick = mainViewModel::clearOldNotifications,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                        Text(" Clear records until yesterday")
                    }
                }
            }
            if (uiState.notifications.isEmpty()) {
                item {
                    EmptyState(
                        title = "No notifications yet",
                        message = "Nearby spray alerts will be stored here in real time."
                    )
                }
            } else {
                items(uiState.notifications, key = { it.id }) { notification ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(notification.title, style = MaterialTheme.typography.titleMedium)
                            Text(notification.body, style = MaterialTheme.typography.bodyMedium)
                            Text(formatter.format(notification.timestamp.toDate()), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item { Text(tr(TextKey.PROFILE_HEALTH_LOGS), style = MaterialTheme.typography.titleLarge) }
            if (uiState.healthLogs.isEmpty()) {
                item {
                    EmptyState(
                        title = tr(TextKey.NO_LOGS),
                        message = tr(TextKey.NO_LOGS_HELP)
                    )
                }
            } else {
                items(uiState.healthLogs.take(20), key = { it.id }) { log ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("${log.hiveName} • ${log.logType.lowercase()}", style = MaterialTheme.typography.titleMedium)
                            Text("${log.status} • ${log.metricValue} ${log.metricUnit}", style = MaterialTheme.typography.bodyMedium)
                            if (log.notes.isNotBlank()) {
                                Text(log.notes, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

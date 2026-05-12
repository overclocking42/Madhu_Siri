package com.example.madhu_siri.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.madhu_siri.data.model.AppThemePreference
import com.example.madhu_siri.data.model.HealthLog
import com.example.madhu_siri.data.model.HealthLogType
import com.example.madhu_siri.data.model.Hive
import com.example.madhu_siri.data.model.NotificationAlert
import com.example.madhu_siri.data.model.SprayAlertDraft
import com.example.madhu_siri.data.model.SprayEvent
import com.example.madhu_siri.data.model.User
import com.example.madhu_siri.data.repository.AppSettingsRepository
import com.example.madhu_siri.data.repository.FirebaseRepository
import com.example.madhu_siri.ui.localization.AppLanguage
import com.example.madhu_siri.utils.HaversineUtil
import com.example.madhu_siri.utils.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(
    private val repository: FirebaseRepository,
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    val hives: StateFlow<List<Hive>> = repository.observeHives()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sprayEvents: StateFlow<List<SprayEvent>> = repository.observeSprayEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(MainUiState(isLoading = true))
    val uiState: StateFlow<MainUiState> = _uiState

    private val _spraySubmissionState = MutableStateFlow(ActionState())
    val spraySubmissionState: StateFlow<ActionState> = _spraySubmissionState

    private val _hiveActionState = MutableStateFlow(ActionState())
    val hiveActionState: StateFlow<ActionState> = _hiveActionState

    private val _healthLogState = MutableStateFlow(ActionState())
    val healthLogState: StateFlow<ActionState> = _healthLogState

    private val _profileState = MutableStateFlow(ActionState())
    val profileState: StateFlow<ActionState> = _profileState

    private var sessionObservationJob: Job? = null
    private var notificationObservationJob: Job? = null
    private val deliveredNotificationIds = mutableSetOf<String>()

    fun onSessionChanged(userId: String?) {
        sessionObservationJob?.cancel()
        deliveredNotificationIds.clear()

        if (userId == null) {
            _uiState.value = MainUiState(isLoading = false)
            return
        }

        sessionObservationJob = viewModelScope.launch {
            combine(
                repository.observeUser(userId),
                repository.observeHives(),
                repository.observeSprayEvents(),
                repository.observeHealthLogs(userId),
                repository.observeNotifications(userId)
            ) { user, allHives, allSprayEvents, healthLogs, notifications ->
                val myHives = allHives.filter { it.ownerId == userId }
                val visibleSprays = allSprayEvents.filter { it.isVisibleForSafety() }
                val nearbyAlerts = visibleSprays.filter { event ->
                    myHives.any { hive ->
                        HaversineUtil.calculateDistance(hive.lat, hive.lng, event.lat, event.lng) <= event.radiusKm
                    }
                }
                val lastFarmerSpray = allSprayEvents
                    .filter { it.farmerId == userId }
                    .maxByOrNull { it.createdAt.toDate().time }
                val nearbyHiveCountForFarmer = lastFarmerSpray?.let { event ->
                    allHives.count { hive ->
                        HaversineUtil.calculateDistance(hive.lat, hive.lng, event.lat, event.lng) <= 2.0
                    }
                } ?: allHives.size
                val nearbyVisibleSprayCountForFarmer = lastFarmerSpray?.let { event ->
                    visibleSprays.count { spray ->
                        HaversineUtil.calculateDistance(spray.lat, spray.lng, event.lat, event.lng) <= 2.0
                    }
                } ?: visibleSprays.size
                val hivesInAlertRadius = myHives.count { hive ->
                    nearbyAlerts.any { event ->
                        HaversineUtil.calculateDistance(hive.lat, hive.lng, event.lat, event.lng) <= event.radiusKm
                    }
                }
                MainUiState(
                    isLoading = false,
                    currentUser = user,
                    myHives = myHives,
                    allHives = allHives,
                    sprayHistory = allSprayEvents.filter { it.farmerId == userId },
                    activeSprayEvents = visibleSprays,
                    nearbySprayAlerts = nearbyAlerts,
                    notifications = notifications,
                    healthLogs = healthLogs,
                    lastFarmerReferenceSpray = lastFarmerSpray,
                    farmerNearbyHiveCount = nearbyHiveCountForFarmer,
                    farmerNearbyVisibleSprayCount = nearbyVisibleSprayCountForFarmer,
                    beekeeperHivesInAlertRadius = hivesInAlertRadius
                )
            }.collect { _uiState.value = it }
        }
    }

    fun startNotificationObserver(context: Context, role: String) {
        if (role != "BEEKEEPER") return
        notificationObservationJob?.cancel()

        notificationObservationJob = viewModelScope.launch {
            uiState.collectLatest { state ->
                state.notifications.forEach { notification ->
                    if (deliveredNotificationIds.add(notification.id)) {
                        NotificationHelper.showNotification(
                            context = context,
                            title = notification.title,
                            message = notification.body
                        )
                    }
                }
            }
        }
    }

    fun addHive(
        name: String,
        lat: Double,
        lng: Double,
        notes: String,
        activeStartTime: String,
        activeEndTime: String
    ) {
        viewModelScope.launch {
            _hiveActionState.value = ActionState(isLoading = true)
            runCatching { repository.addHive(name, lat, lng, notes, activeStartTime, activeEndTime) }
                .onSuccess {
                    _hiveActionState.value = ActionState(successMessage = "Hive saved")
                }
                .onFailure {
                    _hiveActionState.value = ActionState(errorMessage = it.message ?: "Could not save hive")
                }
        }
    }

    fun updateHive(
        hiveId: String,
        name: String,
        lat: Double,
        lng: Double,
        notes: String,
        activeStartTime: String,
        activeEndTime: String
    ) {
        viewModelScope.launch {
            _hiveActionState.value = ActionState(isLoading = true)
            runCatching { repository.updateHive(hiveId, name, lat, lng, notes, activeStartTime, activeEndTime) }
                .onSuccess {
                    _hiveActionState.value = ActionState(successMessage = "Hive updated")
                }
                .onFailure {
                    _hiveActionState.value = ActionState(errorMessage = it.message ?: "Could not update hive")
                }
        }
    }

    fun deleteHive(hiveId: String) {
        viewModelScope.launch {
            _hiveActionState.value = ActionState(isLoading = true)
            runCatching { repository.deleteHive(hiveId) }
                .onSuccess {
                    _hiveActionState.value = ActionState(successMessage = "Hive removed")
                }
                .onFailure {
                    _hiveActionState.value = ActionState(errorMessage = it.message ?: "Could not remove hive")
                }
        }
    }

    fun submitSprayAlert(draft: SprayAlertDraft) {
        viewModelScope.launch {
            _spraySubmissionState.value = ActionState(isLoading = true)
            runCatching { repository.createSprayAlert(draft) }
                .onSuccess { result ->
                    _spraySubmissionState.value = when {
                        result.error != null -> {
                            val suffix = buildString {
                                if (result.blockedByActiveBees && result.nextSafeStartMillis != null) {
                                    append(" Suggested safe start: ")
                                    append(SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(result.nextSafeStartMillis)))
                                    append(".")
                                }
                                if (result.conflictingHiveTimings.isNotEmpty()) {
                                    append(" ")
                                    append(result.conflictingHiveTimings.joinToString(" | "))
                                }
                            }
                            ActionState(
                                errorMessage = result.error + suffix,
                                suggestedSafeTimes = result.suggestedSafeTimes
                            )
                        }
                        else -> ActionState(
                            successMessage = "Spray alert created for ${result.nearbyHiveCount} nearby hives and sent to ${result.notifiedBeekeeperCount} beekeeper accounts."
                        )
                    }
                }
                .onFailure {
                    _spraySubmissionState.value = ActionState(errorMessage = it.message ?: "Could not submit spray alert")
                }
        }
    }

    fun addHealthLog(
        hive: Hive,
        type: HealthLogType,
        status: String,
        metricValue: Double,
        notes: String
    ) {
        viewModelScope.launch {
            _healthLogState.value = ActionState(isLoading = true)
            runCatching {
                repository.addHealthLog(
                    HealthLog(
                        hiveId = hive.id,
                        hiveName = hive.name,
                        logType = type.name,
                        status = status,
                        metricValue = metricValue,
                        metricUnit = if (type == HealthLogType.HONEY) "kg" else "score",
                        notes = notes
                    )
                )
            }.onSuccess {
                _healthLogState.value = ActionState(successMessage = "Tracker updated")
            }.onFailure {
                _healthLogState.value = ActionState(errorMessage = it.message ?: "Could not save tracker data")
            }
        }
    }

    fun clearSpraySubmissionMessage() {
        _spraySubmissionState.value = ActionState()
    }

    fun clearHiveActionMessage() {
        _hiveActionState.value = ActionState()
    }

    fun clearHealthLogMessage() {
        _healthLogState.value = ActionState()
    }

    fun updateProfileSettings(
        fullName: String,
        phoneNumber: String,
        preferredLanguage: AppLanguage,
        themePreference: AppThemePreference
    ) {
        viewModelScope.launch {
            _profileState.value = ActionState(isLoading = true)
            runCatching {
                repository.updateUserProfile(
                    fullName = fullName.trim(),
                    phoneNumber = phoneNumber.trim(),
                    preferredLanguage = preferredLanguage.tag,
                    themePreference = themePreference.name
                )
                settingsRepository.setLanguage(preferredLanguage)
                settingsRepository.setThemePreference(themePreference)
            }.onSuccess {
                _profileState.value = ActionState(successMessage = "Profile settings updated")
            }.onFailure {
                _profileState.value = ActionState(errorMessage = it.message ?: "Could not update profile settings")
            }
        }
    }

    fun clearProfileMessage() {
        _profileState.value = ActionState()
    }

    fun clearOldNotifications() {
        viewModelScope.launch {
            _profileState.value = ActionState(isLoading = true)
            runCatching { repository.clearOldNotifications() }
                .onSuccess { _profileState.value = ActionState(successMessage = "Older notifications were cleared.") }
                .onFailure { _profileState.value = ActionState(errorMessage = it.message ?: "Could not clear old notifications") }
        }
    }

    fun clearOldSprayHistory() {
        viewModelScope.launch {
            _profileState.value = ActionState(isLoading = true)
            runCatching { repository.clearOldSprayHistory() }
                .onSuccess { _profileState.value = ActionState(successMessage = "Older spray history was cleared.") }
                .onFailure { _profileState.value = ActionState(errorMessage = it.message ?: "Could not clear old history") }
        }
    }

    fun getCurrentUserId(): String? = repository.getCurrentUserId()
}

data class MainUiState(
    val isLoading: Boolean = false,
    val currentUser: User? = null,
    val myHives: List<Hive> = emptyList(),
    val allHives: List<Hive> = emptyList(),
    val sprayHistory: List<SprayEvent> = emptyList(),
    val activeSprayEvents: List<SprayEvent> = emptyList(),
    val nearbySprayAlerts: List<SprayEvent> = emptyList(),
    val notifications: List<NotificationAlert> = emptyList(),
    val healthLogs: List<HealthLog> = emptyList(),
    val lastFarmerReferenceSpray: SprayEvent? = null,
    val farmerNearbyHiveCount: Int = 0,
    val farmerNearbyVisibleSprayCount: Int = 0,
    val beekeeperHivesInAlertRadius: Int = 0
)

data class ActionState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val suggestedSafeTimes: List<Long> = emptyList()
)

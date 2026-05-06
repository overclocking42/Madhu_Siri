package com.example.madhu_siri.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.madhu_siri.data.model.Hive
import com.example.madhu_siri.data.model.SprayEvent
import com.example.madhu_siri.data.repository.FirebaseRepository
import com.example.madhu_siri.utils.HaversineUtil
import com.example.madhu_siri.utils.NotificationHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: FirebaseRepository) : ViewModel() {

    val hives: StateFlow<List<Hive>> = repository.getHives()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sprayEvents: StateFlow<List<SprayEvent>> = repository.getSprayEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var lastNotifiedEventId: String? = null

    fun startNotificationObserver(context: Context, role: String) {
        if (role != "BEEKEEPER") return
        
        viewModelScope.launch {
            sprayEvents.collectLatest { events ->
                val latestEvent = events.firstOrNull() ?: return@collectLatest
                if (latestEvent.id == lastNotifiedEventId) return@collectLatest
                
                // Check if any of the beekeeper's hives are within 2km
                val myHives = hives.value.filter { it.ownerId == repository.getCurrentUserId() }
                val isNearby = myHives.any { hive ->
                    HaversineUtil.calculateDistance(hive.lat, hive.lng, latestEvent.lat, latestEvent.lng) < 2.0
                }
                
                if (isNearby) {
                    NotificationHelper.showNotification(
                        context,
                        "Spray Alert!",
                        "A farmer is spraying pesticides within 2km of your hive."
                    )
                    lastNotifiedEventId = latestEvent.id
                }
            }
        }
    }

    fun addHive(lat: Double, lng: Double) {
        viewModelScope.launch {
            repository.addHive(lat, lng)
        }
    }

    fun deleteHive(hiveId: String) {
        viewModelScope.launch {
            repository.deleteHive(hiveId)
        }
    }

    fun sendSprayAlert(lat: Double, lng: Double) {
        viewModelScope.launch {
            repository.sendSprayAlert(lat, lng)
        }
    }
    
    fun getCurrentUserId() = repository.getCurrentUserId()
}

package com.example.madhu_siri

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.madhu_siri.data.repository.AppSettingsRepository
import com.example.madhu_siri.data.repository.FirebaseRepository
import com.example.madhu_siri.viewmodel.AuthViewModel
import com.example.madhu_siri.viewmodel.MainViewModel

class AppViewModelFactory(
    private val repository: FirebaseRepository,
    private val settingsRepository: AppSettingsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> AuthViewModel(repository, settingsRepository) as T
            modelClass.isAssignableFrom(MainViewModel::class.java) -> MainViewModel(repository, settingsRepository) as T
            else -> error("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

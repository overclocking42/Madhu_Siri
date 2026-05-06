package com.example.madhu_siri.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.madhu_siri.data.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: FirebaseRepository) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole

    init {
        checkUserStatus()
    }

    private fun checkUserStatus() {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                val role = repository.getUserRole()
                _userRole.value = role
                _authState.value = if (role == null) AuthState.AuthenticatedNoRole else AuthState.Authenticated(role)
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    fun signIn(email: String, pass: String) {
        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { checkUserStatus() }
            .addOnFailureListener { _authState.value = AuthState.Error(it.message ?: "Login Failed") }
    }

    fun signUp(email: String, pass: String) {
        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { checkUserStatus() }
            .addOnFailureListener { _authState.value = AuthState.Error(it.message ?: "Signup Failed") }
    }

    fun setRole(role: String) {
        viewModelScope.launch {
            repository.saveUserRole(role)
            _userRole.value = role
            _authState.value = AuthState.Authenticated(role)
        }
    }

    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
        _userRole.value = null
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    object AuthenticatedNoRole : AuthState()
    data class Authenticated(val role: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

package com.example.madhu_siri.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.madhu_siri.data.model.AppThemePreference
import com.example.madhu_siri.data.repository.AppSettingsRepository
import com.example.madhu_siri.data.repository.FirebaseRepository
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.example.madhu_siri.ui.components.GoogleAccountSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: FirebaseRepository,
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole

    private val _googleSignInLoading = MutableStateFlow(false)
    val googleSignInLoading: StateFlow<Boolean> = _googleSignInLoading

    private val _googleSignInError = MutableStateFlow<String?>(null)
    val googleSignInError: StateFlow<String?> = _googleSignInError

    private val _googleRegistrationCandidate = MutableStateFlow<GoogleAccountSelection?>(null)
    val googleRegistrationCandidate: StateFlow<GoogleAccountSelection?> = _googleRegistrationCandidate

    init {
        checkUserStatus()
    }

    fun refreshAuthState() {
        checkUserStatus()
    }

    private fun checkUserStatus() {
        viewModelScope.launch {
            val userId = repository.getCurrentUserId()
            if (userId != null) {
                val role = repository.getUserRole()
                if (role == null) {
                    FirebaseAuth.getInstance().signOut()
                    _userRole.value = null
                    _authState.value = AuthState.Unauthenticated
                } else {
                    repository.getCurrentUserProfile()?.let { user ->
                        settingsRepository.syncFromProfile(user.preferredLanguage, user.themePreference)
                    }
                    _userRole.value = role
                    _authState.value = AuthState.Authenticated(role)
                }
                runCatching { repository.syncCurrentUserFcmToken() }
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            runCatching {
                repository.signIn(email.trim(), password)
                repository.syncCurrentUserFcmToken()
            }.onSuccess {
                val actualRole = repository.getUserRole()
                repository.getCurrentUserProfile()?.let { user ->
                    settingsRepository.syncFromProfile(user.preferredLanguage, user.themePreference)
                }
                if (actualRole == null) {
                    FirebaseAuth.getInstance().signOut()
                    _authState.value = AuthState.Error("This account has no role yet. Please create the account first.")
                } else {
                    _userRole.value = actualRole
                    _authState.value = AuthState.Authenticated(actualRole)
                }
            }.onFailure {
                _authState.value = AuthState.Error(userFacingError(it, "Login failed. Please try again."))
            }
        }
    }

    fun signInWithGoogle(credential: AuthCredential) {
        viewModelScope.launch {
            _googleSignInLoading.value = true
            _googleSignInError.value = null
            _authState.value = AuthState.Loading
            runCatching {
                repository.signInWithGoogle(credential)
                val user = repository.getCurrentUserProfile()
                    ?: error("No account exists for this Google login yet. Please create the account first.")
                if (user.role.isBlank()) {
                    FirebaseAuth.getInstance().signOut()
                    error("This Google account has no role yet. Please finish account creation first.")
                }
                repository.syncCurrentUserFcmToken()
                user
            }.onSuccess { user ->
                settingsRepository.syncFromProfile(user.preferredLanguage, user.themePreference)
                _userRole.value = user.role
                _authState.value = AuthState.Authenticated(user.role)
            }.onFailure {
                val message = if (it is FirebaseAuthUserCollisionException) {
                    "This email already has an email account. Sign in with email and password first."
                } else {
                    userFacingError(it, "Google sign-in failed. Try again or use email and password.")
                }
                _googleSignInError.value = message
                _authState.value = AuthState.Error(message)
            }
            _googleSignInLoading.value = false
        }
    }

    fun completeGoogleRegistration(
        idToken: String,
        fullName: String,
        role: String,
        phoneNumber: String,
        preferredLanguage: String
    ) {
        viewModelScope.launch {
            _googleSignInLoading.value = true
            _googleSignInError.value = null
            _authState.value = AuthState.Loading
            runCatching {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                repository.signInWithGoogle(credential)
                val user = repository.upsertGoogleUserProfile(
                    selectedRole = role,
                    preferredLanguage = preferredLanguage,
                    fullNameOverride = fullName.trim(),
                    phoneNumberOverride = phoneNumber.trim()
                )
                repository.syncCurrentUserFcmToken()
                user
            }.onSuccess { user ->
                settingsRepository.syncFromProfile(user.preferredLanguage, user.themePreference)
                _userRole.value = user.role
                _authState.value = AuthState.Authenticated(user.role)
            }.onFailure {
                val message = if (it is FirebaseAuthUserCollisionException) {
                    "This email already has an email account. Sign in with email and password first."
                } else {
                    userFacingError(it, "Google account setup failed. Try again or continue with email.")
                }
                _googleSignInError.value = message
                _authState.value = AuthState.Error(message)
            }
            _googleSignInLoading.value = false
        }
    }

    fun signUp(
        fullName: String,
        email: String,
        password: String,
        role: String,
        phoneNumber: String,
        preferredLanguage: String
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            runCatching {
                repository.register(fullName.trim(), email.trim(), password, role, phoneNumber.trim(), preferredLanguage)
                repository.syncCurrentUserFcmToken()
            }.onSuccess {
                settingsRepository.setLanguage(com.example.madhu_siri.ui.localization.AppLanguage.fromTag(preferredLanguage))
                settingsRepository.setThemePreference(AppThemePreference.SYSTEM)
                _userRole.value = role
                _authState.value = AuthState.Authenticated(role)
            }.onFailure {
                _authState.value = AuthState.Error(userFacingError(it, "Sign up failed. Please try again."))
            }
        }
    }

    fun setRole(role: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading // Set loading state while role is being set
            runCatching {
                repository.saveUserRole(role)
                repository.syncCurrentUserFcmToken()
            }.onSuccess {
                _userRole.value = role
                _authState.value = AuthState.Authenticated(role)
            }.onFailure {
                _authState.value = AuthState.Error(userFacingError(it, "Could not save role right now."))
            }
        }
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
        _userRole.value = null
        _authState.value = AuthState.Unauthenticated
    }

    fun clearAuthError() {
        (_authState.value as? AuthState.Error)?.let { _authState.value = AuthState.Idle }
        _googleSignInError.value = null
    }

    fun setGoogleSignInError(message: String?) {
        _googleSignInError.value = message
    }

    fun prepareGoogleRegistration(selection: GoogleAccountSelection) {
        viewModelScope.launch {
            _googleSignInLoading.value = true
            _googleSignInError.value = null
            runCatching {
                repository.findUserByEmail(selection.email)
            }.onSuccess { existing ->
                if (existing != null) {
                    _googleRegistrationCandidate.value = null
                    _googleSignInError.value =
                        "An account with ${selection.email} already exists. Please log in instead of creating a new account."
                } else {
                    _googleRegistrationCandidate.value = selection
                }
            }.onFailure {
                _googleRegistrationCandidate.value = null
                _googleSignInError.value = userFacingError(it, "Could not verify this Google account right now.")
            }
            _googleSignInLoading.value = false
        }
    }

    fun clearGoogleRegistrationCandidate() {
        _googleRegistrationCandidate.value = null
    }

    private fun userFacingError(throwable: Throwable, fallback: String): String {
        val message = throwable.message.orEmpty()
        return when {
            "Could not deserialize object" in message ||
                "does not define a no-argument constructor" in message ||
                "If you are using ProGuard" in message -> {
                "We could not read your account data on this build. Please update to the latest app build and try again."
            }
            "Given final block not properly padded" in message ||
                "keystore password was incorrect" in message -> {
                "This build was signed with invalid release settings. Please rebuild the app and reinstall it."
            }
            else -> message.ifBlank { fallback }
        }
    }
}

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data object Unauthenticated : AuthState()
    data object AuthenticatedNoRole : AuthState()
    data class Authenticated(val role: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

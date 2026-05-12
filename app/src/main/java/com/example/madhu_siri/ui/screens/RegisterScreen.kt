package com.example.madhu_siri.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.madhu_siri.data.repository.AppSettingsRepository
import com.example.madhu_siri.data.model.UserRole
import com.example.madhu_siri.ui.components.BrandHeader
import com.example.madhu_siri.ui.components.GoogleAccountSelection
import com.example.madhu_siri.ui.components.GoogleSignInButton
import com.example.madhu_siri.ui.components.InlineMessage
import com.example.madhu_siri.ui.components.LoadingBlock
import com.example.madhu_siri.ui.components.RoleSelector
import com.example.madhu_siri.ui.components.SocialDivider
import com.example.madhu_siri.ui.localization.AppLanguage
import com.example.madhu_siri.ui.localization.LocalAppText
import com.example.madhu_siri.ui.localization.TextKey
import com.example.madhu_siri.viewmodel.AuthState
import com.example.madhu_siri.viewmodel.AuthViewModel

private enum class RegisterStep {
    METHOD,
    DETAILS
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    settingsRepository: AppSettingsRepository,
    onBackToLogin: () -> Unit
) {
    val tr = LocalAppText.current
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var selectedRole by rememberSaveable { mutableStateOf(UserRole.FARMER) }
    var selectedLanguage by rememberSaveable { mutableStateOf(settingsRepository.language.value) }
    var registerStep by rememberSaveable { mutableStateOf(RegisterStep.METHOD) }
    val authState by viewModel.authState.collectAsState()
    val googleSignInError by viewModel.googleSignInError.collectAsState()
    val googleRegistrationCandidate by viewModel.googleRegistrationCandidate.collectAsState()
    val pendingGoogleAccount = googleRegistrationCandidate
    val usingGoogle = pendingGoogleAccount != null
    val stepOneEmailError = remember(email) {
        when {
            email.isBlank() -> "Email is required"
            !email.contains("@") -> "Enter a valid email"
            else -> null
        }
    }

    val formError = remember(fullName, email, phoneNumber, password, confirmPassword, usingGoogle) {
        when {
            fullName.isBlank() -> "Name is required"
            !email.contains("@") -> "Enter a valid email"
            phoneNumber.filter(Char::isDigit).length < 10 -> "Enter a valid contact number"
            !usingGoogle && password.length < 6 -> "Password must be at least 6 characters"
            !usingGoogle && password != confirmPassword -> "Passwords do not match"
            else -> null
        }
    }

    LaunchedEffect(googleRegistrationCandidate) {
        val candidate = googleRegistrationCandidate ?: return@LaunchedEffect
        fullName = candidate.displayName.ifBlank { fullName }
        email = candidate.email.ifBlank { email }
        registerStep = RegisterStep.DETAILS
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        BrandHeader(
            title = tr(TextKey.REGISTER_TITLE),
            subtitle = tr(TextKey.REGISTER_SUBTITLE)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (registerStep == RegisterStep.METHOD) {
                    Text(
                        "Choose Google or email first, then continue to your account details.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    GoogleSignInButton(
                        onAccountSelected = { selection ->
                            viewModel.clearAuthError()
                            viewModel.prepareGoogleRegistration(selection)
                        },
                        onError = viewModel::setGoogleSignInError,
                        forceAccountPicker = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    InlineMessage(
                        successMessage = null,
                        errorMessage = googleSignInError,
                        onDismissed = viewModel::clearAuthError,
                        autoDismissMillis = 4000L
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    SocialDivider()
                    Spacer(modifier = Modifier.height(18.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(tr(TextKey.EMAIL)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = email.isNotBlank() && stepOneEmailError != null,
                        supportingText = {
                            if (email.isNotBlank() && stepOneEmailError != null) {
                                Text(stepOneEmailError)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = {
                            if (stepOneEmailError == null) {
                                viewModel.clearGoogleRegistrationCandidate()
                                viewModel.clearAuthError()
                                registerStep = RegisterStep.DETAILS
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = stepOneEmailError == null
                    ) {
                        Text("Continue with email")
                    }
                } else {
                    Text(
                        if (usingGoogle) {
                            "Your Google account filled the basics. Add the remaining details to finish the account."
                        } else {
                            "Finish your details below, then create your account."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    RoleSelector(selectedRole = selectedRole, onRoleSelected = { selectedRole = it })
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(tr(TextKey.FULL_NAME)) },
                        singleLine = true,
                        supportingText = {
                            if (usingGoogle) {
                                Text("Name and email were filled from your Google account. You can still edit them.")
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(tr(TextKey.EMAIL)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(tr(TextKey.PHONE_NUMBER)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    if (!usingGoogle) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(tr(TextKey.PASSWORD)) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(tr(TextKey.CONFIRM_PASSWORD)) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        tr(TextKey.APP_LANGUAGE),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppLanguage.entries.forEach { language ->
                            androidx.compose.material3.FilterChip(
                                selected = selectedLanguage == language,
                                onClick = { selectedLanguage = language },
                                label = { Text(language.nativeLabel) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))

                    if (authState is AuthState.Loading) {
                        LoadingBlock("Creating account…")
                    } else {
                        Button(
                            onClick = {
                                if (usingGoogle) {
                                    viewModel.completeGoogleRegistration(
                                        idToken = pendingGoogleAccount!!.idToken,
                                        fullName = fullName,
                                        role = selectedRole.name,
                                        phoneNumber = phoneNumber,
                                        preferredLanguage = selectedLanguage.tag
                                    )
                                } else {
                                    viewModel.signUp(
                                        fullName = fullName,
                                        email = email,
                                        password = password,
                                        role = selectedRole.name,
                                        phoneNumber = phoneNumber,
                                        preferredLanguage = selectedLanguage.tag
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = formError == null
                        ) {
                            Text(
                                if (usingGoogle) {
                                    tr(
                                        TextKey.CONTINUE_AS,
                                        if (selectedRole == UserRole.FARMER) tr(TextKey.ROLE_FARMER) else tr(TextKey.ROLE_BEEKEEPER)
                                    )
                                } else {
                                    tr(
                                        TextKey.CREATE_ACCOUNT_AS,
                                        if (selectedRole == UserRole.FARMER) tr(TextKey.ROLE_FARMER) else tr(TextKey.ROLE_BEEKEEPER)
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            viewModel.clearGoogleRegistrationCandidate()
                            registerStep = RegisterStep.METHOD
                            viewModel.clearAuthError()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Back")
                    }
                }

                if (registerStep == RegisterStep.METHOD) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onBackToLogin, modifier = Modifier.fillMaxWidth()) {
                        Text(tr(TextKey.BACK_TO_LOGIN))
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onBackToLogin, modifier = Modifier.fillMaxWidth()) {
                        Text(tr(TextKey.BACK_TO_LOGIN))
                    }
                }
                InlineMessage(
                    successMessage = null,
                    errorMessage = if (registerStep == RegisterStep.DETAILS) {
                        formError ?: (authState as? AuthState.Error)?.message
                    } else {
                        null
                    },
                    onDismissed = if (registerStep == RegisterStep.DETAILS && formError == null) viewModel::clearAuthError else null,
                    autoDismissMillis = 4000L
                )
            }
        }
    }
}

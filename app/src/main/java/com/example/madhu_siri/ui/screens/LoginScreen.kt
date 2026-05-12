package com.example.madhu_siri.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.madhu_siri.ui.components.BrandHeader
import com.example.madhu_siri.ui.components.GoogleAccountSelection
import com.example.madhu_siri.ui.components.GoogleSignInButton
import com.example.madhu_siri.ui.components.InlineMessage
import com.example.madhu_siri.ui.components.LoadingBlock
import com.example.madhu_siri.ui.components.SocialDivider
import com.example.madhu_siri.ui.localization.LocalAppText
import com.example.madhu_siri.ui.localization.TextKey
import com.example.madhu_siri.viewmodel.AuthState
import com.example.madhu_siri.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onOpenRegister: () -> Unit
) {
    val tr = LocalAppText.current
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()
    val googleSignInError by viewModel.googleSignInError.collectAsState()

    val emailError = remember(email) {
        when {
            email.isBlank() -> "Email is required"
            !email.contains("@") -> "Enter a valid email"
            else -> null
        }
    }
    val passwordError = remember(password) {
        when {
            password.isBlank() -> "Password is required"
            password.length < 6 -> "Minimum 6 characters"
            else -> null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        BrandHeader(
            title = tr(TextKey.APP_NAME),
            subtitle = tr(TextKey.LOGIN_SUBTITLE)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(tr(TextKey.LOGIN_TITLE), style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Continue with Google or sign in with your email and password.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
                GoogleSignInButton(
                    onAccountSelected = { selection: GoogleAccountSelection ->
                        viewModel.clearAuthError()
                        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(selection.idToken, null)
                        viewModel.signInWithGoogle(credential = credential)
                    },
                    onError = viewModel::setGoogleSignInError
                )
                Spacer(modifier = Modifier.height(10.dp))
                InlineMessage(
                    successMessage = null,
                    errorMessage = googleSignInError
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
                    isError = email.isNotBlank() && emailError != null,
                    supportingText = {
                        if (email.isNotBlank() && emailError != null) Text(emailError)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr(TextKey.PASSWORD)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = password.isNotBlank() && passwordError != null,
                    supportingText = {
                        if (password.isNotBlank() && passwordError != null) Text(passwordError)
                    }
                )
                Spacer(modifier = Modifier.height(18.dp))

                when (authState) {
                    is AuthState.Loading -> LoadingBlock("Signing in…")
                    else -> {
                        Button(
                            onClick = { viewModel.signIn(email, password) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = emailError == null && passwordError == null
                        ) {
                            Text("Sign in")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onOpenRegister, modifier = Modifier.fillMaxWidth()) {
                    Text(tr(TextKey.CREATE_NEW_ACCOUNT))
                }
                Spacer(modifier = Modifier.height(8.dp))
                InlineMessage(successMessage = null, errorMessage = (authState as? AuthState.Error)?.message)
            }
        }
    }
}

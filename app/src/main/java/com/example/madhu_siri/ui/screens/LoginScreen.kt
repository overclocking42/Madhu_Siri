package com.example.madhu_siri.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.madhu_siri.viewmodel.AuthState
import com.example.madhu_siri.viewmodel.AuthViewModel

@Composable
fun LoginScreen(viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val state by viewModel.authState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Madhu-Siri", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password, 
            onValueChange = { password = it }, 
            label = { Text("Password") }, 
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        if (state is AuthState.Loading) {
            CircularProgressIndicator()
        } else {
            Button(onClick = { viewModel.signIn(email, password) }, modifier = Modifier.fillMaxWidth()) {
                Text("Login")
            }
            TextButton(onClick = { viewModel.signUp(email, password) }) {
                Text("Register")
            }
        }

        if (state is AuthState.Error) {
            Text((state as AuthState.Error).message, color = MaterialTheme.colorScheme.error)
        }
    }
}

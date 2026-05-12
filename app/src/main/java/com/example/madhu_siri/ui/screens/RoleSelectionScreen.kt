package com.example.madhu_siri.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.madhu_siri.viewmodel.AuthViewModel
import com.example.madhu_siri.viewmodel.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionScreen(authViewModel: AuthViewModel, onRoleSelected: (String) -> Unit) {
    var selectedRole by remember { mutableStateOf("") }
    val authState by authViewModel.authState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Select Your Role",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Role selection options (e.g., Beekeeper, Farmer)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            OutlinedButton(onClick = { selectedRole = "BEEKEEPER" })
            {
                Text("Beekeeper")
            }
            OutlinedButton(onClick = { selectedRole = "FARMER" })
            {
                Text("Farmer")
            }
        }

        Button(
            onClick = {
                if (selectedRole.isNotEmpty()) {
                    authViewModel.setRole(selectedRole)
                    onRoleSelected(selectedRole)
                }
            },
            enabled = selectedRole.isNotEmpty() && authState !is AuthState.Loading // Disable if no role is selected or if loading
        ) {
            Text("Continue")
        }
    }
}

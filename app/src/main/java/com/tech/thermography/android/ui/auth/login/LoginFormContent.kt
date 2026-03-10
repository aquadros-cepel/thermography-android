package com.tech.thermography.android.ui.auth.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.tech.thermography.android.R
import com.tech.thermography.android.ui.components.CompactUiWrapper

@Composable
fun LoginFormContent(
    uiState: LoginUiState,
    isTablet: Boolean,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onRememberChanged: (Boolean) -> Unit,
    onLoginClicked: () -> Unit,
    onNavigateToCreateAccount: () -> Unit
) {
    var username by remember { mutableStateOf(uiState.username) }
    var password by remember { mutableStateOf(uiState.password) }
    var remember by remember { mutableStateOf(uiState.rememberMe) }
    
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Image(
            painter = painterResource(id = R.drawable.logo_thermal_energy_vb),
            contentDescription = "Logo small",
            modifier = Modifier
                .size(140.dp)
                .padding(bottom = 24.dp)
        )

//        Text("Thermal Energy",
//            style = MaterialTheme.typography.headlineMedium,
//            fontWeight = FontWeight.Bold,
//            modifier = Modifier.padding(bottom = 12.dp))

        Text("Gestão de Inspeções Termográficas",
            style = MaterialTheme.typography.bodyLarge,
//            color = Color.DarkGray,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 48.dp))

        CompactUiWrapper {
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    onUsernameChanged(it)
                },
                label = { Text("Usuário") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )
        }

        Spacer(Modifier.height(12.dp))

        CompactUiWrapper {
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    onPasswordChanged(it)
                },
                label = { Text("Senha") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        focusManager.clearFocus()
                        onLoginClicked()
                    }
                )
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = remember, onCheckedChange = {
                    remember = it
                    onRememberChanged(it)
                })
                Text("Manter conectado",
                    style = MaterialTheme.typography.bodyMedium)
            }
            Text("Esqueceu sua senha?",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { }
            )
        }

        uiState.error?.let {
            Text(it, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = { 
                focusManager.clearFocus()
                onLoginClicked() 
            }, 
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.loading) { // Corrigido de isLoading para loading
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text("Entrar")
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("Criar conta",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.clickable { onNavigateToCreateAccount() })
            
        Spacer(Modifier.height(32.dp))
    }
}

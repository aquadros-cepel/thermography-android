package com.tech.thermography.android.ui.home

import android.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.tech.thermography.android.navigation.NavRoutes
import com.tech.thermography.android.navigation.NavBarItems

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController? = null,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val navBarItems = NavBarItems.items
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CenterAlignedTopAppBar(
            title = { Text("Thermal Energy") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Renderiza os botões em 2 linhas x 2 colunas
            for (row in 0 until 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (col in 0 until 2) {
                        val index = row * 2 + col
                        if (index < navBarItems.size) {
                            val (route, label, icon) = navBarItems[index]
                            Button(
                                onClick = { 
                                    if (label == "Logout") {
                                        showLogoutDialog = true
                                    } else {
                                        navController?.navigate(route) 
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(2f)
                                    .padding(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(icon, contentDescription = label, tint = Color.Black)
                                    Text(
                                        text = label,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Confirmar Logout") },
            text = { 
                Text("Após fazer o Logout você só conseguirá fazer Login estando on-line.\n\nSe estiver trabalhando em um local sem conexão de rede não será possível autenticar via Login e entrar na aplicação.\n\nTem certeza que deseja fazer Logout?",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout {
                            navController?.navigate(NavRoutes.LOGIN) {
                                popUpTo(NavRoutes.HOME) { inclusive = true }
                            }
                        }
                    }
                ) {
                    Text("Sim", color = MaterialTheme.colorScheme.error,
                        fontSize = 18.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Não",
                    fontSize = 18.sp)
                }
            }
        )
    }
}

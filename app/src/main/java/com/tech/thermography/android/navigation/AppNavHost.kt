package com.tech.thermography.android.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.LinkedCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tech.thermography.android.ui.auth.login.LoginScreen
import com.tech.thermography.android.ui.home.HomeScreen
import com.tech.thermography.android.ui.inspection_report.InspectionRecordsScreen
import com.tech.thermography.android.ui.sync.SyncScreen
import com.tech.thermography.android.ui.thermal_anomaly.ThermalAnomalyForm
import java.util.UUID

@Composable
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalMotionApi::class)
fun AppNavHost() {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Itens da barra de navegação
    val items = listOf(
        Triple(NavRoutes.HOME, "Home", Icons.Filled.Home),
        Triple(NavRoutes.INSPECTION_RECORDS, "Rotas", Icons.Outlined.Route),
        Triple(NavRoutes.THERMOGRAMS, "Termogramas", Icons.Outlined.LinkedCamera),
        Triple(NavRoutes.SETTINGS, "Configurações", Icons.Filled.Settings)
    )

    // Verifica se a tela atual deve mostrar a Bottom Bar
    val bottomBarRoutes = items.map { it.first }
    val showBottomBar = currentDestination?.route in bottomBarRoutes

    // Recuperação segura do Activity para o WindowSizeClass
    val context = LocalContext.current
    val activity = context.findActivity()
    
    // Define padrão se não encontrar activity (ex: preview)
    val windowSizeClass = if (activity != null) {
        calculateWindowSizeClass(activity)
    } else {
        null
    }
    
    val isTablet = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Expanded

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = if (isTablet) Modifier else Modifier.height(90.dp)
                ) {
                    items.forEach { (route, label, icon) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = if (isTablet) {
                                { Text(label) }
                            } else null,
                            alwaysShowLabel = isTablet,
                            selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.LOGIN,
            modifier = Modifier.padding(innerPadding)
        ) {

            composable(NavRoutes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = { 
                        navController.navigate(NavRoutes.THERMOGRAMS) {
                            popUpTo(NavRoutes.LOGIN) { inclusive = true }
                        }
                    },
                    onNavigateToCreateAccount = { /* TODO */ }
                )
            }

            composable("sync") {
                SyncScreen(onSyncComplete = { 
                    navController.navigate(NavRoutes.INSPECTION_RECORDS) {
                        popUpTo("sync") { inclusive = true }
                    }
                })
            }

            composable(NavRoutes.HOME) {
                HomeScreen()
            }

            composable(NavRoutes.INSPECTION_RECORDS) {
                InspectionRecordsScreen(onViewRouteClick = { id ->
                    navController.navigate("${NavRoutes.INSPECTION_RECORD_DETAIL}/$id")
                })
            }

            composable(NavRoutes.THERMOGRAMS) {
                ThermalAnomalyForm()
            }

            // Backward-compatible route (3 segments) -> thermographicId will be null
            composable("${NavRoutes.THERMAL_ANOMALY}/{plantId}/{equipmentId}/{inspectionRecordId}") { backStackEntry ->
                val plantIdArg = backStackEntry.arguments?.getString("plantId")
                val equipmentIdArg = backStackEntry.arguments?.getString("equipmentId")
                val inspectionRecordIdArg = backStackEntry.arguments?.getString("inspectionRecordId")
                fun parseUuidOrNull(s: String?): UUID? = try { if (s == null || s == "null") null else UUID.fromString(s) } catch (_: Exception) { null }
                val plantId = parseUuidOrNull(plantIdArg)
                val equipmentId = parseUuidOrNull(equipmentIdArg)
                val inspectionRecordId = parseUuidOrNull(inspectionRecordIdArg)
                ThermalAnomalyForm(plantId, equipmentId, inspectionRecordId, null, navController)
            }

            // Thermal Anomaly form with IDs as path segments: thermal_anomaly/{plantId}/{equipmentId}/{inspectionRecordId}/{thermographicId}
            composable("${NavRoutes.THERMAL_ANOMALY}/{plantId}/{equipmentId}/{inspectionRecordId}/{thermographicId}") { backStackEntry ->
                val plantIdArg = backStackEntry.arguments?.getString("plantId")
                val equipmentIdArg = backStackEntry.arguments?.getString("equipmentId")
                val inspectionRecordIdArg = backStackEntry.arguments?.getString("inspectionRecordId")
                val thermographicIdArg = backStackEntry.arguments?.getString("thermographicId")

                fun parseUuidOrNull(s: String?): UUID? = try { if (s == null || s == "null") null else UUID.fromString(s) } catch (_: Exception) { null }

                val plantId = parseUuidOrNull(plantIdArg)
                val equipmentId = parseUuidOrNull(equipmentIdArg)
                val inspectionRecordId = parseUuidOrNull(inspectionRecordIdArg)
                val thermographicId = parseUuidOrNull(thermographicIdArg)

                ThermalAnomalyForm(plantId, equipmentId, inspectionRecordId, thermographicId, navController)
            }

            // Query-style route to avoid path matching issues: thermal_anomaly?plantId=...&equipmentId=...&inspectionRecordId=...&thermographicId=...
            composable("${NavRoutes.THERMAL_ANOMALY}?plantId={plantId}&equipmentId={equipmentId}&inspectionRecordId={inspectionRecordId}&thermographicId={thermographicId}") { backStackEntry ->
                val plantIdArg = backStackEntry.arguments?.getString("plantId")
                val equipmentIdArg = backStackEntry.arguments?.getString("equipmentId")
                val inspectionRecordIdArg = backStackEntry.arguments?.getString("inspectionRecordId")
                val thermographicIdArg = backStackEntry.arguments?.getString("thermographicId")
                fun parseUuidOrNull(s: String?): UUID? = try { if (s == null || s == "null") null else UUID.fromString(s) } catch (_: Exception) { null }
                val plantId = parseUuidOrNull(plantIdArg)
                val equipmentId = parseUuidOrNull(equipmentIdArg)
                val inspectionRecordId = parseUuidOrNull(inspectionRecordIdArg)
                val thermographicId = parseUuidOrNull(thermographicIdArg)
                ThermalAnomalyForm(plantId, equipmentId, inspectionRecordId, thermographicId, navController)
            }

            composable(NavRoutes.SETTINGS) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tela de Configurações")
                }
            }

            // Inspection record detail route: inspection/{id} with optional expandEquipmentId query
            composable("${NavRoutes.INSPECTION_RECORD_DETAIL}/{id}?expandEquipmentId={expandEquipmentId}") { backStackEntry ->
                val idArg = backStackEntry.arguments?.getString("id")
                val expandArg = backStackEntry.arguments?.getString("expandEquipmentId")
                fun parseUuidOrNull(s: String?): UUID? = try { if (s == null || s == "null") null else UUID.fromString(s) } catch (_: Exception) { null }
                val uuid = parseUuidOrNull(idArg)
                val expandUuid = parseUuidOrNull(expandArg)
                if (uuid != null) {
                    com.tech.thermography.android.ui.inspection_report.InspectionRecordDetailScreen(recordId = uuid, navController = navController, expandToEquipmentId = expandUuid)
                } else {
                    // show fallback screen
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Registro inválido")
                    }
                }
            }
        }
    }
}

// Função de extensão auxiliar para encontrar a Activity a partir do Contexto
private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

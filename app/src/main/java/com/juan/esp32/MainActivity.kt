package com.juan.esp32

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.juan.esp32.service.BleForegroundService
import com.juan.esp32.ui.theme.ESP32Theme

class MainActivity : ComponentActivity() {

    // ✅ CORRECT PERMISSIONS LIST
    private val permissions = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
    }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ESP32Theme {
                Scaffold { innerPadding ->
                    PermissionScreen(
                        modifier = Modifier.padding(innerPadding),
                        permissions = permissions,
                        onPermissionsGranted = {
                            startBleService()
                        }
                    )
                }
            }
        }
    }

    private fun startBleService() {
        BleForegroundService.startService(this)
    }
}

@Composable
fun PermissionScreen(
    modifier: Modifier = Modifier,
    permissions: Array<String>,
    onPermissionsGranted: () -> Unit = {}
) {
    val context = LocalContext.current

    var permissionGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result: Map<String, Boolean> ->
        permissionGranted = result.values.all { it }
        if (permissionGranted) {
            onPermissionsGranted()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!permissionGranted) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Necesitamos permisos para usar Bluetooth y conectarnos a tu ESP32",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(onClick = {
                    permissionLauncher.launch(permissions)
                }) {
                    Text("Conceder permisos")
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "¡Permisos concedidos! 🎉",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Iniciando conexión con ESP32...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPermissionScreen() {
    ESP32Theme {
        PermissionScreen(permissions = arrayOf())
    }
}
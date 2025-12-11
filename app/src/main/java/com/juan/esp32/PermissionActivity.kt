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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.juan.esp32.ui.theme.ESP32Theme

class MainActivity : ComponentActivity() {

    private val permissions = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ESP32Theme {
                PermissionScreen(permissions)
            }
        }
    }
}

@Composable
fun PermissionScreen(permissions: Array<String>) {

    var granted by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result: Map<String, Boolean> ->
        granted = result.values.all { it }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {

        if (!granted) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Necesitamos permisos para usar Bluetooth con tu ESP32",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(onClick = { launcher.launch(permissions) }) {
                    Text("Conceder permisos")
                }
            }
        } else {
            Text(
                "¡Permisos concedidos! 🎉",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PermissionScreenPreview() {
    ESP32Theme {
        PermissionScreen(arrayOf())
    }
}

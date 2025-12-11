package com.juan.esp32.data.ble

object BleConstants {
    // ✅ MAC REAL del ESP32
    const val DEVICE_MAC_ADDRESS = "28:37:2F:72:E4:96"

    // ✅ UUID de la característica que envía datos
    const val CHARACTERISTIC_UUID = "00001234-0000-1000-8000-00805F9B34FB"

    // ✅ Este queda como placeholder por ahora (lo buscaremos automáticamente)
    const val SERVICE_UUID = "00000000-0000-0000-0000-000000000000" // Placeholder

    // ✅ Configuración estándar de Bluetooth (NO cambiar)
    const val CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805F9B34FB"

    // ✅ Intervalo de reconexión
    const val RECONNECT_INTERVAL_MS = 5000L
}
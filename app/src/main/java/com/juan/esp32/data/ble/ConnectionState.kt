package com.juan.esp32.data.ble

sealed class ConnectionState {
    object Disconnected : ConnectionState()      // 🔴
    object Connecting : ConnectionState()        // 🟡
    object Connected : ConnectionState()         // 🟢
    object Reconnecting : ConnectionState()      // 🟡 (parpadeante)

    fun toColor(): String {
        return when (this) {
            is Disconnected -> "#FF0000"  // Rojo
            is Connecting -> "#FFFF00"    // Amarillo
            is Connected -> "#00FF00"     // Verde
            is Reconnecting -> "#FFA500"  // Naranja
        }
    }
}
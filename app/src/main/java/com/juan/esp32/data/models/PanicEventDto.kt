package com.juan.esp32.domain.models

import java.util.*

data class PanicEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: PanicEventType = PanicEventType.MANUAL_TRIGGER,
    val sensorData: SensorData? = null,
    val location: Pair<Double, Double>? = null, // Lat, Long
    val resolved: Boolean = false
) {
    enum class PanicEventType {
        MANUAL_TRIGGER,    // Botón de pánico presionado
        AUTO_THRESHOLD,    // Límite de sensor superado automáticamente
        CONNECTION_LOST,   // Pérdida de conexión BLE
        LOW_BATTERY        // Batería baja del ESP32
    }

    fun toCsvLine(): String {
        return "$timestamp,${type.name},${sensorData?.ir ?: 0},${sensorData?.red ?: 0},${resolved}"
    }
}
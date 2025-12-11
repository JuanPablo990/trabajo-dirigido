package com.juan.esp32.data.models

import java.util.*

/**
 * DTO para eventos de pánico - Simple y directo
 */
data class PanicEventDto(
    // Identificador único
    val id: String = UUID.randomUUID().toString(),

    // Timestamp del evento
    val timestamp: Long = System.currentTimeMillis(),

    // Tipo de evento (MANUAL, AUTOMATICO, etc.)
    val eventType: String = "MANUAL",

    // Datos de sensor en el momento del pánico (opcional)
    val sensorData: SensorReadingDto? = null,

    // Información adicional
    val message: String = "Botón de pánico presionado",
    val location: String? = null,
    val resolved: Boolean = false
) {
    /**
     * Convierte a línea CSV para guardar
     */
    fun toCsvLine(): String {
        val sensorInfo = sensorData?.let {
            "${it.ir},${it.red},${it.obj.toFloat()}"
        } ?: ",,"

        return "$timestamp,$eventType,$sensorInfo,$message,${resolved}"
    }

    /**
     * Crea un evento de pánico manual (cuando presionas el botón)
     */
    companion object {
        fun createManualPanic(sensorData: SensorReadingDto? = null): PanicEventDto {
            return PanicEventDto(
                eventType = "MANUAL",
                sensorData = sensorData,
                message = "PANIC_BUTTON_PRESSED"
            )
        }

        /**
         * Cabecera del CSV para eventos de pánico
         */
        fun getCsvHeader(): String {
            return "timestamp,event_type,ir_value,red_value,temperature,message,resolved"
        }
    }
}
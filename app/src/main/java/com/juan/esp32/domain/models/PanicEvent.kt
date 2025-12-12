package com.juan.esp32.domain.models

import java.time.LocalDateTime
import java.util.UUID

/**
 * Modelo de dominio para un evento de pánico.
 * Representa un evento de emergencia en el dominio de negocio.
 */
data class PanicEvent(
    val id: UUID,
    val timestamp: LocalDateTime,
    val eventType: EventType,
    val severity: SeverityLevel,
    val sensorData: SensorReading?,
    val location: Location?,
    val userNotes: String?,
    val isResolved: Boolean,
    val resolutionTime: LocalDateTime?,
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * Determina si el evento es crítico.
     */
    val isCritical: Boolean
        get() = severity == SeverityLevel.CRITICAL

    /**
     * Determina si el evento es reciente (últimos 5 minutos).
     */
    fun isRecent(): Boolean {
        val now = LocalDateTime.now()
        val minutesAgo = now.minusMinutes(5)
        return timestamp.isAfter(minutesAgo)
    }

    /**
     * Marca el evento como resuelto.
     */
    fun resolve(notes: String? = null): PanicEvent {
        return copy(
            isResolved = true,
            resolutionTime = LocalDateTime.now(),
            userNotes = notes ?: userNotes
        )
    }

    /**
     * Valida que el evento tenga datos mínimos.
     */
    fun isValid(): Boolean {
        return when (eventType) {
            EventType.MANUAL -> true // Los manuales siempre son válidos
            EventType.AUTOMATIC -> sensorData != null // Los automáticos requieren datos
            EventType.SCHEDULED -> true
            EventType.TEST -> true
        }
    }

    companion object {
        /**
         * Crea un evento de pánico manual (cuando el usuario presiona el botón).
         */
        fun createManualPanic(
            sensorData: SensorReading? = null,
            location: Location? = null,
            notes: String? = null
        ): PanicEvent {
            return PanicEvent(
                id = UUID.randomUUID(),
                timestamp = LocalDateTime.now(),
                eventType = EventType.MANUAL,
                severity = SeverityLevel.HIGH,
                sensorData = sensorData,
                location = location,
                userNotes = notes,
                isResolved = false,
                resolutionTime = null
            )
        }

        /**
         * Crea un evento de pánico automático (detectado por el sistema).
         */
        fun createAutomaticPanic(
            sensorData: SensorReading,
            reason: String,
            location: Location? = null
        ): PanicEvent {
            return PanicEvent(
                id = UUID.randomUUID(),
                timestamp = LocalDateTime.now(),
                eventType = EventType.AUTOMATIC,
                severity = determineSeverity(sensorData),
                sensorData = sensorData,
                location = location,
                userNotes = "Detectado automáticamente: $reason",
                isResolved = false,
                resolutionTime = null
            )
        }

        /**
         * Determina la severidad basada en los datos del sensor.
         */
        private fun determineSeverity(sensorData: SensorReading): SeverityLevel {
            val heartRate = sensorData.estimateHeartRate()

            return when {
                heartRate == 0 -> SeverityLevel.CRITICAL
                heartRate > 180 || heartRate < 40 -> SeverityLevel.HIGH
                !sensorData.accelerometerData.isStable() -> SeverityLevel.MEDIUM
                else -> SeverityLevel.LOW
            }
        }
    }
}

/**
 * Tipo de evento de pánico
 */
enum class EventType {
    MANUAL,      // Usuario presionó el botón
    AUTOMATIC,   // Sistema detectó anomalía
    SCHEDULED,   // Prueba programada
    TEST         // Prueba de funcionamiento
}

/**
 * Nivel de severidad del evento
 */
enum class SeverityLevel {
    LOW,      // Baja prioridad
    MEDIUM,   // Prioridad media
    HIGH,     // Alta prioridad
    CRITICAL  // Crítico, requiere atención inmediata
}

/**
 * Ubicación del evento
 */
data class Location(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float, // Precisión en metros
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val source: LocationSource = LocationSource.GPS
)

/**
 * Fuente de la ubicación
 */
enum class LocationSource {
    GPS,
    NETWORK,
    MANUAL,
    UNKNOWN
}

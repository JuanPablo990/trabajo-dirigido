package com.juan.esp32.domain.usecases

import com.juan.esp32.domain.models.SensorData
import kotlinx.coroutines.flow.firstOrNull

/**
 * Caso de uso para manejar el botón de pánico.
 * Encapsula toda la lógica relacionada con eventos de pánico.
 */
class PanicButtonUseCase(
    private val repository: com.juan.esp32.data.repository.SensorRepository
) {

    /**
     * Ejecuta el caso de uso cuando se presiona el botón de pánico.
     * @param includeSensorData Si es true, incluye los datos actuales del sensor
     * @param userMessage Mensaje opcional del usuario
     * @return ID del evento de pánico creado
     */
    suspend operator fun invoke(
        includeSensorData: Boolean = true,
        userMessage: String? = null
    ): Result<String> {
        return try {
            val sensorData = if (includeSensorData) {
                // Obtener datos actuales del sensor
                repository.sensorData.firstOrNull()
            } else {
                null
            }

            // Disparar evento de pánico a través del repositorio
            repository.triggerPanicEvent(sensorData)

            // Obtener el último evento de pánico para retornar su ID
            val lastEvent = repository.lastPanicEvent.firstOrNull()

            Result.success(lastEvent?.id ?: "unknown")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifica si hay un evento de pánico reciente (últimos 5 minutos).
     */
    suspend fun hasRecentPanicEvent(): Boolean {
        return try {
            val lastEvent = repository.lastPanicEvent.firstOrNull()
            lastEvent?.let { event ->
                val currentTime = System.currentTimeMillis()
                val fiveMinutesAgo = currentTime - (5 * 60 * 1000)
                event.timestamp > fiveMinutesAgo
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Obtiene estadísticas de eventos de pánico del día actual.
     */
    suspend fun getTodayPanicStats(): Map<String, Any> {
        return try {
            val stats = mutableMapOf<String, Any>()

            // Obtener el último evento
            val lastEvent = repository.lastPanicEvent.firstOrNull()

            stats["lastEventTime"] = lastEvent?.timestamp ?: 0L
            stats["hasRecentEvent"] = hasRecentPanicEvent()

            stats
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

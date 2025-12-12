package com.juan.esp32.domain.usecases

import com.juan.esp32.data.ble.BleManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * Caso de uso para reconectar automáticamente al dispositivo BLE.
 * Encapsula la lógica de reconexión con backoff exponencial.
 */
class ReconnectBleUseCase(
    private val repository: com.juan.esp32.data.repository.SensorRepository
) {

    private var isReconnecting = false
    private var reconnectAttempt = 0
    private val maxReconnectAttempts = 10
    private val initialDelayMs = 1000L
    private val maxDelayMs = 30000L // 30 segundos

    /**
     * Inicia la reconexión automática.
     * Usa backoff exponencial para los reintentos.
     */
    suspend operator fun invoke(): Result<ReconnectionResult> {
        if (isReconnecting) {
            return Result.success(
                ReconnectionResult(
                    success = false,
                    message = "Ya se está reconectando",
                    attemptCount = reconnectAttempt
                )
            )
        }

        isReconnecting = true
        reconnectAttempt = 0

        return try {
            while (reconnectAttempt < maxReconnectAttempts && isReconnecting) {
                reconnectAttempt++

                // Calcular delay con backoff exponencial
                val delayMs = calculateBackoffDelay(reconnectAttempt)

                // Esperar antes del intento (excepto en el primer intento)
                if (reconnectAttempt > 1) {
                    delay(delayMs)
                }

                // Verificar si ya está conectado
                val currentState = repository.connectionState.first()
                if (currentState == BleManager.ConnectionState.CONNECTED) {
                    isReconnecting = false
                    return Result.success(
                        ReconnectionResult(
                            success = true,
                            message = "Ya está conectado",
                            attemptCount = reconnectAttempt - 1
                        )
                    )
                }

                // Intentar reconectar
                val connectResult = connect()

                if (connectResult) {
                    isReconnecting = false
                    return Result.success(
                        ReconnectionResult(
                            success = true,
                            message = "Reconexión exitosa en intento $reconnectAttempt",
                            attemptCount = reconnectAttempt
                        )
                    )
                }

                // Si no fue exitoso, continuar con el siguiente intento
            }

            isReconnecting = false
            Result.success(
                ReconnectionResult(
                    success = false,
                    message = "No se pudo reconectar después de $maxReconnectAttempts intentos",
                    attemptCount = maxReconnectAttempts
                )
            )

        } catch (e: Exception) {
            isReconnecting = false
            Result.failure(e)
        }
    }

    /**
     * Método auxiliar para conectar
     */
    private suspend fun connect(): Boolean {
        return try {
            repository.connectToEsp32()

            // Esperar 5 segundos para ver si se conecta
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < 5000) {
                val state = repository.connectionState.first()
                if (state == BleManager.ConnectionState.CONNECTED) {
                    return true
                }
                delay(500)
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Detiene la reconexión automática.
     */
    fun stopReconnecting() {
        isReconnecting = false
    }

    /**
     * Verifica si se está reconectando actualmente.
     */
    fun isReconnecting(): Boolean = isReconnecting

    /**
     * Calcula el delay para backoff exponencial.
     */
    private fun calculateBackoffDelay(attempt: Int): Long {
        val delay = initialDelayMs * (1L shl (attempt - 1)) // 2^(attempt-1)
        return delay.coerceAtMost(maxDelayMs)
    }

    /**
     * Reinicia el contador de intentos (útil después de una conexión exitosa).
     */
    fun resetAttempts() {
        reconnectAttempt = 0
    }

    /**
     * Monitorea la conexión y reconecta automáticamente cuando se pierde.
     */
    suspend fun startAutoReconnectMonitor() {
        // Este método podría ejecutarse en un bucle infinito en un servicio
        while (true) {
            val state = repository.connectionState.first()

            if (state == BleManager.ConnectionState.DISCONNECTED && !isReconnecting) {
                // Iniciar reconexión automática
                invoke()
            }

            delay(5000) // Verificar cada 5 segundos
        }
    }

    data class ReconnectionResult(
        val success: Boolean,
        val message: String,
        val attemptCount: Int
    )
}

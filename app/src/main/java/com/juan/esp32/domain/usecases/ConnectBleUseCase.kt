package com.juan.esp32.domain.usecases

import com.juan.esp32.data.ble.BleManager
import com.juan.esp32.data.repository.SensorRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Caso de uso para conectar al dispositivo BLE.
 * Encapsula la lógica de conexión y manejo de errores.
 */
class ConnectBleUseCase @Inject constructor(
    private val repository: SensorRepository
) {

    /**
     * Ejecuta el caso de uso para conectar al dispositivo BLE.
     * @param timeoutMs Tiempo máximo de espera en milisegundos
     * @param retryCount Número de reintentos en caso de fallo
     * @return Resultado de la conexión
     */
    suspend operator fun invoke(
        timeoutMs: Long = 10000L,
        retryCount: Int = 3
    ): Result<ConnectionResult> {
        var currentRetry = 0

        while (currentRetry < retryCount) {
            try {
                // Iniciar conexión
                repository.connectToEsp32()

                // Esperar hasta que se conecte o timeout
                val startTime = System.currentTimeMillis()
                var connected = false

                while (System.currentTimeMillis() - startTime < timeoutMs) {
                    val state = repository.connectionState.first()

                    when (state) {
                        BleManager.ConnectionState.CONNECTED -> {
                            connected = true
                            break
                        }
                        BleManager.ConnectionState.DISCONNECTED -> {
                            // Si se desconectó durante la espera, salir
                            break
                        }
                        else -> {
                            // Estado CONNECTING o RECONNECTING, seguir esperando
                            delay(500)
                        }
                    }
                }

                if (connected) {
                    return Result.success(
                        ConnectionResult(
                            success = true,
                            message = "Conexión BLE establecida",
                            retryCount = currentRetry
                        )
                    )
                }

                // Si no se conectó, intentar de nuevo
                currentRetry++
                if (currentRetry < retryCount) {
                    delay(1000) // Esperar 1 segundo antes de reintentar
                }

            } catch (e: Exception) {
                currentRetry++
                if (currentRetry >= retryCount) {
                    return Result.failure(e)
                }
                delay(1000)
            }
        }

        return Result.success(
            ConnectionResult(
                success = false,
                message = "No se pudo conectar después de $retryCount intentos",
                retryCount = retryCount
            )
        )
    }

    /**
     * Verifica si el dispositivo BLE está disponible para conexión.
     */
    suspend fun isDeviceAvailable(): Boolean {
        return try {
            // Esta es una verificación básica
            // Podrías implementar escaneo BLE aquí si es necesario
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Obtiene el estado actual de la conexión BLE.
     */
    suspend fun getConnectionStatus(): BleManager.ConnectionState {
        return repository.connectionState.first()
    }

    /**
     * Verifica si está conectado.
     */
    suspend fun isConnected(): Boolean {
        return getConnectionStatus() == BleManager.ConnectionState.CONNECTED
    }

    data class ConnectionResult(
        val success: Boolean,
        val message: String,
        val retryCount: Int
    )
}

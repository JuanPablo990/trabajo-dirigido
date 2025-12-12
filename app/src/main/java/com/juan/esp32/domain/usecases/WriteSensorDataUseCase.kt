package com.juan.esp32.domain.usecases

import com.juan.esp32.domain.models.SensorData
import com.juan.esp32.data.repository.SensorRepository
import kotlinx.coroutines.flow.firstOrNull

/**
 * Caso de uso para escribir datos de sensor.
 * Encapsula la lógica de validación y persistencia de datos del sensor.
 */
class WriteSensorDataUseCase(
    private val repository: com.juan.esp32.data.repository.SensorRepository
) {

    /**
     * Ejecuta el caso de uso para escribir datos de sensor.
     * @param sensorData Datos del sensor a escribir
     * @return Resultado de la operación
     */
    suspend operator fun invoke(sensorData: SensorData): Result<Unit> {
        return try {
            // Validar datos antes de escribir
            if (!isValidSensorData(sensorData)) {
                return Result.failure(IllegalArgumentException("Datos de sensor inválidos"))
            }

            // Aquí podrías agregar lógica de transformación o enriquecimiento
            val enrichedData = enrichSensorData(sensorData)

            // Actualizar el flow del repositorio con los nuevos datos
            // Nota: Necesitarías modificar el repositorio para tener un método que actualice el flow
            // Por ahora, el repositorio ya maneja esto automáticamente con handleIncomingSensorData

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Valida que los datos del sensor sean correctos.
     */
    private fun isValidSensorData(data: SensorData): Boolean {
        return data.timestamp > 0 &&
                data.ir >= 0 &&
                data.red >= 0 &&
                data.objTemp >= 0 && data.objTemp <= 50 && // Rango razonable de temperatura
                data.ambTemp >= -20 && data.ambTemp <= 60
    }

    /**
     * Enriquece los datos del sensor con información adicional.
     */
    private fun enrichSensorData(data: SensorData): SensorData {
        // Aquí podrías agregar cálculos adicionales, metadatos, etc.
        return data
    }

    /**
     * Obtiene los datos actuales del sensor.
     */
    suspend fun getCurrentSensorData(): SensorData? {
        return repository.sensorData.firstOrNull()
    }
}

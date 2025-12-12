package com.juan.esp32.domain.models

import java.time.LocalDateTime

/**
 * Modelo de dominio para una lectura de sensor.
 * Representa los datos del sensor en el dominio de negocio.
 */
data class SensorReading(
    val id: String,
    val timestamp: LocalDateTime,
    val heartRateData: HeartRateData,
    val accelerometerData: AccelerometerData,
    val gyroscopeData: GyroscopeData,
    val temperatureData: TemperatureData,
    val signalQuality: SignalQuality
) {
    /**
     * Calcula la frecuencia cardíaca estimada.
     * Lógica de negocio centralizada aquí.
     */
    fun estimateHeartRate(): Int {
        return when (signalQuality) {
            SignalQuality.EXCELLENT -> calculateHeartRate()
            SignalQuality.GOOD -> calculateHeartRate()
            SignalQuality.POOR -> 0 // No confiable
            SignalQuality.NO_SIGNAL -> 0
        }
    }

    /**
     * Determina si la lectura es válida para procesar.
     */
    fun isValid(): Boolean {
        return heartRateData.isValid() &&
                accelerometerData.isStable() &&
                signalQuality != SignalQuality.NO_SIGNAL
    }

    /**
     * Algoritmo privado para cálculo de frecuencia cardíaca.
     */
    private fun calculateHeartRate(): Int {
        val ratio = heartRateData.getPPGRatio() // Usamos el método de HeartRateData
        return (60 + (ratio * 40)).toInt().coerceIn(40, 180)
    }
}

/**
 * Datos específicos de frecuencia cardíaca (PPG)
 */
data class HeartRateData(
    val infraredValue: Int,  // Valor IR del sensor PPG
    val redValue: Int,       // Valor Rojo del sensor PPG
    val samplingRate: Int = 100 // Hz
) {
    fun isValid(): Boolean {
        return infraredValue > 0 && redValue > 0
    }

    fun getPPGRatio(): Float {
        return if (infraredValue > 0) redValue.toFloat() / infraredValue.toFloat() else 0f
    }
}

/**
 * Datos del acelerómetro
 */
data class AccelerometerData(
    val x: Float, // m/s²
    val y: Float,
    val z: Float,
    val range: Float = 4.0f // ±4g
) {
    fun getMagnitude(): Float {
        return Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
    }

    fun isStable(): Boolean {
        return getMagnitude() < 1.5 // Si la magnitud es menor a 1.5g, se considera estable
    }

    fun isInMotion(): Boolean {
        return getMagnitude() > 0.5
    }
}

/**
 * Datos del giroscopio
 */
data class GyroscopeData(
    val x: Float, // °/s
    val y: Float,
    val z: Float
) {
    fun getRotationMagnitude(): Float {
        return Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
    }
}

/**
 * Datos de temperatura
 */
data class TemperatureData(
    val objectTemperature: Float, // Temperatura del objeto/body
    val ambientTemperature: Float, // Temperatura ambiente
    val unit: TemperatureUnit = TemperatureUnit.CELSIUS
) {
    fun getTemperatureDifference(): Float {
        return objectTemperature - ambientTemperature
    }
}

/**
 * Calidad de la señal del sensor
 */
enum class SignalQuality {
    EXCELLENT, // Señal perfecta
    GOOD,      // Señal aceptable
    POOR,      // Señal débil
    NO_SIGNAL  // Sin señal
}

/**
 * Unidades de temperatura
 */
enum class TemperatureUnit {
    CELSIUS,
    FAHRENHEIT,
    KELVIN
}

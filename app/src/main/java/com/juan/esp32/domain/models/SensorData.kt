package com.juan.esp32.domain.models

data class SensorData(
    val timestamp: Long,
    val ir: Int,
    val red: Int,
    val ax: Float,
    val ay: Float,
    val az: Float,
    val gx: Float,
    val gy: Float,
    val gz: Float,
    val objTemp: Float,
    val ambTemp: Float
)
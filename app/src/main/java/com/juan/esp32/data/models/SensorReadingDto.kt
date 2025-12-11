package com.juan.esp32.data.models

import com.google.gson.annotations.SerializedName
import java.util.*

data class SensorReadingDto(
    @SerializedName("ts") val timestamp: Long = System.currentTimeMillis(),
    @SerializedName("ir") val infraredValue: Int = 0,
    @SerializedName("red") val redValue: Int = 0,
    @SerializedName("ax") val accelerationX: Float = 0f,
    @SerializedName("ay") val accelerationY: Float = 0f,
    @SerializedName("az") val accelerationZ: Float = 0f,
    @SerializedName("gx") val gyroX: Float = 0f,
    @SerializedName("gy") val gyroY: Float = 0f,
    @SerializedName("gz") val gyroZ: Float = 0f,
    @SerializedName("obj") val objectTemperature: Float = 0f,
    @SerializedName("amb") val ambientTemperature: Float = 0f,
    @SerializedName("deviceId") val deviceId: String = "",
    @SerializedName("battery") val batteryLevel: Int = 100,

    // Para control interno
    val id: String = UUID.randomUUID().toString(),
    val receivedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
) {
    fun toCsvLine(): String {
        return "$timestamp,$infraredValue,$redValue,$accelerationX,$accelerationY,$accelerationZ," +
                "$gyroX,$gyroY,$gyroZ,$objectTemperature,$ambientTemperature,$batteryLevel"
    }

    companion object {
        fun getCsvHeader(): String {
            return "timestamp,ir,red,ax,ay,az,gx,gy,gz,obj_temp,amb_temp,battery"
        }
    }
}
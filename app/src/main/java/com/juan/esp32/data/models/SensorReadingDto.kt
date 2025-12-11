package com.juan.esp32.data.models

/**
 * DTO para recibir datos del ESP32 - Mapea EXACTAMENTE el JSON que recibes
 * No cambies los nombres de las propiedades, deben coincidir con el JSON del ESP32
 */
data class SensorReadingDto(
    // ⚠️ NO CAMBIES ESTOS NOMBRES - Deben coincidir con el JSON del ESP32
    val ts: Long,      // timestamp
    val ir: Int,       // valor infrarrojo
    val red: Int,      // valor rojo
    val ax: Double,    // aceleración X
    val ay: Double,    // aceleración Y
    val az: Double,    // aceleración Z
    val gx: Double,    // giroscopio X
    val gy: Double,    // giroscopio Y
    val gz: Double,    // giroscopio Z
    val obj: Double,   // temperatura objeto
    val amb: Double    // temperatura ambiente
) {
    /**
     * Convierte este DTO a tu domain model SensorData
     */
    fun toSensorData(): com.juan.esp32.domain.models.SensorData {
        return com.juan.esp32.domain.models.SensorData(
            timestamp = ts,
            ir = ir,
            red = red,
            ax = ax.toFloat(),
            ay = ay.toFloat(),
            az = az.toFloat(),
            gx = gx.toFloat(),
            gy = gy.toFloat(),
            gz = gz.toFloat(),
            objTemp = obj.toFloat(),
            ambTemp = amb.toFloat()
        )
    }

    /**
     * Para guardar en CSV - usa los mismos nombres que tu FileManager actual
     */
    fun toCsvLine(): String {
        return "$ts,$ir,$red,${ax.toFloat()},${ay.toFloat()},${az.toFloat()}," +
                "${gx.toFloat()},${gy.toFloat()},${gz.toFloat()},${obj.toFloat()},${amb.toFloat()}"
    }

    companion object {
        /**
         * Cabecera del CSV (opcional, pero útil)
         */
        fun getCsvHeader(): String {
            return "timestamp,ir,red,ax,ay,az,gx,gy,gz,obj_temp,amb_temp"
        }

        /**
         * Intenta crear un DTO desde un JSONObject
         */
        fun fromJsonObject(obj: org.json.JSONObject): SensorReadingDto? {
            return try {
                SensorReadingDto(
                    ts = obj.getLong("ts"),
                    ir = obj.getInt("ir"),
                    red = obj.getInt("red"),
                    ax = obj.getDouble("ax"),
                    ay = obj.getDouble("ay"),
                    az = obj.getDouble("az"),
                    gx = obj.getDouble("gx"),
                    gy = obj.getDouble("gy"),
                    gz = obj.getDouble("gz"),
                    obj = obj.getDouble("obj"),
                    amb = obj.getDouble("amb")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
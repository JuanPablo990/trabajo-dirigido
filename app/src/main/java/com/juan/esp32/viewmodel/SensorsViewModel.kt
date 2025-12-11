package com.juan.esp32.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.juan.esp32.data.models.PanicEventDto
import com.juan.esp32.data.models.SensorReadingDto
import com.juan.esp32.data.storage.FileManager
import kotlinx.coroutines.launch
import org.json.JSONObject

class SensorsViewModel(application: Application) : AndroidViewModel(application) {

    private val fileManager = FileManager(application.applicationContext)
    val latestData = MutableLiveData<com.juan.esp32.domain.models.SensorData>()
    val errorLiveData = MutableLiveData<String>()

    // Nuevo: para eventos de pánico
    val panicEventLiveData = MutableLiveData<com.juan.esp32.data.models.PanicEventDto>()

    fun onNewSensorJson(jsonString: String) {
        viewModelScope.launch {
            try {
                val obj = JSONObject(jsonString)

                // ✅ OPCIÓN 1: Usando el DTO (más limpio)
                val dto = SensorReadingDto.fromJsonObject(obj)
                dto?.let {
                    val sensorData = it.toSensorData()

                    // 1. Enviar a la UI
                    latestData.postValue(sensorData)

                    // 2. Guardar en archivo (usa tu FileManager actual)
                    fileManager.appendDailyData(sensorData)
                } ?: run {
                    errorLiveData.postValue("Error al crear DTO desde JSON")
                }

                // ✅ OPCIÓN 2: Tu código original (sigue funcionando)
                // Mantén esta opción si prefieres no cambiar
                /*
                val data = com.juan.esp32.domain.models.SensorData(
                    timestamp = obj.getLong("ts"),
                    ir = obj.getInt("ir"),
                    red = obj.getInt("red"),
                    ax = obj.getDouble("ax").toFloat(),
                    ay = obj.getDouble("ay").toFloat(),
                    az = obj.getDouble("az").toFloat(),
                    gx = obj.getDouble("gx").toFloat(),
                    gy = obj.getDouble("gy").toFloat(),
                    gz = obj.getDouble("gz").toFloat(),
                    objTemp = obj.getDouble("obj").toFloat(),
                    ambTemp = obj.getDouble("amb").toFloat()
                )

                latestData.postValue(data)
                fileManager.appendDailyData(data)
                */

            } catch (e: Exception) {
                errorLiveData.postValue("Error procesando JSON: ${e.message}")
            }
        }
    }

    /**
     * Función para el botón de pánico
     */
    fun triggerPanicButton(currentSensorData: com.juan.esp32.domain.models.SensorData? = null) {
        viewModelScope.launch {
            // Convertir SensorData a SensorReadingDto si existe
            val sensorDto = currentSensorData?.let { data ->
                SensorReadingDto(
                    ts = data.timestamp,
                    ir = data.ir,
                    red = data.red,
                    ax = data.ax.toDouble(),
                    ay = data.ay.toDouble(),
                    az = data.az.toDouble(),
                    gx = data.gx.toDouble(),
                    gy = data.gy.toDouble(),
                    gz = data.gz.toDouble(),
                    obj = data.objTemp.toDouble(),
                    amb = data.ambTemp.toDouble()
                )
            }

            // Crear evento de pánico
            val panicEvent = PanicEventDto.createManualPanic(sensorDto)

            // Guardar evento (podrías añadir esto a FileManager)
            fileManager.writeLine("PANIC_EVENT: ${panicEvent.toCsvLine()}")

            // Notificar a la UI
            panicEventLiveData.postValue(panicEvent)

            // Aquí podrías también:
            // 1. Vibrar el teléfono
            // 2. Reproducir un sonido
            // 3. Mostrar una alerta
            // 4. Enviar notificación push
        }
    }
}
package com.juan.esp32.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.juan.esp32.domain.models.SensorData
import com.juan.esp32.data.storage.FileManager
import kotlinx.coroutines.launch
import org.json.JSONObject

class SensorsViewModel(application: Application) : AndroidViewModel(application) {

    private val fileManager = FileManager(application.applicationContext)

    val latestData = MutableLiveData<SensorData>()
    val errorLiveData = MutableLiveData<String>()

    fun onNewSensorJson(jsonString: String) {
        viewModelScope.launch {
            try {
                val obj = JSONObject(jsonString)

                val data = SensorData(
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

                // 1. Mandar datos a la UI
                latestData.postValue(data)

                // 2. Guardar en archivo
                fileManager.appendDailyData(data)

            } catch (e: Exception) {
                errorLiveData.postValue("Error procesando JSON: ${e.message}")
            }
        }
    }
}
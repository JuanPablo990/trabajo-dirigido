package com.juan.esp32.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.juan.esp32.data.repository.SensorRepositoryImpl
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SensorsViewModel(application: Application) : AndroidViewModel(application) {

    // ✅ USANDO EL REPOSITORIO SINGLETON
    private val repository = SensorRepositoryImpl.getInstance(application.applicationContext)

    // ✅ EXPONIENDO LOS FLUJOS DEL REPOSITORIO
    val sensorData: StateFlow<com.juan.esp32.domain.models.SensorData?> = repository.sensorData
    val connectionState: StateFlow<com.juan.esp32.data.ble.BleManager.ConnectionState> = repository.connectionState
    val lastPanicEvent: StateFlow<com.juan.esp32.data.models.PanicEventDto?> = repository.lastPanicEvent

    init {
        // Conectar automáticamente al iniciar
        connectToEsp32()
    }

    // ✅ DELEGANDO AL REPOSITORIO
    fun connectToEsp32() {
        repository.connectToEsp32()
    }

    fun disconnectFromEsp32() {
        repository.disconnectFromEsp32()
    }

    fun triggerPanicButton() {
        viewModelScope.launch {
            repository.triggerPanicEvent(sensorData.value)
        }
    }

    fun getTodayStats() {
        viewModelScope.launch {
            val stats = repository.getTodayStats()
            // Puedes usar estos stats en tu UI
            println("📊 Estadísticas del día: $stats")
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.close()
    }
}
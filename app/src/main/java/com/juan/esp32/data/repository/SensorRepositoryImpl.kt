package com.juan.esp32.data.repository

import android.content.Context
import com.juan.esp32.data.ble.BleManager
import com.juan.esp32.data.models.PanicEventDto
import com.juan.esp32.data.models.SensorReadingDto
import com.juan.esp32.data.storage.FileManager
import com.juan.esp32.domain.models.SensorData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repositorio Singleton - Única fuente de verdad para los datos del ESP32
 */
class SensorRepositoryImpl private constructor(
    private val context: Context,
    private val bleManager: BleManager,
    private val fileManager: FileManager
) : SensorRepository {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // Estado interno
    private val _sensorDataFlow = MutableStateFlow<SensorData?>(null)
    private val _lastPanicEvent = MutableStateFlow<PanicEventDto?>(null)
    private val _connectionState = MutableStateFlow(bleManager.connectionState.value)

    init {
        // Observar cambios en el BLE Manager
        coroutineScope.launch {
            bleManager.connectionState.collect { state ->
                _connectionState.value = state
            }
        }

        // Configurar callback para datos recibidos del BLE
        bleManager.onDataReceived = { json ->
            handleIncomingSensorData(json)
        }
    }

    override val sensorData: StateFlow<SensorData?>
        get() = _sensorDataFlow

    override val connectionState: StateFlow<BleManager.ConnectionState>
        get() = _connectionState

    override val lastPanicEvent: StateFlow<PanicEventDto?>
        get() = _lastPanicEvent

    override fun connectToEsp32() {
        bleManager.connect()
    }

    override fun disconnectFromEsp32() {
        bleManager.disconnect()
    }

    override fun close() {
        bleManager.close()
        fileManager.close()
    }

    override fun triggerPanicEvent(sensorData: SensorData?) {
        coroutineScope.launch {
            val panicEvent = if (sensorData != null) {
                // Convertir SensorData a DTO para el evento
                val sensorDto = SensorReadingDto(
                    ts = sensorData.timestamp,
                    ir = sensorData.ir,
                    red = sensorData.red,
                    ax = sensorData.ax.toDouble(),
                    ay = sensorData.ay.toDouble(),
                    az = sensorData.az.toDouble(),
                    gx = sensorData.gx.toDouble(),
                    gy = sensorData.gy.toDouble(),
                    gz = sensorData.gz.toDouble(),
                    obj = sensorData.objTemp.toDouble(),
                    amb = sensorData.ambTemp.toDouble()
                )
                PanicEventDto.createManualPanic(sensorDto)
            } else {
                PanicEventDto.createManualPanic()
            }

            // Actualizar flow
            _lastPanicEvent.value = panicEvent

            // Guardar en archivo
            fileManager.writeLine("PANIC_EVENT,${panicEvent.toCsvLine()}")
        }
    }

    private fun handleIncomingSensorData(json: JSONObject) {
        coroutineScope.launch {
            try {
                val dto = SensorReadingDto.fromJsonObject(json)
                dto?.let { sensorDto ->
                    val sensorData = sensorDto.toSensorData()
                    _sensorDataFlow.value = sensorData
                    fileManager.appendDailyData(sensorData)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun getCurrentMonthFiles(): List<File> = withContext(Dispatchers.IO) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val currentMonth = dateFormat.format(Date())

            val logsDir = File(context.getExternalFilesDir(null), "logs")
            val monthDir = File(logsDir, currentMonth)

            if (monthDir.exists() && monthDir.isDirectory) {
                monthDir.listFiles { file ->
                    file.isFile && file.name.endsWith(".csv")
                }?.toList() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getTodayStats(): Map<String, Any> = withContext(Dispatchers.IO) {
        val stats = mutableMapOf<String, Any>()

        try {
            val todayFiles = getCurrentMonthFiles().filter { file ->
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                file.name.contains(today)
            }

            stats["filesCount"] = todayFiles.size
            stats["totalSize"] = todayFiles.sumOf { it.length() }
            stats["lastUpdate"] = _sensorDataFlow.value?.timestamp ?: 0L

            // Calcular promedios básicos si hay datos
            _sensorDataFlow.value?.let { data ->
                stats["currentTemp"] = data.objTemp
                stats["currentHeartRate"] = data.getHeartRateEstimation()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        stats
    }

    override suspend fun cleanupOldFiles(daysToKeep: Int): Int = withContext(Dispatchers.IO) {
        var deletedCount = 0

        try {
            val logsDir = File(context.getExternalFilesDir(null), "logs")
            if (!logsDir.exists()) return@withContext 0

            val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)

            logsDir.listFiles()?.forEach { monthDir ->
                if (monthDir.isDirectory) {
                    monthDir.listFiles()?.forEach { file ->
                        if (file.isFile && file.lastModified() < cutoffTime) {
                            if (file.delete()) {
                                deletedCount++
                            }
                        }
                    }

                    // Eliminar carpeta del mes si está vacía
                    if (monthDir.listFiles()?.isEmpty() == true) {
                        monthDir.delete()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        deletedCount
    }

    companion object {
        @Volatile
        private var INSTANCE: SensorRepositoryImpl? = null

        fun getInstance(context: Context): SensorRepositoryImpl {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: createInstance(context).also { INSTANCE = it }
            }
        }

        private fun createInstance(context: Context): SensorRepositoryImpl {
            val bleManager = BleManager(context)
            val fileManager = FileManager(context)
            return SensorRepositoryImpl(context, bleManager, fileManager)
        }
    }
}

interface SensorRepository {
    val sensorData: StateFlow<SensorData?>
    val connectionState: StateFlow<BleManager.ConnectionState>
    val lastPanicEvent: StateFlow<PanicEventDto?>

    fun connectToEsp32()
    fun disconnectFromEsp32()
    fun close()
    fun triggerPanicEvent(sensorData: SensorData?)

    suspend fun getCurrentMonthFiles(): List<File>
    suspend fun getTodayStats(): Map<String, Any>
    suspend fun cleanupOldFiles(daysToKeep: Int): Int
}

// Extensión para calcular frecuencia cardíaca estimada
private fun SensorData.getHeartRateEstimation(): Int {
    return if (red > 0 && ir > 0) {
        val ratio = red.toFloat() / ir.toFloat()
        (60 + (ratio * 40)).toInt().coerceIn(40, 180)
    } else {
        0
    }
}
package com.juan.esp32.data.repository

import android.content.Context
import com.juan.esp32.data.ble.BleManager
import com.juan.esp32.data.models.PanicEventDto
import com.juan.esp32.data.models.SensorReadingDto
import com.juan.esp32.data.storage.FileManager
import com.juan.esp32.domain.models.SensorData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Implementación del repositorio que centraliza todas las operaciones de datos
 * de sensores del ESP32. Actúa como única fuente de verdad para la aplicación.
 */
class SensorRepositoryImpl(
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

    /**
     * Inicia la conexión BLE con el ESP32
     */
    override fun connectToEsp32() {
        bleManager.connect()
    }

    /**
     * Desconecta del ESP32
     */
    override fun disconnectFromEsp32() {
        bleManager.disconnect()
    }

    /**
     * Cierra todos los recursos del repositorio
     */
    override fun close() {
        bleManager.close()
        fileManager.close()
    }

    /**
     * Maneja datos entrantes del ESP32
     */
    private fun handleIncomingSensorData(json: JSONObject) {
        coroutineScope.launch {
            try {
                val dto = SensorReadingDto.fromJsonObject(json)
                dto?.let { sensorDto ->
                    // Convertir a domain model
                    val sensorData = sensorDto.toSensorData()

                    // Actualizar flow
                    _sensorDataFlow.value = sensorData

                    // Guardar en almacenamiento local
                    fileManager.appendDailyData(sensorData)

                    // Log para debug
                    println("📊 Datos guardados: ${sensorData.timestamp}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Registra un evento de pánico manual
     */
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

            // Aquí podrías añadir lógica adicional:
            // - Vibrar el dispositivo
            // - Reproducir sonido
            // - Enviar notificación
        }
    }

    /**
     * Obtiene todos los archivos CSV del mes actual
     */
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

    /**
     * Obtiene estadísticas de los datos del día actual
     */
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

    /**
     * Limpia archivos antiguos (más de 30 días)
     */
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
}

/**
 * Interfaz del repositorio (opcional pero recomendada para testing)
 */
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

/**
 * Extensión para calcular frecuencia cardíaca estimada (añade esto a tu SensorData.kt)
 */
private fun SensorData.getHeartRateEstimation(): Int {
    return if (red > 0 && ir > 0) {
        val ratio = red.toFloat() / ir.toFloat()
        // Fórmula simplificada para estimación
        (60 + (ratio * 40)).toInt().coerceIn(40, 180)
    } else {
        0
    }
}
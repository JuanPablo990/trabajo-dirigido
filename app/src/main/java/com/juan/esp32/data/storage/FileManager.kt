package com.juan.esp32.data.storage

import android.content.Context
import com.juan.esp32.data.storage.paths.StoragePaths
import com.juan.esp32.domain.models.SensorData
import org.json.JSONObject
import java.io.FileWriter
import java.io.IOException

class FileManager(private val context: Context) {

    private var writer: FileWriter? = null
    private var currentFileName: String = ""
    private val storagePaths = StoragePaths(context)

    /** Escribe un texto en el archivo diario */
    fun writeLine(text: String) {
        try {
            val file = storagePaths.dailyFile()

            if (file.name != currentFileName) {
                writer?.close()
                writer = FileWriter(file, true)
                currentFileName = file.name
            }

            writer?.apply {
                appendLine(text)
                flush()
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /** NUEVA FUNCIÓN solicitada → para guardar SensorData como JSON */
    fun appendDailyData(data: SensorData) {
        val json = JSONObject().apply {
            put("ts", data.timestamp)
            put("ir", data.ir)
            put("red", data.red)
            put("ax", data.ax)
            put("ay", data.ay)
            put("az", data.az)
            put("gx", data.gx)
            put("gy", data.gy)
            put("gz", data.gz)
            put("obj", data.objTemp)
            put("amb", data.ambTemp)
        }

        writeLine(json.toString())
    }

    fun close() {
        try {
            writer?.close()
        } catch (_: Exception) {
        }
    }
}
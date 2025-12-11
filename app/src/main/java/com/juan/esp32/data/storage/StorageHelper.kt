package com.juan.esp32.data.storage

import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StorageHelper(private val baseDir: File) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun saveJson(json: JSONObject) {
        val today = dateFormat.format(Date())
        val folder = File(baseDir, today.substring(0, 7)) // 2025-01
        if (!folder.exists()) folder.mkdirs()

        val file = File(folder, "$today.csv")
        val writer = FileWriter(file, true)

        val line = buildCsvLine(json)
        writer.appendLine(line)
        writer.flush()
        writer.close()
    }

    private fun buildCsvLine(json: JSONObject): String {
        val ts = json.optLong("ts")
        val ir = json.optInt("ir")
        val red = json.optInt("red")
        val ax = json.optDouble("ax")
        val ay = json.optDouble("ay")
        val az = json.optDouble("az")
        val gx = json.optDouble("gx")
        val gy = json.optDouble("gy")
        val gz = json.optDouble("gz")
        val obj = json.optDouble("obj")
        val amb = json.optDouble("amb")

        return "$ts,$ir,$red,$ax,$ay,$az,$gx,$gy,$gz,$obj,$amb"
    }
}

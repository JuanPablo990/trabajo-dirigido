package com.juan.esp32.data.storage.paths

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class StoragePaths(private val context: Context) {
    private val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun baseFolder(): File {
        // ✅ USO CORRECTO: Almacenamiento privado de la app
        val dir = File(context.getExternalFilesDir(null), "logs")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun monthFolder(): File {
        val month = dateFormat.format(Date())
        val dir = File(baseFolder(), month)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun dailyFile(): File {
        val day = dayFormat.format(Date())
        return File(monthFolder(), "$day.csv")
    }
}
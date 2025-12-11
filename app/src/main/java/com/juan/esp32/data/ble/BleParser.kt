package com.juan.esp32.data.ble

import org.json.JSONObject

object BleParser {

    fun parseJson(bytes: ByteArray): JSONObject? {
        return try {
            val text = bytes.toString(Charsets.UTF_8)
            JSONObject(text)
        } catch (e: Exception) {
            null
        }
    }
}

package com.juan.esp32.data.storage


import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


object ZipCompressor {


    fun zipFile(input: File, outputZip: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZip))).use { out ->
            FileInputStream(input).use { fi ->
                val entry = ZipEntry(input.name)
                out.putNextEntry(entry)
                fi.copyTo(out, 1024)
            }
        }
    }
}
package com.juan.esp32.data.storage

import android.util.Log
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.File
import java.io.FileInputStream

class FTPUploader {
    private val TAG = "FTPUploader"
    private val ftp = FTPClient()

    fun upload(file: File): Boolean {
        return try {
            // Configurar conexión FTP
            ftp.connect(FtpConstants.FTP_HOST, FtpConstants.FTP_PORT)
            ftp.login(FtpConstants.FTP_USERNAME, FtpConstants.FTP_PASSWORD)
            ftp.enterLocalPassiveMode()
            ftp.setFileType(FTP.BINARY_FILE_TYPE)

            // Verificar respuesta del servidor
            if (!FTPReply.isPositiveCompletion(ftp.replyCode)) {
                Log.e(TAG, "❌ FTP Server refused connection")
                ftp.disconnect()
                return false
            }

            // Cambiar al directorio remoto
            ftp.changeWorkingDirectory(FtpConstants.FTP_REMOTE_PATH)

            // Subir archivo
            FileInputStream(file).use { fis ->
                val result = ftp.storeFile(file.name, fis)
                Log.d(TAG, if (result) "✅ Archivo subido: ${file.name}" else "❌ Error subiendo: ${file.name}")
                result
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ FTP Upload Error: ${e.message}")
            e.printStackTrace()
            false
        } finally {
            // Cerrar conexión
            try {
                if (ftp.isConnected) {
                    ftp.logout()
                    ftp.disconnect()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cerrando conexión FTP: ${e.message}")
            }
        }
    }

    fun uploadMultiple(files: List<File>): Map<File, Boolean> {
        val results = mutableMapOf<File, Boolean>()

        if (!connect()) {
            Log.e(TAG, "❌ No se pudo conectar al servidor FTP")
            files.forEach { results[it] = false }
            return results
        }

        files.forEach { file ->
            results[file] = upload(file)
        }

        disconnect()
        return results
    }

    private fun connect(): Boolean {
        return try {
            ftp.connect(FtpConstants.FTP_HOST, FtpConstants.FTP_PORT)
            ftp.login(FtpConstants.FTP_USERNAME, FtpConstants.FTP_PASSWORD)
            ftp.enterLocalPassiveMode()
            ftp.setFileType(FTP.BINARY_FILE_TYPE)

            FTPReply.isPositiveCompletion(ftp.replyCode)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error conectando FTP: ${e.message}")
            false
        }
    }

    private fun disconnect() {
        try {
            if (ftp.isConnected) {
                ftp.logout()
                ftp.disconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error desconectando FTP: ${e.message}")
        }
    }
}
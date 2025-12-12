package com.juan.esp32.domain.usecases

import java.io.File

/**
 * Caso de uso para enviar archivos al servidor.
 * Encapsula la lógica de preparación y envío de archivos.
 */
class SendFilesUseCase(
    private val repository: com.juan.esp32.data.repository.SensorRepository
) {

    /**
     * Ejecuta el caso de uso para enviar archivos.
     * @param files Lista de archivos a enviar (si es null, envía los del mes actual)
     * @param compress Si es true, comprime los archivos antes de enviar
     * @return Resultado con información del envío
     */
    suspend operator fun invoke(
        files: List<File>? = null,
        compress: Boolean = false
    ): Result<SendFilesResult> {
        return try {
            // 1. Obtener archivos a enviar
            val filesToSend = files ?: repository.getCurrentMonthFiles()

            if (filesToSend.isEmpty()) {
                return Result.success(
                    SendFilesResult(
                        success = true,
                        message = "No hay archivos para enviar",
                        filesCount = 0,
                        totalSize = 0
                    )
                )
            }

            // 2. Validar archivos
            val validationResult = validateFiles(filesToSend)
            if (!validationResult.success) {
                return Result.failure(IllegalStateException(validationResult.message))
            }

            // 3. Preparar archivos (comprimir si es necesario)
            val preparedFiles = if (compress) {
                // TODO: Implementar compresión
                // compressFiles(filesToSend)
                filesToSend
            } else {
                filesToSend
            }

            // 4. Enviar archivos (esto dependerá de tu implementación FTP)
            // TODO: Implementar lógica de envío real
            val sendResult = sendFilesToServer(preparedFiles)

            if (sendResult.success) {
                Result.success(sendResult)
            } else {
                Result.failure(RuntimeException("Error al enviar archivos: ${sendResult.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Envía archivos de forma programada (por ejemplo, a las 3 AM).
     */
    suspend fun sendScheduledFiles(): Result<SendFilesResult> {
        return try {
            // Obtener archivos del día anterior
            val yesterdayFiles = getYesterdayFiles()

            // Enviar con compresión por defecto para ahorrar ancho de banda
            invoke(yesterdayFiles, compress = true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene archivos del día anterior.
     */
    private suspend fun getYesterdayFiles(): List<File> {
        val allFiles = repository.getCurrentMonthFiles()
        val yesterday = System.currentTimeMillis() - (24 * 60 * 60 * 1000)

        return allFiles.filter { file ->
            file.lastModified() >= yesterday - (24 * 60 * 60 * 1000) &&
                    file.lastModified() < yesterday
        }
    }

    /**
     * Valida que los archivos sean adecuados para enviar.
     */
    private fun validateFiles(files: List<File>): ValidationResult {
        if (files.isEmpty()) {
            return ValidationResult(false, "No hay archivos para validar")
        }

        for (file in files) {
            if (!file.exists()) {
                return ValidationResult(false, "El archivo ${file.name} no existe")
            }

            if (!file.canRead()) {
                return ValidationResult(false, "No se puede leer el archivo ${file.name}")
            }

            if (file.length() == 0L) {
                return ValidationResult(false, "El archivo ${file.name} está vacío")
            }
        }

        return ValidationResult(true, "Archivos validados correctamente")
    }

    /**
     * Envía archivos al servidor (implementación de ejemplo).
     */
    private suspend fun sendFilesToServer(files: List<File>): SendFilesResult {
        // TODO: Implementar lógica real de envío FTP

        // Simulación de envío exitoso
        return SendFilesResult(
            success = true,
            message = "Archivos enviados exitosamente",
            filesCount = files.size,
            totalSize = files.sumOf { it.length() },
            serverResponse = "OK"
        )
    }

    data class SendFilesResult(
        val success: Boolean,
        val message: String,
        val filesCount: Int,
        val totalSize: Long,
        val serverResponse: String? = null
    )

    data class ValidationResult(
        val success: Boolean,
        val message: String
    )
}

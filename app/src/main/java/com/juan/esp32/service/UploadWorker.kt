package com.juan.esp32.services

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy  // ✅ CAMBIAR A ESTE
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.juan.esp32.data.repository.SensorRepositoryImpl
import com.juan.esp32.data.storage.FTPUploader
import java.io.File
import java.util.concurrent.TimeUnit

class UploadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "UploadWorker"
        private const val WORK_NAME = "ESP32UploadWorker"

        // Constantes para configuración
        private const val SCHEDULED_UPLOAD_TIME_HOUR = 3 // 3 AM
        private const val SCHEDULED_UPLOAD_TIME_MINUTE = 0

        /**
         * Programa el trabajo de subida programada (diario a las 3 AM)
         */
        fun scheduleDailyUpload(context: Context) {
            val uploadRequest = PeriodicWorkRequestBuilder<UploadWorker>(
                24, // repeatInterval
                TimeUnit.HOURS,
                15, // flexInterval
                TimeUnit.MINUTES
            )
                .addTag("daily_upload")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // ✅ CAMBIAR AQUÍ
                uploadRequest
            )

            Log.d(TAG, "Daily upload scheduled at 3 AM")
        }

        /**
         * Programa una subida inmediata
         */
        fun scheduleImmediateUpload(context: Context) {
            val uploadRequest = OneTimeWorkRequestBuilder<UploadWorker>()
                .addTag("immediate_upload")
                .build()

            WorkManager.getInstance(context).enqueue(uploadRequest)
            Log.d(TAG, "Immediate upload scheduled")
        }

        /**
         * Cancela todos los trabajos de subida
         */
        fun cancelAllUploads(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag("daily_upload")
            WorkManager.getInstance(context).cancelAllWorkByTag("immediate_upload")
            Log.d(TAG, "All uploads cancelled")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting upload work...")

        return try {
            // Obtener el repositorio
            val repository = SensorRepositoryImpl.getInstance(applicationContext)

            // 1. Obtener archivos del mes actual
            val files = repository.getCurrentMonthFiles()

            if (files.isEmpty()) {
                Log.d(TAG, "No files to upload")
                return Result.success(createResultData("No files to upload", 0, 0, 0))
            }

            Log.d(TAG, "Found ${files.size} files to upload")

            // 2. Validar archivos
            val validFiles = validateFiles(files)
            if (validFiles.isEmpty()) {
                Log.w(TAG, "No valid files to upload")
                return Result.success(createResultData("No valid files", 0, files.size, 0))
            }

            // 3. Crear instancia de FTPUploader
            val ftpUploader = FTPUploader()

            // 4. Subir archivos
            val results = ftpUploader.uploadMultiple(validFiles)

            // 5. Calcular estadísticas
            val successCount = results.count { it.value }
            val failedCount = results.count { !it.value }
            val totalSize = validFiles.sumOf { it.length() }

            Log.d(TAG, "Upload completed: $successCount successful, $failedCount failed")

            // 6. Opcional: Eliminar archivos subidos exitosamente
            if (successCount > 0) {
                deleteSuccessfullyUploadedFiles(results)
            }

            // 7. Retornar resultado
            val message = if (failedCount == 0) {
                "Todos los archivos subidos exitosamente"
            } else {
                "$successCount de ${validFiles.size} archivos subidos ($failedCount fallaron)"
            }

            Result.success(createResultData(message, successCount, validFiles.size, totalSize))

        } catch (e: Exception) {
            Log.e(TAG, "Error en el trabajo de subida: ${e.message}", e)
            Result.failure(createErrorResultData(e.message ?: "Error desconocido"))
        }
    }

    /**
     * Valida que los archivos sean adecuados para subir
     */
    private fun validateFiles(files: List<File>): List<File> {
        return files.filter { file ->
            file.exists() && file.isFile && file.canRead() && file.length() > 0
        }
    }

    /**
     * Elimina archivos que se subieron exitosamente
     */
    private fun deleteSuccessfullyUploadedFiles(results: Map<File, Boolean>) {
        var deletedCount = 0

        results.forEach { (file, success) ->
            if (success && file.exists()) {
                if (file.delete()) {
                    deletedCount++
                    Log.d(TAG, "Archivo subido eliminado: ${file.name}")
                } else {
                    Log.w(TAG, "Error al eliminar archivo: ${file.name}")
                }
            }
        }

        Log.d(TAG, "Se eliminaron $deletedCount archivos después de la subida exitosa")
    }

    /**
     * Crea datos de resultado exitoso
     */
    private fun createResultData(
        message: String,
        successCount: Int,
        totalFiles: Int,
        totalSize: Long
    ): Data {
        return Data.Builder()
            .putString("message", message)
            .putInt("success_count", successCount)
            .putInt("total_files", totalFiles)
            .putLong("total_size", totalSize)
            .putBoolean("success", true)
            .build()
    }

    /**
     * Crea datos de resultado de error
     */
    private fun createErrorResultData(errorMessage: String): Data {
        return Data.Builder()
            .putString("error", errorMessage)
            .putBoolean("success", false)
            .build()
    }
}
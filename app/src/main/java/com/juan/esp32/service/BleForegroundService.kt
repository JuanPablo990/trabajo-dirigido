package com.juan.esp32.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.juan.esp32.R
import com.juan.esp32.MainActivity
import com.juan.esp32.data.ble.BleManager
import com.juan.esp32.data.repository.SensorRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class BleForegroundService : Service() {

    companion object {
        private const val TAG = "BleForegroundService"
        private const val NOTIFICATION_CHANNEL_ID = "ble_service_channel"
        private const val NOTIFICATION_CHANNEL_NAME = "ESP32 BLE Service"
        private const val NOTIFICATION_ID = 101
        private const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
        private const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"

        fun startService(context: Context) {
            val intent = Intent(context, BleForegroundService::class.java).apply {
                action = ACTION_START_SERVICE
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, BleForegroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.stopService(intent)
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var repository: com.juan.esp32.data.repository.SensorRepository
    private var connectionStateJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")

        // Obtener el repositorio singleton
        repository = SensorRepositoryImpl.getInstance(applicationContext)

        // Crear canal de notificación
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")

        when (intent?.action) {
            ACTION_START_SERVICE -> {
                startForegroundService()
            }
            ACTION_STOP_SERVICE -> {
                stopForegroundService()
            }
            else -> {
                startForegroundService()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy")
        cleanup()
        super.onDestroy()
    }

    private fun startForegroundService() {
        Log.d(TAG, "Starting foreground service")

        // Iniciar servicio en primer plano
        val notification = buildNotification("Conectando al ESP32...", BleManager.ConnectionState.CONNECTING)
        startForeground(NOTIFICATION_ID, notification)

        // Observar estado de conexión
        observeConnectionState()

        // Conectar al ESP32
        scope.launch {
            Log.d(TAG, "Attempting to connect to ESP32...")
            repository.connectToEsp32()
        }
    }

    private fun stopForegroundService() {
        Log.d(TAG, "Stopping foreground service")

        // Desconectar del ESP32
        repository.disconnectFromEsp32()

        // Detener servicio
        stopForeground(true)
        stopSelf()
    }

    private fun observeConnectionState() {
        connectionStateJob?.cancel()

        connectionStateJob = scope.launch {
            repository.connectionState.collect { state ->
                Log.d(TAG, "Connection state changed: $state")

                // Actualizar notificación con el estado actual
                val message = when (state) {
                    BleManager.ConnectionState.DISCONNECTED -> "Desconectado del ESP32"
                    BleManager.ConnectionState.CONNECTING -> "Conectando al ESP32..."
                    BleManager.ConnectionState.CONNECTED -> "Conectado al ESP32"
                    BleManager.ConnectionState.RECONNECTING -> "Reconectando al ESP32..."
                }

                updateNotification(message, state)
            }
        }
    }

    private fun buildNotification(contentText: String, state: BleManager.ConnectionState): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val iconColor = when (state) {
            BleManager.ConnectionState.DISCONNECTED -> R.color.red
            BleManager.ConnectionState.CONNECTING -> R.color.yellow
            BleManager.ConnectionState.CONNECTED -> R.color.green
            BleManager.ConnectionState.RECONNECTING -> R.color.orange
        }

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("ESP32 Monitor")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_bluetooth)
            .setColor(getColor(iconColor))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .build()
    }

    private fun updateNotification(contentText: String, state: BleManager.ConnectionState) {
        val notification = buildNotification(contentText, state)
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Servicio de monitoreo de conexión BLE con ESP32"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun cleanup() {
        connectionStateJob?.cancel()
        connectionStateJob = null
        Log.d(TAG, "Service cleanup completed")
    }
}
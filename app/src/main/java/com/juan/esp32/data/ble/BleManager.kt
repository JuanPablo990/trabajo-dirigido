package com.juan.esp32.data.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {

    private var bluetoothGatt: BluetoothGatt? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val TAG = "BleManager"

    // Estados de conexión
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    enum class ConnectionState {
        DISCONNECTED,    // 🔴 Rojo
        CONNECTING,      // 🟡 Amarillo
        CONNECTED,       // 🟢 Verde
        RECONNECTING     // 🟡 Amarillo (parpadeante)
    }

    // Callback para enviar JSON al repositorio
    var onDataReceived: ((JSONObject) -> Unit)? = null

    fun connect() {
        _connectionState.value = ConnectionState.CONNECTING
        Log.d(TAG, "Intentando conectar al ESP32...")

        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null) {
                Log.e(TAG, "Bluetooth no disponible")
                _connectionState.value = ConnectionState.DISCONNECTED
                return
            }

            val device = adapter.getRemoteDevice(BleConstants.DEVICE_MAC_ADDRESS)
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
            Log.d(TAG, "GATT creado para dispositivo: ${device.name ?: "Sin nombre"}")

        } catch (e: Exception) {
            Log.e(TAG, "Error al conectar: ${e.message}")
            _connectionState.value = ConnectionState.DISCONNECTED
            reconnect()
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "✅ Conectado al ESP32")
                    _connectionState.value = ConnectionState.CONNECTED
                    gatt.discoverServices()
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.w(TAG, "❌ Desconectado del ESP32")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    gatt.close()
                    reconnect()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Error al descubrir servicios: $status")
                return
            }

            Log.d(TAG, "Servicios descubiertos: ${gatt.services.size}")

            // ✅ BUSCAR AUTOMÁTICAMENTE la característica con el UUID correcto
            val targetUuid = java.util.UUID.fromString(BleConstants.CHARACTERISTIC_UUID)
            var characteristicFound = false

            for (service in gatt.services) {
                Log.d(TAG, "Servicio: ${service.uuid}")

                for (characteristic in service.characteristics) {
                    Log.d(TAG, "  - Característica: ${characteristic.uuid}")

                    if (characteristic.uuid == targetUuid) {
                        Log.i(TAG, "✅ Característica objetivo encontrada en servicio: ${service.uuid}")
                        characteristicFound = true

                        // Activar notificaciones
                        gatt.setCharacteristicNotification(characteristic, true)

                        // Configurar descriptor para notificaciones
                        val descriptor = characteristic.getDescriptor(
                            java.util.UUID.fromString(BleConstants.CLIENT_CHARACTERISTIC_CONFIG)
                        )

                        descriptor?.let {
                            it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(it)
                            Log.d(TAG, "✅ Notificaciones activadas")
                        }

                        break
                    }
                }

                if (characteristicFound) break
            }

            if (!characteristicFound) {
                Log.e(TAG, "❌ No se encontró la característica con UUID: ${BleConstants.CHARACTERISTIC_UUID}")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val rawBytes = characteristic.value ?: return
            val text = rawBytes.toString(Charsets.UTF_8)

            try {
                val json = JSONObject(text)
                Log.d(TAG, "📥 Datos recibidos: ${json.length()} campos")

                // ✅ ENVIAR DATOS AL REPOSITORIO
                onDataReceived?.invoke(json)

            } catch (e: Exception) {
                Log.e(TAG, "Error al parsear JSON: ${e.message}")
                Log.d(TAG, "Texto recibido: $text")
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "✅ Descriptor escrito exitosamente")
            } else {
                Log.e(TAG, "❌ Error escribiendo descriptor: $status")
            }
        }
    }

    private fun reconnect() {
        scope.launch {
            _connectionState.value = ConnectionState.RECONNECTING
            Log.d(TAG, "🔄 Reconectando en ${BleConstants.RECONNECT_INTERVAL_MS}ms...")

            delay(BleConstants.RECONNECT_INTERVAL_MS)

            if (_connectionState.value != ConnectionState.CONNECTED) {
                Log.d(TAG, "🔄 Intentando reconexión...")
                connect()
            }
        }
    }

    fun disconnect() {
        Log.d(TAG, "Desconectando manualmente...")
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun close() {
        disconnect()
        scope.cancel()
        // Limpiar el callback cuando se cierre
        onDataReceived = null
        Log.d(TAG, "BleManager cerrado")
    }
}
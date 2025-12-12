package com.juan.esp32.data.storage

object FtpConstants {
    // 🔧 Configuración del servidor FTP
    const val FTP_HOST = "100.110.85.85"
    const val FTP_PORT = 21 // Puerto FTP estándar
    const val FTP_USERNAME = "esp32user" // Tu usuario FTP
    const val FTP_PASSWORD = "Esp32Local2024" // Tu contraseña FTP
    const val FTP_REMOTE_PATH = "/esp32_data/" // Ruta en el servidor

    // ⚙️ Configuración de envío
    const val MAX_RETRIES = 3
    const val TIMEOUT_MS = 30000 // 30 segundos
}
package com.juan.esp32.data.storage

import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.File
import java.io.FileInputStream


class FTPUploader(
    private val host: String,
    private val user: String,
    private val pass: String
) {
    private val ftp = FTPClient()


    fun upload(file: File): Boolean {
        return try {
            ftp.connect(host)
            ftp.login(user, pass)
            ftp.enterLocalPassiveMode()
            ftp.setFileType(FTP.BINARY_FILE_TYPE)


            FileInputStream(file).use { fis ->
                ftp.storeFile(file.name, fis)
            }


            ftp.logout()
            ftp.disconnect()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
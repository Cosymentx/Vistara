package com.obscura.wallpapers.core.common

import android.os.Build
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object RomUtil {
    private const val TAG = "RomUtils"

    val isHuaweiRom: Boolean by lazy {
        checkManufacturer("HUAWEI") || checkProperty("ro.build.display.id", "EMUI") ||
                checkProperty("ro.build.version.emui", "")
    }

    val isMiuiRom: Boolean by lazy {
        checkManufacturer("Xiaomi") || checkProperty("ro.miui.ui.version.name", "") ||
                checkProperty("ro.miui.ui.version.code", "")
    }

    val isOppoRom: Boolean by lazy {
        checkManufacturer("OPPO") || checkProperty("ro.build.version.opporom", "")
    }

    val isVivoRom: Boolean by lazy {
        checkManufacturer("vivo") || checkProperty("ro.vivo.os.version", "")
    }

    val isOnePlusRom: Boolean by lazy {
        checkManufacturer("OnePlus") || checkProperty("ro.build.ota.versionname", "OnePlus")
    }

    val isSamsungRom: Boolean by lazy {
        checkManufacturer("samsung")
    }

    val isMeizuRom: Boolean by lazy {
        checkManufacturer("Meizu") || checkProperty("ro.build.display.id", "Flyme")
    }

    val isLenovoRom: Boolean by lazy {
        checkManufacturer("LENOVO")
    }

    val isSonyRom: Boolean by lazy {
        checkManufacturer("Sony")
    }

    val isGoogleRom: Boolean by lazy {
        checkManufacturer("Google")
    }

    private fun checkManufacturer(manufacturer: String): Boolean {
        return Build.MANUFACTURER.uppercase().contains(manufacturer.uppercase())
    }

    private fun checkProperty(property: String, value: String): Boolean {
        return try {
            val propertyValue = getSystemProperty(property)
            if (value.isEmpty()) {
                !propertyValue.isNullOrEmpty()
            } else {
                propertyValue?.contains(value) == true
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun getSystemProperty(propName: String): String? {
        val line: String
        var input: BufferedReader? = null
        try {
            val p = Runtime.getRuntime().exec("getprop $propName")
            input = BufferedReader(InputStreamReader(p.inputStream), 1024)
            line = input.readLine()
            input.close()
            return line
        } catch (ex: IOException) {
            return null
        } finally {
            if (input != null) {
                try {
                    input.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}

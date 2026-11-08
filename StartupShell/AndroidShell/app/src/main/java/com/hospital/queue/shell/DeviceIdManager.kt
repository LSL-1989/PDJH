package com.hospital.queue.shell

import android.content.Context
import android.content.SharedPreferences
import java.security.SecureRandom

/**
 * 首次启动生成并持久化本机设备 ID（与 H5 generateShellDeviceId 对齐）。
 * 格式：XXXX-XXXX-XXXX-XXXX（19 位），仅 [A-Z0-9-]，总长 ≤20。
 * 清除应用数据后视为新设备；已存的非法旧值（如 HJ-{UUID}）会重新生成。
 */
object DeviceIdManager {
    private const val PREFS_NAME = "shell_device"
    private const val KEY_DEVICE_ID = "device_id"
    private const val MAX_LEN = 20
    private val VALID_PATTERN = Regex("^[A-Z0-9-]{1,$MAX_LEN}$")
    private val CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val random = SecureRandom()

    fun getOrCreateDeviceId(context: Context): String {
        val prefs = prefs(context)
        prefs.getString(KEY_DEVICE_ID, null)?.let { existing ->
            if (isValid(existing)) return existing
        }

        val deviceId = generateShellDeviceId()
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        return deviceId
    }

    /** 与 packages/common generateShellDeviceId 同规则：4 段 × 4 位，共 19 位。 */
    fun generateShellDeviceId(): String {
        fun segment() = buildString(4) {
            repeat(4) { append(CHARS[random.nextInt(CHARS.length)]) }
        }
        return "${segment()}-${segment()}-${segment()}-${segment()}"
    }

    fun isValid(id: String?): Boolean {
        if (id.isNullOrBlank()) return false
        val trimmed = id.trim()
        return VALID_PATTERN.matches(trimmed)
    }

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}

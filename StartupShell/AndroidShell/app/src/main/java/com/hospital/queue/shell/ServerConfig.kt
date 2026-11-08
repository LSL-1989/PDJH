package com.hospital.queue.shell

import android.content.Context
import android.content.SharedPreferences

/**
 * 可配置的服务根 URL，默认来自 BuildConfig.DEFAULT_SERVER_BASE。
 * 空字符串表示 H5 使用相对路径 /api。
 */
object ServerConfig {
    private const val PREFS_NAME = "shell_config"
    private const val KEY_SERVER_BASE = "server_base"

    fun getServerBase(context: Context): String {
        val prefs = prefs(context)
        if (prefs.contains(KEY_SERVER_BASE)) {
            return prefs.getString(KEY_SERVER_BASE, "") ?: ""
        }
        return BuildConfig.DEFAULT_SERVER_BASE
    }

    fun setServerBase(context: Context, baseUrl: String) {
        prefs(context).edit().putString(KEY_SERVER_BASE, baseUrl.trim()).apply()
    }

    /** 清除运行时覆盖，恢复 BuildConfig.DEFAULT_SERVER_BASE。 */
    fun clearServerBase(context: Context) {
        prefs(context).edit().remove(KEY_SERVER_BASE).apply()
    }

    /** WebView 首页：{baseUrl}{WEB_PATH}，base 为空时使用 DEFAULT_SERVER_BASE 加载。 */
    fun getHomeUrl(context: Context): String {
        val configured = getServerBase(context).trimEnd('/')
        val base = configured.ifEmpty {
            BuildConfig.DEFAULT_SERVER_BASE.trimEnd('/')
        }
        val path = BuildConfig.WEB_PATH.trim('/')
        return "$base/$path/"
    }

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}

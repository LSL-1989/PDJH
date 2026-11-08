package com.hospital.queue.shell

import android.content.Context
import android.webkit.JavascriptInterface

/**
 * 注入 window.AndroidNative，供 H5 读取设备指纹与终端类型。
 * 不使用 WiFi MAC；getDeviceId 为 ≤20 位 [A-Z0-9-]（见 DeviceIdManager）。
 */
class AndroidNativeBridge(private val context: Context) {

    @JavascriptInterface
    fun getDeviceId(): String = DeviceIdManager.getOrCreateDeviceId(context)

    @JavascriptInterface
    fun getTerminalType(): String = BuildConfig.TERMINAL_TYPE

    @JavascriptInterface
    fun getServerBase(): String = ServerConfig.getServerBase(context)
}

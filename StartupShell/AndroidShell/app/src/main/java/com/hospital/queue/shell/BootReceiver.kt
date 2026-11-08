package com.hospital.queue.shell

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 开机自启，进入 Kiosk WebView 首页。
 * 兼容标准 BOOT_COMPLETED 与部分国产 ROM 的 QUICKBOOT。
 * 部分 Android 10+ 设备仍可能限制后台拉起，需厂商白名单或设备管理员。
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != ACTION_QUICKBOOT_POWERON &&
            action != ACTION_HTC_QUICKBOOT
        ) {
            return
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(launchIntent)
    }

    companion object {
        private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
        private const val ACTION_HTC_QUICKBOOT = "com.htc.intent.action.QUICKBOOT_POWERON"
    }
}

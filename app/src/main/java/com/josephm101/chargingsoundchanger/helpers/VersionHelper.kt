package com.josephm101.chargingsoundchanger.helpers

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

object VersionHelper {
    fun getAppPackageInfo (context: Context): PackageInfo {
        val pInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        return pInfo
    }

    fun getAppVersionCode(packageInfo: PackageInfo): Long {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // API 28, "Pie"
            return packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            return packageInfo.versionCode.toLong()
        }
    }
}
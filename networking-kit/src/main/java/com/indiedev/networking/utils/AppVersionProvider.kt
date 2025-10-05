package com.indiedev.networking.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log

internal interface AppVersionProvider {
    fun getAppVersionName(): String

    fun getAppVersionCode(): String
}

class AppVersionProviderImp(val context: Context) :
    AppVersionProvider {
    override fun getAppVersionName(): String {
        return getVersionDetail().first ?: "unknown"
    }

    override fun getAppVersionCode(): String {
        return getVersionDetail().second.toString()
    }

    private fun getVersionDetail(): Pair<String?, Int> {
        try {
            val pInfo: PackageInfo =
                context.packageManager.getPackageInfo(context.packageName, 0)

            return Pair(pInfo.versionName, pInfo.versionCode)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(this::javaClass.name, e.message.toString())
        }
        return Pair("", 0)
    }
}

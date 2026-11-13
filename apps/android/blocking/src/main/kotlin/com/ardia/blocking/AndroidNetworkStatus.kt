package com.ardia.blocking

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class AndroidNetworkStatus(context: Context) : NetworkStatus {
    private val connectivity: ConnectivityManager? = context.applicationContext.getSystemService(ConnectivityManager::class.java)

    override val isOnline: Boolean
        get() {
            val manager = connectivity ?: return true
            return runCatching {
                val network = manager.activeNetwork ?: return false
                manager.getNetworkCapabilities(network)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            }.getOrDefault(true)
        }

    override val isExpensive: Boolean
        get() {
            val manager = connectivity ?: return false
            return runCatching { manager.isActiveNetworkMetered }.getOrDefault(false)
        }
}

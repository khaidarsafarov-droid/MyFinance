package com.truckerload.presentation.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class ConnectivityStatus {
    Online,
    Offline,
}

object ConnectivityObserver {
    fun observe(context: Context): Flow<ConnectivityStatus> = callbackFlow {
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        fun current(): ConnectivityStatus {
            val network = cm.activeNetwork ?: return ConnectivityStatus.Offline
            val caps = cm.getNetworkCapabilities(network) ?: return ConnectivityStatus.Offline
            val online = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            return if (online) ConnectivityStatus.Online else ConnectivityStatus.Offline
        }
        trySend(current())
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(current())
            }

            override fun onLost(network: Network) {
                trySend(current())
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(current())
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, callback)
        awaitClose { runCatching { cm.unregisterNetworkCallback(callback) } }
    }.distinctUntilChanged()
}

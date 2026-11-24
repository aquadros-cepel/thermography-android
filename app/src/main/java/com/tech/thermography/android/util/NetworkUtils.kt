package com.tech.thermography.android.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

enum class NetworkTransport {
    DISCONNECTED,
    WIFI,
    CELLULAR,
    OTHER
}

object NetworkUtils {
    private fun currentNetworkTransport(context: Context): NetworkTransport {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return NetworkTransport.DISCONNECTED
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return NetworkTransport.DISCONNECTED
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return NetworkTransport.DISCONNECTED
        }
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkTransport.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkTransport.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkTransport.OTHER
            else -> NetworkTransport.DISCONNECTED
        }
    }

    fun hasInternetConnection(context: Context): Boolean =
        currentNetworkTransport(context) != NetworkTransport.DISCONNECTED

    fun observeConnectivity(context: Context): Flow<Boolean> =
        observeNetworkTransport(context)
            .map { it != NetworkTransport.DISCONNECTED }
            .distinctUntilChanged()

    fun observeNetworkTransport(context: Context): Flow<NetworkTransport> = callbackFlow {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            private fun publishNetworkTransport() {
                trySend(currentNetworkTransport(context))
            }

            override fun onAvailable(network: Network) = publishNetworkTransport()
            override fun onLost(network: Network) = publishNetworkTransport()
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) = publishNetworkTransport()
        }

        connectivityManager.registerDefaultNetworkCallback(callback)
        trySend(currentNetworkTransport(context))
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}


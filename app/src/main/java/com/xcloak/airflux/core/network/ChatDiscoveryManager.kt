package com.xcloak.airflux.core.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DiscoveredChat(
    val name: String,
    val ip: String,
    val port: Int,
    val token: String
)

class ChatDiscoveryManager(context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val serviceType = "_airflux_chat._tcp."

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredChat>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredChat>> = _discoveredDevices.asStateFlow()

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun registerService(port: Int, token: String, deviceName: String = android.os.Build.MODEL) {
        stopRegistration()

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "AirFlux Chat ($deviceName)"
            serviceType = this@ChatDiscoveryManager.serviceType
            setPort(port)
            // Use setAttribute for the token (requires API 21+)
            setAttribute("token", token)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                Log.d("NSD", "Service registered: ${info.serviceName}")
            }

            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e("NSD", "Registration failed: $errorCode")
            }

            override fun onServiceUnregistered(info: NsdServiceInfo) {}
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
        }

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun startDiscovery() {
        stopDiscovery()
        _discoveredDevices.value = emptyList()

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onDiscoveryStarted(serviceType: String) {}

            override fun onDiscoveryStopped(serviceType: String) {}

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType == this@ChatDiscoveryManager.serviceType) {
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}

                        override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                            val ip = resolvedInfo.host.hostAddress ?: return
                            val port = resolvedInfo.port
                            val token = resolvedInfo.attributes["token"]?.let { String(it) } ?: ""
                            val name = resolvedInfo.serviceName

                            val discovered = DiscoveredChat(name, ip, port, token)
                            val current = _discoveredDevices.value.toMutableList()
                            if (current.none { it.ip == ip && it.port == port }) {
                                current.add(discovered)
                                _discoveredDevices.value = current
                            }
                        }
                    })
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                val current = _discoveredDevices.value.toMutableList()
                current.removeAll { it.name == serviceInfo.serviceName }
                _discoveredDevices.value = current
            }
        }

        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stopRegistration() {
        registrationListener?.let {
            try {
                nsdManager.unregisterService(it)
            } catch (e: Exception) {}
            registrationListener = null
        }
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {}
            discoveryListener = null
        }
    }
}

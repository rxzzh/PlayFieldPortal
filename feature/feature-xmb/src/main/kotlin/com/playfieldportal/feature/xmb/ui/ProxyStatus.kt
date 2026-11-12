package com.playfieldportal.feature.xmb.ui

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri

/** A local OS configuration indicator, not an assertion that an external website is reachable. */
enum class ProxyState(val label: String, val description: String) {
    OFF("PROXY OFF", "VPN and system proxy off"),
    VPN("VPN ON", "VPN active"),
    SYSTEM_PROXY("PROXY ON", "System HTTP proxy configured"),
    UNKNOWN("PROXY ?", "Proxy status unavailable"),
}

internal fun proxyStateOf(vpn: Boolean, systemProxy: Boolean): ProxyState = when {
    vpn -> ProxyState.VPN
    systemProxy -> ProxyState.SYSTEM_PROXY
    else -> ProxyState.OFF
}

@Suppress("DEPRECATION")
internal fun readProxyState(cm: ConnectivityManager?): ProxyState {
    if (cm == null) return ProxyState.UNKNOWN
    return runCatching {
        val vpn = cm.allNetworks.any { cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true }
        val proxy = cm.defaultProxy
        val configured = proxy?.host?.isNotBlank() == true ||
            (proxy != null && proxy.pacFileUrl != Uri.EMPTY)
        proxyStateOf(vpn, configured)
    }.getOrDefault(ProxyState.UNKNOWN)
}

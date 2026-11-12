package com.playfieldportal.feature.xmb.ui

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.ProxyInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProxyStatusTest {
    private val cm = mockk<ConnectivityManager>()
    @Test fun `VPN and HTTP proxy states are distinct from off`() {
        assertEquals(ProxyState.OFF, proxyStateOf(false, false))
        assertEquals(ProxyState.VPN, proxyStateOf(true, false))
        assertEquals(ProxyState.VPN, proxyStateOf(true, true))
        assertEquals(ProxyState.SYSTEM_PROXY, proxyStateOf(false, true))
    }
    @Test fun `installed or running VPN app is not enough - network transport must be active`() {
        val wifi = mockk<Network>()
        every { cm.allNetworks } returns arrayOf(wifi)
        val capabilities = mockk<NetworkCapabilities>()
        every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) } returns false
        every { cm.getNetworkCapabilities(wifi) } returns capabilities
        every { cm.defaultProxy } returns null
        assertEquals(ProxyState.OFF, readProxyState(cm))
        every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) } returns true
        assertEquals(ProxyState.VPN, readProxyState(cm))
    }
    @Test fun `system HTTP proxy is detected without a VPN`() {
        every { cm.allNetworks } returns emptyArray()
        every { cm.defaultProxy } returns ProxyInfo.buildDirectProxy("127.0.0.1", 7890)
        assertEquals(ProxyState.SYSTEM_PROXY, readProxyState(cm))
    }
    @Test fun `missing service or inaccessible network state is unknown rather than falsely off`() {
        assertEquals(ProxyState.UNKNOWN, readProxyState(null))
        every { cm.allNetworks } throws SecurityException("unavailable")
        assertEquals(ProxyState.UNKNOWN, readProxyState(cm))
    }
}

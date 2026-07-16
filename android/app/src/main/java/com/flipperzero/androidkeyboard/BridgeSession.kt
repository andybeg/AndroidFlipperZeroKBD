package com.flipperzero.androidkeyboard

import android.content.Context
import com.flipperzero.androidkeyboard.ble.FlipperBleClient

object BridgeSession {
    private var client: FlipperBleClient? = null

    fun getClient(context: Context): FlipperBleClient {
        return client ?: FlipperBleClient(context.applicationContext).also { client = it }
    }

    fun sendTap(keyCode: Byte, modifiers: Byte = 0): Boolean {
        return client?.sendTap(keyCode, modifiers) == true
    }

    fun isReady(): Boolean {
        return client?.state == FlipperBleClient.State.READY
    }
}

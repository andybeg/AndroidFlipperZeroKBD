package com.flipperzero.androidkeyboard

import android.content.Context
import com.flipperzero.androidkeyboard.ble.BridgeProtocol
import com.flipperzero.androidkeyboard.ble.FlipperBleClient

object BridgeSession {
    private var client: FlipperBleClient? = null

    fun getClient(context: Context): FlipperBleClient {
        return client ?: FlipperBleClient(context.applicationContext).also { client = it }
    }

    fun sendTap(keyCode: Byte, modifiers: Byte = 0): Boolean {
        return client?.sendTap(keyCode, modifiers) == true
    }

    fun sendMouseMove(dx: Int, dy: Int): Boolean {
        return client?.sendMouseMove(dx, dy) == true
    }

    fun sendMouseButton(down: Boolean, button: Byte = BridgeProtocol.MOUSE_BTN_LEFT): Boolean {
        return client?.sendMouseButton(down, button) == true
    }

    fun sendMouseScroll(delta: Int): Boolean {
        return client?.sendMouseScroll(delta) == true
    }

    fun isReady(): Boolean {
        return client?.state == FlipperBleClient.State.READY
    }
}

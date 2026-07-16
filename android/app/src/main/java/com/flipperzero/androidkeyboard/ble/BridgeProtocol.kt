package com.flipperzero.androidkeyboard.ble

/**
 * Wire frames: explicit key down / key up.
 *
 * FB 4B 03 [event][mods][keycode]
 * event: 0x01 = down, 0x02 = up
 */
object BridgeProtocol {
    private const val MAGIC_0: Byte = 0xFB.toByte()
    private const val MAGIC_1: Byte = 0x4B
    private const val PAYLOAD_LEN: Byte = 0x03

    const val EVENT_DOWN: Byte = 0x01
    const val EVENT_UP: Byte = 0x02

    fun encodeKeyEvent(down: Boolean, keyCode: Byte, modifiers: Byte = 0): ByteArray {
        return byteArrayOf(
            MAGIC_0,
            MAGIC_1,
            PAYLOAD_LEN,
            if (down) EVENT_DOWN else EVENT_UP,
            modifiers,
            keyCode,
        )
    }
}

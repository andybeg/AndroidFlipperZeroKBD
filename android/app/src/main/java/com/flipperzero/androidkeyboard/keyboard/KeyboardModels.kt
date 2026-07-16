package com.flipperzero.androidkeyboard.keyboard

data class KeyboardLayout(
    val name: String,
    val rows: List<List<KeyboardKey>>,
)

data class KeyboardKey(
    val label: String,
    val hid: Byte,
    val mods: Byte = 0,
    val span: Float = 1f,
    /** If true, tapping toggles a sticky modifier instead of sending a tap. */
    val stickyMod: Boolean = false,
)

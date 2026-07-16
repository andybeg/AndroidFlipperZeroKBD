package com.flipperzero.androidkeyboard.keyboard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader

object KeyboardLayoutLoader {

    private const val DEFAULT_ASSET = "keyboard.json"

    fun loadFromAssets(context: Context, assetName: String = DEFAULT_ASSET): KeyboardLayout {
        val json = context.assets.open(assetName).bufferedReader().use(BufferedReader::readText)
        return parse(json)
    }

    fun parse(json: String): KeyboardLayout {
        val root = JSONObject(json)
        val name = root.optString("name", "unnamed")
        val rowsJson = root.getJSONArray("rows")
        val rows = mutableListOf<List<KeyboardKey>>()

        for (r in 0 until rowsJson.length()) {
            val rowJson = rowsJson.getJSONArray(r)
            rows += parseRow(rowJson)
        }

        return KeyboardLayout(name = name, rows = rows)
    }

    private fun parseRow(rowJson: JSONArray): List<KeyboardKey> {
        val keys = mutableListOf<KeyboardKey>()
        for (i in 0 until rowJson.length()) {
            val obj = rowJson.getJSONObject(i)
            keys += KeyboardKey(
                label = obj.getString("label"),
                hid = parseHexByte(obj.getString("hid")),
                mods = parseHexByte(obj.optString("mods", "0x00")),
                span = obj.optDouble("span", 1.0).toFloat().coerceAtLeast(0.5f),
                stickyMod = obj.optBoolean("sticky_mod", false),
            )
        }
        return keys
    }

    private fun parseHexByte(raw: String): Byte {
        val cleaned = raw.trim().removePrefix("0x").removePrefix("0X")
        return cleaned.toInt(16).toByte()
    }
}

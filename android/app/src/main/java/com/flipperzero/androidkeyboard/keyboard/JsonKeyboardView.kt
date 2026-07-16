package com.flipperzero.androidkeyboard.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt

class JsonKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    fun interface Listener {
        fun onKey(key: KeyboardKey, effectiveMods: Byte)
    }

    var listener: Listener? = null

    private var stickyMods: Byte = 0
    private val keyViews = mutableListOf<Pair<KeyboardKey, TextView>>()

    init {
        orientation = VERTICAL
        setBackgroundColor(BG)
    }

    fun bindLayout(layout: KeyboardLayout) {
        removeAllViews()
        keyViews.clear()
        stickyMods = 0

        val density = resources.displayMetrics.density
        val margin = (4 * density).toInt()

        layout.rows.forEach { row ->
            val grid = GridLayout(context)
            val totalSpan = row.sumOf { it.span.toDouble() }.toFloat().coerceAtLeast(1f)
            // Use integer column units: multiply spans by 2 for half-steps (1.5 → 3)
            val unitColumns = (totalSpan * 2).toInt().coerceAtLeast(row.size)
            grid.columnCount = unitColumns
            grid.setPadding(margin, margin / 2, margin, margin / 2)

            var column = 0
            row.forEach { key ->
                val units = (key.span * 2).toInt().coerceAtLeast(1)
                val button = TextView(context).apply {
                    text = key.label
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    setBackgroundColor(keyBackground(key, false))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, (14 * density).toInt(), 0, (14 * density).toInt())
                    setOnClickListener { onKeyClicked(key) }
                }

                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(column, units, units.toFloat())
                    setMargins(margin, margin, margin, margin)
                }
                button.layoutParams = params
                grid.addView(button)
                keyViews += key to button
                column += units
            }
            addView(grid)
        }
        refreshStickyVisuals()
    }

    private fun onKeyClicked(key: KeyboardKey) {
        if (key.stickyMod) {
            stickyMods = (stickyMods.toInt() xor key.mods.toInt()).toByte()
            refreshStickyVisuals()
            return
        }

        val mods = (key.mods.toInt() or stickyMods.toInt()).toByte()
        listener?.onKey(key, mods)

        if (stickyMods.toInt() != 0 && key.hid.toInt() != 0) {
            stickyMods = 0
            refreshStickyVisuals()
        }
    }

    private fun refreshStickyVisuals() {
        keyViews.forEach { (key, view) ->
            val active = key.stickyMod && (stickyMods.toInt() and key.mods.toInt()) != 0
            view.setBackgroundColor(keyBackground(key, active))
        }
    }

    private fun keyBackground(key: KeyboardKey, stickyActive: Boolean): Int {
        return when {
            stickyActive -> ACCENT
            key.stickyMod || key.label in SPECIAL_LABELS -> KEY_SPECIAL
            else -> KEY_NORMAL
        }
    }

    companion object {
        private val BG = "#121212".toColorInt()
        private val KEY_NORMAL = "#2A2A2A".toColorInt()
        private val KEY_SPECIAL = "#3A3A3A".toColorInt()
        private val ACCENT = "#2E7D32".toColorInt()
        private val SPECIAL_LABELS = setOf("⇥", "⌫", "␣", "↵", "⇧")
    }
}

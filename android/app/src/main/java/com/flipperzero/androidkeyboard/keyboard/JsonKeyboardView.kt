package com.flipperzero.androidkeyboard.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import kotlin.math.abs

class JsonKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    fun interface KeyListener {
        fun onKey(key: KeyboardKey, effectiveMods: Byte)
    }

    fun interface LayoutSwipeListener {
        /** +1 = swipe right (next), -1 = swipe left (previous). */
        fun onLayoutSwipe(direction: Int)
    }

    var keyListener: KeyListener? = null
    var layoutSwipeListener: LayoutSwipeListener? = null

    private var stickyMods: Byte = 0
    private val keyViews = mutableListOf<Pair<KeyboardKey, TextView>>()
    private val swipeThresholdPx: Float

    init {
        orientation = VERTICAL
        setBackgroundColor(BG)
        swipeThresholdPx = 48f * resources.displayMetrics.density
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
            val unitColumns = (totalSpan * 2).toInt().coerceAtLeast(row.size)
            grid.columnCount = unitColumns
            grid.setPadding(margin, margin / 2, margin, margin / 2)

            var column = 0
            row.forEach { cell ->
                val units = (cell.span * 2).toInt().coerceAtLeast(1)
                val view: View = when (cell) {
                    is KeyboardCell.Single -> createKeyButton(cell.key, density, stacked = false)
                    is KeyboardCell.Stack -> createStackView(cell, density)
                }

                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(column, units, units.toFloat())
                    setMargins(margin, margin, margin, margin)
                }
                view.layoutParams = params
                grid.addView(view)
                column += units
            }
            addView(grid)
        }
        refreshStickyVisuals()
    }

    private fun createStackView(
        stack: KeyboardCell.Stack,
        density: Float,
    ): LinearLayout {
        val vertical = stack.axis == StackAxis.VERTICAL
        val container = LinearLayout(context).apply {
            orientation = if (vertical) VERTICAL else HORIZONTAL
            weightSum = stack.keys.size.toFloat().coerceAtLeast(1f)
        }
        val gap = (2 * density).toInt()
        stack.keys.forEachIndexed { index, key ->
            val button = createKeyButton(key, density, stacked = true)
            val lp = LinearLayout.LayoutParams(
                if (vertical) ViewGroup.LayoutParams.MATCH_PARENT else 0,
                if (vertical) 0 else ViewGroup.LayoutParams.MATCH_PARENT,
                1f,
            ).apply {
                if (vertical) {
                    if (index > 0) topMargin = gap
                } else {
                    if (index > 0) marginStart = gap
                }
            }
            button.layoutParams = lp
            container.addView(button)
        }
        // Match typical single-key height so ↑↓ sit in one “standard” slot.
        container.minimumHeight = ((if (vertical) 48 else 36) * density).toInt()
        return container
    }

    private fun createKeyButton(key: KeyboardKey, density: Float, stacked: Boolean): TextView {
        val dual = !key.altLabel.isNullOrBlank()
        return TextView(context).apply {
            text = formatKeyText(key)
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setBackgroundColor(keyBackground(key, false))
            val primarySp = when {
                stacked -> 13f
                dual -> 16f
                key.label.length > 2 -> 14f
                else -> 18f
            }
            setTextSize(TypedValue.COMPLEX_UNIT_SP, primarySp)
            typeface = Typeface.DEFAULT_BOLD
            val padV = when {
                stacked -> (4 * density).toInt()
                dual -> (8 * density).toInt()
                else -> (12 * density).toInt()
            }
            setPadding(0, padV, 0, padV)
            attachKeyInteraction(key, this)
            keyViews += key to this
        }
    }

    private fun formatKeyText(key: KeyboardKey): CharSequence {
        val alt = key.altLabel?.takeIf { it.isNotBlank() } ?: return key.label
        val text = "${key.label}\n$alt"
        val spanned = SpannableString(text)
        val start = key.label.length + 1
        spanned.setSpan(
            RelativeSizeSpan(0.58f),
            start,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        spanned.setSpan(
            ForegroundColorSpan(ALT_LABEL),
            start,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        return spanned
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachKeyInteraction(key: KeyboardKey, button: TextView) {
        if (key.isSpace) {
            var downX = 0f
            var tracking = false
            button.setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.x
                        tracking = true
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!tracking) return@setOnTouchListener false
                        tracking = false
                        val dx = event.x - downX
                        if (abs(dx) >= swipeThresholdPx) {
                            layoutSwipeListener?.onLayoutSwipe(if (dx > 0) 1 else -1)
                        } else {
                            onKeyClicked(key)
                        }
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        tracking = false
                        true
                    }
                    else -> true
                }
            }
        } else {
            button.setOnClickListener { onKeyClicked(key) }
        }
    }

    private fun onKeyClicked(key: KeyboardKey) {
        if (key.stickyMod) {
            stickyMods = (stickyMods.toInt() xor key.mods.toInt()).toByte()
            refreshStickyVisuals()
            return
        }

        val mods = (key.mods.toInt() or stickyMods.toInt()).toByte()
        keyListener?.onKey(key, mods)

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
            key.stickyMod || key.label in SPECIAL_LABELS || key.isSpace -> KEY_SPECIAL
            else -> KEY_NORMAL
        }
    }

    companion object {
        private val BG = "#121212".toColorInt()
        private val KEY_NORMAL = "#2A2A2A".toColorInt()
        private val KEY_SPECIAL = "#3A3A3A".toColorInt()
        private val ACCENT = "#2E7D32".toColorInt()
        private val ALT_LABEL = "#9E9E9E".toColorInt()
        private val SPECIAL_LABELS = setOf(
            "⇥", "⌫", "␣", "↵", "⇧", "⌃", "⌥", "⌘", "esc", "☰",
            "Tab", "Bksp", "Space", "Enter", "Shift", "Ctrl", "Win", "Alt", "Menu",
            "←", "↑", "↓", "→",
        )
    }
}

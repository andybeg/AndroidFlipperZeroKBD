package com.flipperzero.androidkeyboard

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.flipperzero.androidkeyboard.ble.BlePermissions
import com.flipperzero.androidkeyboard.ble.FlipperBleClient
import com.flipperzero.androidkeyboard.databinding.ActivityKeyboardBinding
import com.flipperzero.androidkeyboard.keyboard.JsonKeyboardView
import com.flipperzero.androidkeyboard.keyboard.KeyboardKey
import com.flipperzero.androidkeyboard.keyboard.KeyboardLayout
import com.flipperzero.androidkeyboard.keyboard.KeyboardLayoutLoader
import com.flipperzero.androidkeyboard.keyboard.LayoutInfo
import com.flipperzero.androidkeyboard.prefs.AppPreferences
import com.flipperzero.androidkeyboard.touchpad.TouchpadView

class KeyboardActivity : AppCompatActivity(), FlipperBleClient.Listener {

    private enum class InputMode { KEYBOARD, TOUCHPAD }

    private lateinit var binding: ActivityKeyboardBinding
    private lateinit var bleClient: FlipperBleClient
    private lateinit var prefs: AppPreferences

    private var catalog: List<LayoutInfo> = emptyList()
    private var enabledLayouts: List<LayoutInfo> = emptyList()
    private var currentIndex: Int = 0
    private var inputMode: InputMode = InputMode.KEYBOARD
    private val hideBannerRunnable = Runnable { hideLayoutBanner() }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (!results.values.all { it }) {
            toast(getString(R.string.ble_permission_required))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        enterFullscreen()

        binding = ActivityKeyboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = AppPreferences(this)
        bleClient = BridgeSession.getClient(this)
        catalog = KeyboardLayoutLoader.loadCatalog(this)

        binding.bleDot.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor("#777777".toColorInt())
        }

        binding.keyboardView.keyListener = JsonKeyListener()
        binding.keyboardView.layoutSwipeListener = LayoutSwipeListener()
        binding.touchpadView.listener = TouchpadListener()

        binding.btnBle.setOnClickListener { onBleButtonClicked() }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.btnModeKeyboard.setOnClickListener { setInputMode(InputMode.KEYBOARD) }
        binding.btnModeTouchpad.setOnClickListener { setInputMode(InputMode.TOUCHPAD) }

        setInputMode(InputMode.KEYBOARD)
        ensurePermissions()
        renderBleState(bleClient.state, bleClient.statusMessage)
    }

    override fun onResume() {
        super.onResume()
        enterFullscreen()
        bleClient.setListener(this)
        renderBleState(bleClient.state, bleClient.statusMessage)
        reloadEnabledLayouts()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enterFullscreen()
        }
    }

    override fun onPause() {
        binding.root.removeCallbacks(hideBannerRunnable)
        bleClient.setListener(null)
        super.onPause()
    }

    override fun onStateChanged(state: FlipperBleClient.State, message: String) {
        runOnUiThread { renderBleState(state, message) }
    }

    private fun reloadEnabledLayouts() {
        val enabledIds = prefs.enabledLayoutIds(catalog)
        enabledLayouts = enabledIds.mapNotNull { id -> catalog.firstOrNull { it.id == id } }
        if (enabledLayouts.isEmpty()) {
            enabledLayouts = catalog
        }

        val wantedId = prefs.currentLayoutId
        currentIndex = enabledLayouts.indexOfFirst { it.id == wantedId }.takeIf { it >= 0 } ?: 0
        applyCurrentLayout()
    }

    private fun applyCurrentLayout(announce: Boolean = false) {
        if (enabledLayouts.isEmpty()) return
        currentIndex = currentIndex.coerceIn(0, enabledLayouts.lastIndex)
        val info = enabledLayouts[currentIndex]
        val layout: KeyboardLayout = KeyboardLayoutLoader.loadLayout(this, info)
        binding.keyboardView.bindLayout(layout)
        binding.txtLayoutName.text = getString(R.string.layout_active, layout.name)
        prefs.currentLayoutId = layout.id
        if (announce) {
            showLayoutBanner(layout.name)
        }
    }

    private fun cycleLayout(direction: Int) {
        if (enabledLayouts.size <= 1) {
            toast(getString(R.string.layout_only_one))
            return
        }
        val size = enabledLayouts.size
        currentIndex = ((currentIndex + direction) % size + size) % size
        applyCurrentLayout(announce = true)
    }

    private fun showLayoutBanner(name: String) {
        binding.txtLayoutBanner.animate().cancel()
        binding.root.removeCallbacks(hideBannerRunnable)
        binding.txtLayoutBanner.text = name
        binding.txtLayoutBanner.alpha = 1f
        binding.txtLayoutBanner.visibility = View.VISIBLE
        binding.root.postDelayed(hideBannerRunnable, LAYOUT_BANNER_MS)
    }

    private fun hideLayoutBanner() {
        binding.txtLayoutBanner.animate()
            .alpha(0f)
            .setDuration(200L)
            .withEndAction {
                binding.txtLayoutBanner.visibility = View.GONE
                binding.txtLayoutBanner.alpha = 1f
            }
            .start()
    }

    private fun setInputMode(mode: InputMode) {
        inputMode = mode
        val keyboard = mode == InputMode.KEYBOARD
        binding.keyboardView.visibility = if (keyboard) View.VISIBLE else View.GONE
        binding.touchpadView.visibility = if (keyboard) View.GONE else View.VISIBLE
        binding.txtLayoutName.visibility = if (keyboard) View.VISIBLE else View.GONE
        binding.btnModeKeyboard.isSelected = keyboard
        binding.btnModeTouchpad.isSelected = !keyboard
    }

    private inner class JsonKeyListener : JsonKeyboardView.KeyListener {
        override fun onKey(key: KeyboardKey, effectiveMods: Byte) {
            if (key.hid.toInt() == 0) return
            if (!BridgeSession.sendTap(key.hid, effectiveMods)) {
                toast(getString(R.string.ble_not_ready))
            }
        }
    }

    private inner class LayoutSwipeListener : JsonKeyboardView.LayoutSwipeListener {
        override fun onLayoutSwipe(direction: Int) {
            cycleLayout(direction)
        }
    }

    private inner class TouchpadListener : TouchpadView.Listener {
        override fun onReadyRequired() {
            toast(getString(R.string.ble_not_ready))
        }
    }

    private fun onBleButtonClicked() {
        when (bleClient.state) {
            FlipperBleClient.State.READY,
            FlipperBleClient.State.CONNECTED,
            FlipperBleClient.State.CONNECTING,
            -> bleClient.disconnect()

            FlipperBleClient.State.DISCONNECTED,
            FlipperBleClient.State.ERROR,
            -> connectConfigured()
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectConfigured() {
        val mac = prefs.flipperMac
        if (mac.isNullOrBlank()) {
            toast(getString(R.string.settings_mac_required))
            startActivity(Intent(this, SettingsActivity::class.java))
            return
        }
        if (!ensurePermissions()) return
        bleClient.connectAddress(mac)
    }

    private fun renderBleState(state: FlipperBleClient.State, message: String) {
        binding.txtBleStatus.text = when (state) {
            FlipperBleClient.State.READY -> getString(R.string.ble_ready)
            FlipperBleClient.State.CONNECTING -> getString(R.string.ble_connecting)
            FlipperBleClient.State.CONNECTED -> getString(R.string.ble_connected)
            FlipperBleClient.State.ERROR -> message
            FlipperBleClient.State.DISCONNECTED -> getString(R.string.ble_disconnected)
        }

        val color = when (state) {
            FlipperBleClient.State.READY -> "#4CAF50".toColorInt()
            FlipperBleClient.State.CONNECTING,
            FlipperBleClient.State.CONNECTED,
            -> "#FF9800".toColorInt()
            FlipperBleClient.State.ERROR -> "#F44336".toColorInt()
            FlipperBleClient.State.DISCONNECTED -> "#777777".toColorInt()
        }

        (binding.bleDot.background as? GradientDrawable)?.setColor(color)
            ?: binding.bleDot.setBackgroundColor(color)
    }

    private fun ensurePermissions(): Boolean {
        val needed = BlePermissions.missing(this)
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed)
            return false
        }
        return true
    }

    private fun enterFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val LAYOUT_BANNER_MS = 1200L
    }
}

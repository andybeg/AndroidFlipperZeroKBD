package com.flipperzero.androidkeyboard

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.flipperzero.androidkeyboard.ble.BlePermissions
import com.flipperzero.androidkeyboard.ble.FlipperBleClient
import com.flipperzero.androidkeyboard.databinding.ActivitySettingsBinding
import com.flipperzero.androidkeyboard.keyboard.KeyboardLayoutLoader
import com.flipperzero.androidkeyboard.keyboard.LayoutInfo
import com.flipperzero.androidkeyboard.prefs.AppPreferences

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: AppPreferences
    private var devices: List<BluetoothDevice> = emptyList()
    private var catalog: List<LayoutInfo> = emptyList()
    private var selectedDeviceIndex: Int = -1
    private val layoutChecks = mutableListOf<Pair<LayoutInfo, CheckBox>>()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results.values.all { it }) {
            refreshDevices()
        } else {
            Toast.makeText(this, R.string.ble_permission_required, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = AppPreferences(this)
        catalog = KeyboardLayoutLoader.loadCatalog(this)
        updateCurrentMacLabel()
        bindLayoutChecks()

        binding.btnRefresh.setOnClickListener { ensurePermissionsAndRefresh() }
        binding.btnSave.setOnClickListener { saveAll() }

        ensurePermissionsAndRefresh()
    }

    private fun bindLayoutChecks() {
        binding.listLayouts.removeAllViews()
        layoutChecks.clear()
        val enabled = prefs.enabledLayoutIds(catalog).toSet()
        catalog.forEach { info ->
            val box = CheckBox(this).apply {
                text = info.title
                isChecked = info.id in enabled
                textSize = 16f
            }
            binding.listLayouts.addView(box)
            layoutChecks += info to box
        }
    }

    private fun ensurePermissionsAndRefresh() {
        val needed = BlePermissions.missing(this, BlePermissions.requiredForBondedDevices())
        if (needed.isEmpty()) {
            refreshDevices()
        } else {
            permissionLauncher.launch(needed)
        }
    }

    @SuppressLint("MissingPermission")
    private fun refreshDevices() {
        devices = FlipperBleClient.bondedFlipperDevices(this)
        binding.listDevices.removeAllViews()
        selectedDeviceIndex = -1

        if (devices.isEmpty()) {
            Toast.makeText(this, R.string.settings_no_devices, Toast.LENGTH_LONG).show()
            return
        }

        val group = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
        }
        val saved = prefs.flipperMac
        devices.forEachIndexed { index, device ->
            val name = device.name ?: "Flipper"
            val button = RadioButton(this).apply {
                id = index
                text = "$name\n${device.address}"
                textSize = 15f
            }
            group.addView(button)
            if (saved != null && device.address.equals(saved, ignoreCase = true)) {
                selectedDeviceIndex = index
                button.isChecked = true
            }
        }
        group.setOnCheckedChangeListener { _, checkedId ->
            selectedDeviceIndex = checkedId
        }
        binding.listDevices.addView(group)
    }

    @SuppressLint("MissingPermission")
    private fun saveAll() {
        val enabledIds = layoutChecks
            .filter { (_, box) -> box.isChecked }
            .map { (info, _) -> info.id }

        if (enabledIds.isEmpty()) {
            Toast.makeText(this, R.string.settings_layouts_required, Toast.LENGTH_SHORT).show()
            return
        }
        prefs.setEnabledLayoutIds(enabledIds)

        if (selectedDeviceIndex in devices.indices) {
            prefs.flipperMac = devices[selectedDeviceIndex].address
            updateCurrentMacLabel()
        } else if (prefs.flipperMac.isNullOrBlank()) {
            Toast.makeText(this, R.string.settings_select_device, Toast.LENGTH_SHORT).show()
            return
        }

        if (prefs.currentLayoutId !in enabledIds) {
            prefs.currentLayoutId = enabledIds.first()
        }

        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun updateCurrentMacLabel() {
        val mac = prefs.flipperMac
        binding.txtCurrentMac.text = if (mac.isNullOrBlank()) {
            getString(R.string.settings_mac_none)
        } else {
            getString(R.string.settings_mac_current, mac)
        }
    }
}

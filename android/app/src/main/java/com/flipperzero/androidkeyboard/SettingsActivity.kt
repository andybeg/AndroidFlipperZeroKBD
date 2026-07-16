package com.flipperzero.androidkeyboard

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.flipperzero.androidkeyboard.ble.BlePermissions
import com.flipperzero.androidkeyboard.ble.FlipperBleClient
import com.flipperzero.androidkeyboard.databinding.ActivitySettingsBinding
import com.flipperzero.androidkeyboard.prefs.AppPreferences

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: AppPreferences
    private var devices: List<BluetoothDevice> = emptyList()
    private var selectedIndex: Int = -1

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
        updateCurrentMacLabel()

        binding.btnRefresh.setOnClickListener { ensurePermissionsAndRefresh() }
        binding.btnSave.setOnClickListener { saveSelected() }
        binding.listDevices.setOnItemClickListener { _, _, position, _ ->
            selectedIndex = position
        }

        ensurePermissionsAndRefresh()
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
        val labels = devices.map { device ->
            val name = device.name ?: "Flipper"
            "$name\n${device.address}"
        }
        binding.listDevices.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_single_choice,
            labels,
        )

        val saved = prefs.flipperMac
        if (saved != null) {
            val index = devices.indexOfFirst { it.address.equals(saved, ignoreCase = true) }
            if (index >= 0) {
                selectedIndex = index
                binding.listDevices.setItemChecked(index, true)
            }
        }

        if (devices.isEmpty()) {
            Toast.makeText(this, R.string.settings_no_devices, Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun saveSelected() {
        val index = selectedIndex.takeIf { it >= 0 } ?: binding.listDevices.checkedItemPosition
        if (index < 0 || index >= devices.size) {
            Toast.makeText(this, R.string.settings_select_device, Toast.LENGTH_SHORT).show()
            return
        }
        prefs.flipperMac = devices[index].address
        updateCurrentMacLabel()
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

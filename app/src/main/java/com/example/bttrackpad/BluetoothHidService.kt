package com.example.bttrackpad

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import java.util.concurrent.Executors

@SuppressLint("MissingPermission")
class BluetoothHidService(private val context: Context) {

    private var bluetoothHidDevice: BluetoothHidDevice? = null
    private var connectedDevices = mutableListOf<BluetoothDevice>()

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                bluetoothHidDevice = proxy as BluetoothHidDevice
                registerApp()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                bluetoothHidDevice = null
            }
        }
    }

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            super.onConnectionStateChanged(device, state)
            Log.d("HidService", "Connection state changed: $state")
            if (state == BluetoothProfile.STATE_CONNECTED) {
                if (!connectedDevices.contains(device)) {
                    connectedDevices.add(device)
                }
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                connectedDevices.remove(device)
            }
        }

        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            super.onAppStatusChanged(pluggedDevice, registered)
            Log.d("HidService", "App registration status: $registered")
        }
    }

    init {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        adapter?.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
    }

    private fun registerApp() {
        val sdp = BluetoothHidDeviceAppSdpSettings(
            "BTTrackpad",
            "Android Trackpad",
            "Android",
            BluetoothHidDevice.SUBCLASS1_MOUSE,
            HidDescriptor.MOUSE_REPORT_DESCRIPTOR
        )
        bluetoothHidDevice?.registerApp(sdp, null, null, Executors.newSingleThreadExecutor(), callback)
    }

    fun sendMouseReport(buttons: Int, x: Int, y: Int, wheel: Int) {
        val report = byteArrayOf(
            buttons.toByte(),
            x.coerceIn(-127, 127).toByte(),
            y.coerceIn(-127, 127).toByte(),
            wheel.coerceIn(-127, 127).toByte()
        )
        connectedDevices.forEach { device ->
            bluetoothHidDevice?.sendReport(device, HidDescriptor.ID_MOUSE, report)
        }
    }

    fun isSupported(): Boolean {
        return BluetoothAdapter.getDefaultAdapter()?.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE) != false
    }

    fun unregister() {
        bluetoothHidDevice?.unregisterApp()
        BluetoothAdapter.getDefaultAdapter()?.closeProfileProxy(BluetoothProfile.HID_DEVICE, bluetoothHidDevice)
    }
}

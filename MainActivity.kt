package com.example.tvlinkremote

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executor

class MainActivity : Activity() {
    private var hid: BluetoothHidDevice? = null
    private var connectedHost: BluetoothDevice? = null
    private var registered = false
    private lateinit var status: TextView
    private lateinit var bluetoothAdapter: BluetoothAdapter

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registeredNow: Boolean) {
            runOnUiThread {
                registered = registeredNow
                status.text = if (registeredNow) {
                    "HID ready. On your TV, open Bluetooth pairing and select this phone."
                } else {
                    "HID is not registered. Tap Enable Bluetooth HID."
                }
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            runOnUiThread {
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    connectedHost = device
                    status.text = "Connected to TV/device"
                } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    if (connectedHost == device) connectedHost = null
                    status.text = if (registered) "HID ready. Pair from your TV." else "Disconnected"
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        buildUi()
        if (bluetoothAdapter == null) {
            status.text = "This phone does not support Bluetooth."
            return
        }
        requestBluetoothPermissionIfNeeded()
        bluetoothAdapter.getProfileProxy(this, profileListener, BluetoothProfile.HID_DEVICE)
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hid = proxy as BluetoothHidDevice
                status.text = "Bluetooth HID profile available. Tap Enable Bluetooth HID."
            }
        }
        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) hid = null
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(20))
            setBackgroundColor(0xFF07111F.toInt())
        }
        val title = TextView(this).apply {
            text = "TV Link Remote"
            textSize = 28f
            setTextColor(0xFFFFFFFF.toInt())
        }
        status = TextView(this).apply {
            text = "Checking Bluetooth HID support…"
            textSize = 14f
            setTextColor(0xFFB7C7DD.toInt())
            setPadding(0, dp(12), 0, dp(16))
        }
        root.addView(title)
        root.addView(status)

        root.addView(actionButton("Enable Bluetooth HID") {
            enableHid()
        })
        root.addView(actionButton("Make phone discoverable") {
            @Suppress("DEPRECATION")
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300), 100)
        })
        root.addView(sectionTitle("Keyboard"))
        val textInput = EditText(this).apply {
            hint = "Type a message or search…"
            textSize = 16f
            setSingleLine(true)
        }
        root.addView(textInput)
        root.addView(actionButton("Send text to TV") {
            val value = textInput.text?.toString().orEmpty()
            if (value.isEmpty()) toast("Enter some text first")
            else {
                value.forEach { sendCharacter(it) }
                sendKeyUp()
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(textInput.windowToken, 0)
            }
        })
        root.addView(sectionTitle("TV controls"))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("Home", "Back", "Enter").forEach { label ->
            val b = actionButton(label) {
                when (label) {
                    "Home" -> sendKey(0x4A) // HID consumer/home support varies by TV
                    "Back" -> sendKey(0x2A) // Backspace fallback
                    else -> sendKey(0x28)
                }
            }
            row.addView(b, LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                setMargins(dp(3), dp(4), dp(3), dp(4))
            })
        }
        root.addView(row)
        root.addView(sectionTitle("Touchpad"))
        val pad = TextView(this).apply {
            text = "Touch here to move the cursor\n\nLeft tap = click • Swipe = move"
            gravity = Gravity.CENTER
            textSize = 16f
            setTextColor(0xFFEAF2FF.toInt())
            setBackgroundColor(0xFF14253A.toInt())
            setPadding(dp(12), dp(16), dp(12), dp(16))
            minimumHeight = dp(180)
            var lastX = 0f
            var lastY = 0f
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> { lastX = event.x; lastY = event.y; true }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = ((event.x - lastX) / 2).toInt().coerceIn(-127, 127)
                        val dy = ((event.y - lastY) / 2).toInt().coerceIn(-127, 127)
                        sendMouse(dx, dy, 0)
                        lastX = event.x; lastY = event.y
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        sendMouse(0, 0, 1)
                        sendMouse(0, 0, 0)
                        true
                    }
                    else -> true
                }
            }
        }
        root.addView(pad)
        val scroll = android.widget.ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun enableHid() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            toast("Bluetooth HID Device needs Android 9 or newer.")
            return
        }
        val profile = hid
        if (profile == null) {
            toast("Bluetooth HID profile is not available on this phone.")
            return
        }
        if (!hasConnectPermission()) {
            requestBluetoothPermissionIfNeeded()
            return
        }
        try {
            val sdp = BluetoothHidDeviceAppSdpSettings(
                "TV Link Remote",
                "Bluetooth keyboard and mouse for Android TV",
                "TV Link Remote",
                BluetoothHidDevice.SUBCLASS1_COMBO,
                HID_REPORT_DESCRIPTOR
            )
            profile.registerApp(sdp, null, null, Executor { command -> runOnUiThread(command) }, hidCallback)
            status.text = "Registering HID device…"
        } catch (e: SecurityException) {
            status.text = "Bluetooth permission denied. Allow Nearby devices and try again."
        } catch (e: Exception) {
            status.text = "Could not register HID: ${e.message ?: "unsupported"}"
        }
    }

    private fun sendCharacter(ch: Char) {
        val key = hidUsage(ch) ?: return
        val modifier = if (ch.isUpperCase()) 0x02 else 0x00
        sendKeyboard(modifier, key)
        sendKeyUp()
    }

    private fun hidUsage(ch: Char): Int? {
        if (ch in 'a'..'z') return ch.code - 'a'.code + 0x04
        if (ch in 'A'..'Z') return ch.code - 'A'.code + 0x04
        if (ch in '1'..'9') return ch.code - '1'.code + 0x1E
        if (ch == '0') return 0x27
        return when (ch) {
            ' ' -> 0x2C
            '\n', '\r' -> 0x28
            '-' -> 0x2D
            '_' -> 0x2D
            '=' -> 0x2E
            '.' -> 0x37
            ',' -> 0x36
            '/' -> 0x38
            ':' -> 0x33
            else -> null
        }
    }

    private fun sendKey(usage: Int) {
        sendKeyboard(0, usage)
        sendKeyUp()
    }

    private fun sendKeyboard(modifier: Int, key: Int) {
        val device = connectedHost ?: return
        val report = byteArrayOf(modifier.toByte(), 0, key.toByte(), 0, 0, 0, 0, 0)
        try { hid?.sendReport(device, 1, report) } catch (_: Exception) {}
    }

    private fun sendKeyUp() {
        val device = connectedHost ?: return
        try { hid?.sendReport(device, 1, ByteArray(8)) } catch (_: Exception) {}
    }

    private fun sendMouse(dx: Int, dy: Int, buttons: Int) {
        val device = connectedHost ?: return
        val report = byteArrayOf(buttons.toByte(), dx.toByte(), dy.toByte(), 0)
        try { hid?.sendReport(device, 2, report) } catch (_: Exception) {}
    }

    private fun hasConnectPermission(): Boolean =
        Build.VERSION.SDK_INT < 31 ||
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private fun requestBluetoothPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 31 && !hasConnectPermission()) {
            requestPermissions(arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            ), 501)
        }
    }

    private fun actionButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
        }

    private fun sectionTitle(value: String) = TextView(this).apply {
        text = value
        textSize = 18f
        setTextColor(0xFF70A7FF.toInt())
        setPadding(0, dp(16), 0, dp(6))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun toast(value: String) = Toast.makeText(this, value, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
            bluetoothAdapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, hid)
        } catch (_: Exception) {}
    }

    companion object {
        // Report ID 1 = standard boot-style keyboard report (modifier + reserved + 6 keys)
        // Report ID 2 = mouse buttons + X + Y + wheel
        private val HID_REPORT_DESCRIPTOR = byteArrayOf(
            0x05,0x01, 0x09,0x06, 0xA1.toByte(),0x01, 0x85.toByte(),0x01,
            0x05,0x07, 0x19,0xE0.toByte(), 0x29,0xE7.toByte(), 0x15,0x00, 0x25,0x01,
            0x75,0x01, 0x95,0x08, 0x81.toByte(),0x02, 0x95,0x01, 0x75,0x08, 0x81.toByte(),0x01,
            0x95,0x06, 0x75,0x08, 0x15,0x00, 0x25,0x65, 0x05,0x07, 0x19,0x00, 0x29,0x65, 0x81.toByte(),0x00,
            0xC0.toByte(),
            0x05,0x01, 0x09,0x02, 0xA1.toByte(),0x01, 0x85.toByte(),0x02,
            0x09,0x01, 0xA1.toByte(),0x00, 0x05,0x09, 0x19,0x01, 0x29,0x03,
            0x15,0x00, 0x25,0x01, 0x95,0x03, 0x75,0x01, 0x81.toByte(),0x02,
            0x95,0x01, 0x75,0x05, 0x81.toByte(),0x01,
            0x05,0x01, 0x09,0x30, 0x09,0x31, 0x09,0x38, 0x15,0x81.toByte(), 0x25,0x7F,
            0x75,0x08, 0x95,0x03, 0x81.toByte(),0x06, 0xC0.toByte(), 0xC0.toByte()
        )
    }
}

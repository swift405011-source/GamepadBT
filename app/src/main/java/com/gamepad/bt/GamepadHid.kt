package com.gamepad.bt

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

@SuppressLint("MissingPermission")
class GamepadHid(
    private val context: Context,
    private val onState: (String) -> Unit
) {
    companion object {
        const val BTN_A = 0
        const val BTN_B = 1
        const val BTN_X = 3
        const val BTN_Y = 4
        const val BTN_LB = 6
        const val BTN_RB = 7
        const val BTN_LT = 8
        const val BTN_RT = 9
        const val BTN_BACK = 10
        const val BTN_START = 11
        const val BTN_L3 = 13
        const val BTN_R3 = 14

        private val DESCRIPTOR: ByteArray = intArrayOf(
            0x05, 0x01, 0x09, 0x05, 0xA1, 0x01,
            0x05, 0x09, 0x19, 0x01, 0x29, 0x10,
            0x15, 0x00, 0x25, 0x01, 0x75, 0x01, 0x95, 0x10, 0x81, 0x02,
            0x05, 0x01, 0x09, 0x39,
            0x15, 0x00, 0x25, 0x07, 0x35, 0x00, 0x46, 0x3B, 0x01,
            0x65, 0x14, 0x75, 0x04, 0x95, 0x01, 0x81, 0x42,
            0x65, 0x00, 0x75, 0x04, 0x95, 0x01, 0x81, 0x03,
            0x05, 0x01, 0x09, 0x30, 0x09, 0x31, 0x09, 0x32, 0x09, 0x35,
            0x15, 0x81, 0x25, 0x7F, 0x35, 0x00, 0x45, 0x00,
            0x75, 0x08, 0x95, 0x04, 0x81, 0x02,
            0xC0
        ).map { it.toByte() }.toByteArray()
    }

    private var hid: BluetoothHidDevice? = null
    private var host: BluetoothDevice? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private val adapter: BluetoothAdapter =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            onState(if (registered) "Pronto. Escolha o aparelho abaixo." else "Gamepad não registrado")
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    host = device
                    onState("Conectado a " + device.name)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    host = null
                    onState("Desconectado")
                }
            }
        }
    }

    fun start() {
        adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    val h = proxy as BluetoothHidDevice
                    hid = h
                    val sdp = BluetoothHidDeviceAppSdpSettings(
                        "GamepadBT", "Gamepad", "GamepadBT",
                        0x08.toByte(), DESCRIPTOR
                    )
                    h.registerApp(sdp, null, null, executor, callback)
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                hid = null
            }
        }, BluetoothProfile.HID_DEVICE)
    }

    fun bonded(): List<BluetoothDevice> = adapter.bondedDevices.toList()

    fun connect(device: BluetoothDevice) {
        onState("Conectando a " + device.name + "...")
        hid?.connect(device)
    }

    fun send(buttons: Int, hat: Int, lx: Int, ly: Int, rx: Int, ry: Int) {
        val d = host ?: return
        val h = if (hat == 0) 8 else hat - 1
        val r = byteArrayOf(
            (buttons and 0xFF).toByte(),
            ((buttons shr 8) and 0xFF).toByte(),
            h.toByte(),
            lx.toByte(), ly.toByte(), rx.toByte(), ry.toByte()
        )
        hid?.sendReport(d, 0, r)
    }

    fun tap(bit: Int) {
        send(1 shl bit, 0, 0, 0, 0, 0)
        handler.postDelayed({ send(0, 0, 0, 0, 0, 0) }, 100)
    }
}

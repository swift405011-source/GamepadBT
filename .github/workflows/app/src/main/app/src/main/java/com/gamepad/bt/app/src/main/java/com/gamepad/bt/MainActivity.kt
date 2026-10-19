package com.gamepad.bt

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

@SuppressLint("MissingPermission")
class MainActivity : Activity() {

    private lateinit var pad: GamepadHid
    private lateinit var status: TextView
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        status = TextView(this).apply {
            text = "Iniciando..."
            textSize = 18f
            setPadding(32, 32, 32, 32)
        }
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val testBtn = Button(this).apply {
            text = "Teste: apertar A"
            setOnClickListener { pad.tap(GamepadHid.BTN_A) }
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(status)
            addView(testBtn)
            addView(list)
        }
        setContentView(ScrollView(this).apply { addView(root) })

        pad = GamepadHid(this) { msg -> runOnUiThread { status.text = msg } }

        if (needsPermission()) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1)
        } else {
            startPad()
        }
    }

    private fun needsPermission(): Boolean =
        Build.VERSION.SDK_INT >= 31 &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startPad()
        } else {
            status.text = "Sem permissão de Bluetooth"
        }
    }

    private fun startPad() {
        pad.start()
        for (device in pad.bonded()) {
            val b = Button(this).apply {
                text = device.name ?: device.address
                setOnClickListener { pad.connect(device) }
            }
            list.addView(b)
        }
    }
}

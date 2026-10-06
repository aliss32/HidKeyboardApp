package com.hidkeyboard.app

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.Toast
import java.io.DataOutputStream

class MainActivity : Activity() {
    private var os: DataOutputStream? = null
    private var process: Process? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#121212"))
        }

        val grid = GridLayout(this).apply {
            columnCount = 3
            alignmentMode = GridLayout.ALIGN_BOUNDS
        }

        val keys = listOf(
            Pair("ESC", "29"), Pair("F2", "3b"), Pair("DEL", "4c"),
            Pair("F10", "43"), Pair("F12", "45"), Pair("ENTER", "28"),
            Pair("YUKARI", "52"), Pair("ASAGI", "51"), Pair("SOL", "50"),
            Pair("SAG", "4f"), Pair("A", "04"), Pair("Y", "1c")
        )

        for ((label, code) in keys) {
            val btn = Button(this).apply {
                text = label
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#333333"))
                textSize = 18f
                setOnClickListener { sendKey(code) }
            }
            val params = GridLayout.LayoutParams().apply {
                width = 250
                height = 180
                setMargins(15, 15, 15, 15)
            }
            grid.addView(btn, params)
        }

        layout.addView(grid)
        setContentView(layout)

        initRoot()
    }

    private fun initRoot() {
        try {
            process = Runtime.getRuntime().exec("su")
            os = DataOutputStream(process!!.outputStream)
            os?.writeBytes("setenforce 0\n")
            os?.writeBytes("chmod 666 /dev/hidg0\n")
            os?.flush()
            Toast.makeText(this, "Root Yetkisi Aktif!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Root Hatasi! Cihaz Rootlu Degil.", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendKey(hexCode: String) {
        try {
            os?.writeBytes("echo -ne '\\x00\\x00\\x$hexCode\\x00\\x00\\x00\\x00\\x00' ^> /dev/hidg0\n")
            os?.writeBytes("echo -ne '\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00' ^> /dev/hidg0\n")
            os?.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            os?.close()
            process?.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

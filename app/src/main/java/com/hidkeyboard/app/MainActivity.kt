package com.hidkeyboard.app

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.Toast
import java.io.DataOutputStream
import java.io.File

class MainActivity : Activity() {
    private var os: DataOutputStream? = null
    private var process: Process? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Titreşim servisini başlat
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

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
            Pair("YUKARI", "52"), Pair("AŞAĞI", "51"), Pair("SOL", "50"),
            Pair("SAĞ", "4f"), Pair("A", "04"), Pair("Y", "1c")
        )

        for ((label, code) in keys) {
            val btn = Button(this).apply {
                text = label
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#333333"))
                textSize = 18f
                setOnClickListener { 
                    vibratePhone() // Titreşim tetikle
                    sendKey(code)  // Tuşu PC'ye gönder
                }
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

    private fun vibratePhone() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(40)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initRoot() {
        try {
            process = Runtime.getRuntime().exec("su")
            os = DataOutputStream(process!!.outputStream)
            
            // SELinux kısıtlamalarını kaldır
            os?.writeBytes("setenforce 0\n")
            
            // HATA KONTROLÜ: Kernel düzeyinde USB HID cihazı oluşmuş mu?
            if (File("/dev/hidg0").exists()) {
                os?.writeBytes("chmod 666 /dev/hidg0\n")
                os?.flush()
                Toast.makeText(this, "BAŞARILI: Root ve HID Sürücüsü Aktif!", Toast.LENGTH_LONG).show()
            } else {
                // Eğer bu hatayı görüyorsan, PC telefonu klavye olarak görmüyor demektir.
                Toast.makeText(this, "HATA: /dev/hidg0 bulunamadı! USB Gadget Tool'dan HID profili açılmamış.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Root Hatası! Cihaz Rootlu Değil.", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendKey(hexCode: String) {
        try {
            os?.writeBytes("echo -ne '\\x00\\x00\\x$hexCode\\x00\\x00\\x00\\x00\\x00' > /dev/hidg0\n")
            os?.writeBytes("echo -ne '\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00' > /dev/hidg0\n")
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

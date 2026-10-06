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
                    vibratePhone() 
                    sendKey(code) 
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

        // Arka planda donanımı dinamik olarak yapılandır
        Thread { initConfigFSDynamically() }.start()
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

    private fun initConfigFSDynamically() {
        try {
            process = Runtime.getRuntime().exec("su")
            os = DataOutputStream(process!!.outputStream)
            
            // Çekirdeği okuyup dinamik olarak USB HID yapılandıran Bash Betiği
            val setupScript = """
                # SELinux kısıtlamalarını kaldır
                setenforce 0
                
                # UDC (USB Donanım Çipi) adını dinamik olarak bul
                UDC_NAME=${'$'}(ls /sys/class/udc | head -n 1)
                
                # Eğer UDC boşsa kernel ConfigFS desteklemiyor demektir
                if [ -z "${'$'}UDC_NAME" ]; then
                    exit 1
                fi
                
                # Android'in varsayılan USB bağlantısını (MTP vb.) zorla durdur
                setprop sys.usb.config none
                sleep 1
                
                # Yeni bir USB Gadget (Donanım) oluştur
                GADGET_DIR=/config/usb_gadget/bios_kb
                mkdir -p ${'$'}GADGET_DIR
                cd ${'$'}GADGET_DIR
                
                # Sahte Klavye Kimliği (Logitech/Generic PC Klavyesi)
                echo 0x1d6b > idVendor
                echo 0x0104 > idProduct
                
                mkdir -p strings/0x409
                echo "Android" > strings/0x409/manufacturer
                echo "BIOS Keyboard" > strings/0x409/product
                
                mkdir -p configs/c.1/strings/0x409
                echo "HID Config" > configs/c.1/strings/0x409/configuration
                
                # HID (Klavye) Fonksiyonunu Yarat
                mkdir -p functions/hid.usb0
                echo 1 > functions/hid.usb0/protocol
                echo 1 > functions/hid.usb0/subclass
                echo 8 > functions/hid.usb0/report_length
                
                # Standart Masaüstü Klavyesi Hex Donanım Haritası (Report Descriptor)
                echo -ne '\x05\x01\x09\x06\xa1\x01\x05\x07\x19\xe0\x29\xe7\x15\x00\x25\x01\x75\x01\x95\x08\x81\x02\x95\x01\x75\x08\x81\x03\x95\x05\x75\x01\x05\x08\x19\x01\x29\x05\x91\x02\x95\x01\x75\x03\x91\x03\x95\x06\x75\x08\x15\x00\x25\x65\x05\x07\x19\x00\x29\x65\x81\x00\xc0' > functions/hid.usb0/report_desc
                
                # Konfigürasyonu cihaza bağla
                ln -s functions/hid.usb0 configs/c.1/
                
                # USB'yi aç (Bulunan UDC'yi ata)
                echo ${'$'}UDC_NAME > UDC
                
                # Sürücü iznini ver
                chmod 666 /dev/hidg0
            """.trimIndent()

            os?.writeBytes(setupScript + "\n")
            os?.flush()
            
            // Kernel'in yapılandırmayı bitirmesi için 2 saniye bekle
            Thread.sleep(2000)

            runOnUiThread {
                if (File("/dev/hidg0").exists()) {
                    Toast.makeText(this, "BAŞARILI: Çekirdek Yapılandırıldı! Klavye Hazır.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "HATA: Kernel yapılandırılamadı (Kernelinizde HID modülü derlenmemiş olabilir).", Toast.LENGTH_LONG).show()
                }
            }

        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, "Root Hatası! Cihaz Rootlu Değil.", Toast.LENGTH_LONG).show()
            }
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
            // Uygulamadan çıkarken telefonu eski haline (Dosya Aktarımı/Şarj moduna) geri döndür
            os?.writeBytes("setprop sys.usb.config mtp,adb\n")
            os?.flush()
            os?.close()
            process?.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

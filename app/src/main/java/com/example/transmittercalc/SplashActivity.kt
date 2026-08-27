package com.example.transmittercalc

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar
import java.util.Locale

class SplashActivity : AppCompatActivity() {

    private lateinit var pwdInput: TextInputEditText
    private lateinit var txtHint: TextView
    private lateinit var txtErr: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        pwdInput = findViewById(R.id.pwdInput)
        txtHint = findViewById(R.id.txtSplashHint)
        txtErr = findViewById(R.id.txtSplashErr)

        findViewById<android.view.View>(R.id.btnUnlock).setOnClickListener { checkPassword() }

        pwdInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                txtErr.text = ""
                if (s?.length == 4 && s.toString() == currentPassword()) {
                    unlock()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun currentPassword(): String {
        val now = Calendar.getInstance()
        return String.format(Locale.US, "%02d%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
    }

    private fun checkPassword() {
        val input = pwdInput.text?.toString()?.trim().orEmpty()
        if (input == currentPassword()) {
            unlock()
        } else {
            txtErr.text = "密码错误，请重新输入"
        }
    }

    private fun unlock() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
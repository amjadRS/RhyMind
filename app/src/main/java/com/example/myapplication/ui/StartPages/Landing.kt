package com.example.myapplication.ui.StartPages

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R

class Landing : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.landing) // Match this to your XML file name.

        // Delay of 3 seconds (3000 milliseconds) before switching to WelcomeActivity
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, WelcomeOne::class.java)
            startActivity(intent)
            finish() // Close Landing activity so it won't remain in the back stack
        }, 1500)
    }
}

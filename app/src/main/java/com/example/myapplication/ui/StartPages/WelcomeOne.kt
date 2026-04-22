package com.example.myapplication.ui.StartPages

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R

class WelcomeOne : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.welcome_one) // Use the appropriate layout file name

        // Example of navigating to WelcomeTwo when an action occurs
         val learnMoreButton: Button = findViewById(R.id.learnmore)
         learnMoreButton.setOnClickListener {
             val intent = Intent(this, WelcomeTwo::class.java)
             startActivity(intent)
        }
    }
}

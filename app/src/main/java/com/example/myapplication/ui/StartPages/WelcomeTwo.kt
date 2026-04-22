package com.example.myapplication.ui.StartPages

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.ui.auth.LoginActivity
import com.example.myapplication.ui.auth.RegisterActivity

class WelcomeTwo : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.welcome_two) // Use the appropriate layout file name

        // Example of navigating to WelcomeTwo when an action occurs
        val learnMoreButton: Button = findViewById(R.id.getstarted)
        learnMoreButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}

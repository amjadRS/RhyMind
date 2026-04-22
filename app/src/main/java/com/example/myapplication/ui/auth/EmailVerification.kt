package com.example.myapplication.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.ui.dashboard.DashboardActivity
import com.google.firebase.auth.FirebaseAuth

class EmailVerification : AppCompatActivity() {

    private lateinit var verificationMessageTextView: TextView
    private lateinit var resendButton: Button
    private lateinit var back: ImageButton

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_verification)
        Log.d("EmailVerification", "onCreate")

        verificationMessageTextView = findViewById(R.id.verificationMessage)
        resendButton = findViewById(R.id.resendVerificationBtn)
        back = findViewById(R.id.backButton)

        val userEmail = intent.getStringExtra("user_email")

        if (userEmail != null) {
            verificationMessageTextView.text = "A verification link has been sent to $userEmail. Please verify your email."
        }

        // Disable the resend button until the timer ends
        resendButton.isEnabled = false

        // Start the countdown timer
        startTimer()
        resendButton.setOnClickListener {
            resendButton.isEnabled = false
            auth.currentUser?.sendEmailVerification()
            startTimer()
        }
        back.setOnClickListener{
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()

        }

    }

    private fun redirectToDashboardWithAnimation() {
        // Use a smooth transition to the dashboard activity with an animation
        showToast("Your email is verified")
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    override fun onResume() {
        super.onResume()
        auth.currentUser?.reload()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                if (user != null && user.isEmailVerified) {
                    print("verified")
                    redirectToDashboardWithAnimation()
                }
            } else {
                print("not verified")
            }
        }
    }


    private fun startTimer() {
        // Start a 45-second countdown timer
        val timerText: TextView = findViewById(R.id.timerText)
        var timeRemaining = 40

        val timer = object : CountDownTimer(40000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerText.text = "Resend email in: $timeRemaining s"
                timeRemaining--
            }

            override fun onFinish() {
                // Enable the button when the timer finishes
                resendButton.isEnabled = true
                timerText.text = "You can resend the email now."
            }
        }

        timer.start()
    }

}

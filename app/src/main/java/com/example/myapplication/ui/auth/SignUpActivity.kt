package com.example.myapplication.ui.auth

import com.example.myapplication.ui.models.UserModel
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var username: EditText
    private lateinit var firstName: EditText
    private lateinit var lastName: EditText
    private lateinit var phoneNo: EditText
    private lateinit var signUpButton: Button
    private lateinit var clickableLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.signup)

        signUpButton = findViewById(R.id.signUp_button)
        auth = FirebaseAuth.getInstance()
        firstName = findViewById(R.id.first_name)
        lastName = findViewById(R.id.last_name)
        email = findViewById(R.id.email)
        username = findViewById(R.id.username)
        password = findViewById(R.id.password)
        phoneNo = findViewById(R.id.phone_no)
        clickableLogin = findViewById(R.id.login_clickable)

        clickableLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
        signUpButton.setOnClickListener {
            Toast.makeText(this, "Button clicked!", Toast.LENGTH_SHORT).show()

            val firstNameInput = firstName.text.toString().trim()
            val lastNameInput = lastName.text.toString().trim()
            val emailInput = email.text.toString().trim()
            val phoneNoInput = phoneNo.text.toString().trim()
            val usernameInput = username.text.toString().trim()
            val passwordInput = password.text.toString().trim()

            // Validate input before proceeding
            if (validateInput(firstNameInput, lastNameInput, emailInput, phoneNoInput, usernameInput, passwordInput)) {
                auth.createUserWithEmailAndPassword(emailInput, passwordInput)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Log.d("SignUpActivity", "User created successfully")

                            val user = auth.currentUser
                            val uid = user?.uid ?: ""

                            if (uid.isNotEmpty()) {
                                user?.sendEmailVerification()
                                    ?.addOnCompleteListener { verificationTask ->
                                        if (verificationTask.isSuccessful) {
                                            Log.d("SignUpActivity", "Email verification sent.")
                                        } else {
                                            Log.e(
                                                "SignUpActivity",
                                                "Email verification failed: ${verificationTask.exception?.message}"
                                            )
                                        }
                                    }

                                val userModel = UserModel(
                                    userId = uid,
                                    firstName = firstNameInput,
                                    lastName = lastNameInput,
                                    email = emailInput,
                                    phoneNumber = phoneNoInput,
                                    username = usernameInput,
                                    password = passwordInput,
                                    createdAt = Timestamp.now(),
                                    profileImage = "",
                                    categories = listOf("Work","Family","Sport")
                                )

                                // Save user model to Firestore
                                val db = FirebaseFirestore.getInstance()
                                db.collection("users").document(uid)
                                    .set(userModel.toSignUpMap())
                                    .addOnSuccessListener {
                                        Log.d("SignUpActivity", "User data saved to Firestore")
                                        val intent = Intent(this, EmailVerification::class.java)
                                        intent.putExtra("user_email", emailInput)
                                        startActivity(intent)
                                        finish()
                                    }
                                    .addOnFailureListener { exception ->
                                        Log.e(
                                            "SignUpActivity",
                                            "Error saving user data: ${exception.message}"
                                        )
                                        Toast.makeText(
                                            this,
                                            "Error saving user data: ${exception.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                Toast.makeText(this, "Sign-up successful!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Log.e("SignUpActivity", "Sign-up failed: ${task.exception?.message}")
                            Toast.makeText(
                                this,
                                "Sign-up failed: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        }

    }


    private fun validateInput(
        firstname: String,
        lastname: String,
        email: String,
        phoneNo: String,
        username: String,
        password: String
    ): Boolean {
        return when {
            firstname.isEmpty() -> {
                firstName.error = "Please enter your first name"
                false
            }
            !firstname.matches(Regex("^[A-Za-z]+$")) -> {
                firstName.error = "First name must only contain alphabetic characters"
                false
            }
            firstname.length < 2 -> {
                firstName.error = "First name must be at least 2 characters"
                false
            }
            lastname.isEmpty() -> {
                lastName.error = "Please enter your last name"
                false
            }
            !lastname.matches(Regex("^[A-Za-z]+$")) -> {
                lastName.error = "Last name must only contain alphabetic characters"
                false
            }
            lastname.length < 2 -> {
                lastName.error = "Last name must be at least 2 characters"
                false
            }
            email.isEmpty() -> {
                this.email.error = "Please enter your email"
                false
            }
            !isValidEmail(email) -> {
                this.email.error = "Invalid email format"
                false
            }
            phoneNo.isEmpty() -> {
                this.phoneNo.error = "Please enter your phone number"
                false
            }
            !isValidPhoneNumber(phoneNo) -> {
                this.phoneNo.error =
                    "PhoneNo. must start country code or network provider and remaining (7 digits)"
                false
            }
            username.isEmpty() -> {
                this.username.error = "Please enter a username"
                false
            }
            username.length !in 3..15 -> {
                this.username.error = "Username must be between 3 and 15 characters"
                false
            }
            password.isEmpty() -> {
                this.password.error = "Please enter a valid password"
                false
            }
            password.length < 6 -> {
                this.password.error = "Password must be at least 6 characters"
                false
            }
            else -> true
        }
    }

    // Email Validation - Strong regex pattern
    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        return email.matches(Regex(emailPattern))
    }

    // Phone number validation (Jordan formats)
    private fun isValidPhoneNumber(phoneNo: String): Boolean {
        return phoneNo.matches(Regex("^(\\+962|00962|0)(77|78|79)\\d{7}\$"))
    }
}

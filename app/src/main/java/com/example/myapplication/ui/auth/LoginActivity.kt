package com.example.myapplication.ui.auth

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.R
import com.example.myapplication.ui.dashboard.DashboardActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var usernameOrEmail: EditText
    private lateinit var password: EditText
    private lateinit var loginButton: Button
    private lateinit var googleSignInButton: Button
    private lateinit var signUpLink: TextView
    private lateinit var forgotPasswordLink: TextView
    private lateinit var progressBar: ProgressBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContentView(R.layout.login)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize Google Sign-In options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_web_client_id)) // Ensure this is configured correctly
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize UI components
        usernameOrEmail = findViewById(R.id.login_email)
        password = findViewById(R.id.login_password)
        loginButton = findViewById(R.id.loginbutton)
        googleSignInButton = findViewById(R.id.googleSignInButton)
        signUpLink = findViewById(R.id.SignUp)
        forgotPasswordLink = findViewById(R.id.forgotpassword)
        progressBar = findViewById(R.id.progressBar)

        // Set up login button click listener
        loginButton.setOnClickListener {
            val emailInput = usernameOrEmail.text.toString().trim()
            val passwordInput = password.text.toString().trim()

            if (validateInput(emailInput, passwordInput)) {
                showProgressBar(true)
                loginUser(emailInput, passwordInput)
                showProgressBar(false)
            }
        }

        // Set up Google Sign-In button click listener
        googleSignInButton.setOnClickListener {
            showProgressBar(true)
            signInWithGoogle()
        }

        // Navigate to SignUpActivity when SignUp link is clicked
        signUpLink.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        // Handle forgot password logic
        forgotPasswordLink.setOnClickListener {
            val intent = Intent(this, ForgotActivity::class.java)
            startActivity(intent)
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleGoogleSignInResult(task)
        } else {
            showProgressBar(false)
            Toast.makeText(this, "Google Sign-In canceled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.result
            if (account != null && account.idToken != null) {
                updateUIWithGoogleAccount(account)
            } else {
                showProgressBar(false)
                Toast.makeText(this, "Google Sign-In failed. Try again.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Google Sign-In failed: ${e.message}")
            showProgressBar(false)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUIWithGoogleAccount(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            showProgressBar(false)
            if (task.isSuccessful) {
                Log.d("LoginActivity", "Login successful")
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, DashboardActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Log.e("LoginActivity", "Login failed: ${task.exception?.message}")
                Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loginUser(usernameOrEmail: String, password: String) {
        if (isValidEmail(usernameOrEmail)) {
            // Login using email
            auth.signInWithEmailAndPassword(usernameOrEmail, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d("LoginActivity", "Login successful")
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                        val user = auth.currentUser

                        if (user != null && user.isEmailVerified) {
                            val intent = Intent(this, DashboardActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            auth.currentUser?.sendEmailVerification()
                            val intent = Intent(this, EmailVerification::class.java)
                            intent.putExtra("user_email", usernameOrEmail)
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        Log.e("LoginActivity", "Login failed: credentials does not match")
                        Toast.makeText(
                            this,
                            "Login failed: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        } else {
            // If it's not a valid email or alternative, login using username in Firestore
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").whereEqualTo("username", usernameOrEmail).get()

            userRef.addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // Username not found
                    Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show()
                } else {
                    // If a document is found with the username, retrieve the email and log the user in
                    val email = documents.firstOrNull()?.getString("email")
                    email?.let {
                        auth.signInWithEmailAndPassword(it, password)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    Log.d("LoginActivity", "Login successful")
                                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                                    val user = auth.currentUser
                                    val uid = user?.uid ?: ""
                                    val db = FirebaseFirestore.getInstance()
                                    val userRef = db.collection("users").document(uid)
                                    if (user != null && user.isEmailVerified) {
                                        val intent = Intent(this, DashboardActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    } else {
                                        auth.currentUser?.sendEmailVerification()
                                        val intent = Intent(this, EmailVerification::class.java)
                                        intent.putExtra("user_email", email)
                                        startActivity(intent)
                                        finish()
                                    }
                                } else {
                                    Log.e("LoginActivity", "Login failed: ${task.exception?.message}")
                                    Toast.makeText(
                                        this,
                                        "Login failed: ${task.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    } ?: run {
                        // If email is null or missing, handle accordingly (optional)
                        Toast.makeText(
                            this,
                            "Error: Email not associated with this username",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }.addOnFailureListener { exception ->
                // Handle error case
                Log.e("LoginActivity", "Error fetching user: ${exception.message}")
                Toast.makeText(this, "Error fetching user data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateInput(usernameOrEmail: String, password: String): Boolean {
        return when {
            usernameOrEmail.isEmpty() -> {
                this.usernameOrEmail.error = "Please enter your email or username"
                false
            }

            !isValidEmail(usernameOrEmail) && usernameOrEmail.length < 3 -> {
                this.usernameOrEmail.error = "Please enter a valid username or email"
                false
            }

            password.isEmpty() -> {
                this.password.error = "Please enter your password"
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

    private fun showProgressBar(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
}
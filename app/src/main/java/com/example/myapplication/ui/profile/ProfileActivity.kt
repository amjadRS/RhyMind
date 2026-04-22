package com.example.myapplication.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.example.myapplication.ui.models.UserModel
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import java.util.UUID


class ProfileActivity : AppCompatActivity() {

    private lateinit var storage: FirebaseStorage
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var firstName: EditText
    private lateinit var lastName: EditText
    private lateinit var email: EditText
    private lateinit var phoneNumber: EditText
    private lateinit var updateButton: Button
    private lateinit var changeProfileImage: ImageView
    private lateinit var profileImage: CircleImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile) // Fix: Ensure correct XML layout

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        profileImage = findViewById(R.id.profile_image)

        // Bind UI elements
        firstName = findViewById(R.id.etfname)
        lastName = findViewById(R.id.etlname)
        email = findViewById(R.id.etEmail)
        phoneNumber = findViewById(R.id.etphone)
        updateButton = findViewById(R.id.UpdateButton)
        changeProfileImage = findViewById(R.id.Change_Profiles)

        // Initialize the profileImage view
        profileImage = findViewById(R.id.profile_image)

        // Load user profile
        loadUserProfile()

        // Handle update button click
        updateButton.setOnClickListener {
            updateUserProfile()
        }

        // Handle change profile image click
        changeProfileImage.setOnClickListener {
            openImagePicker()
        }
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val user = document.toObject(UserModel::class.java)
                        if (user != null) {
                            firstName.setText(user.firstName)
                            lastName.setText(user.lastName)
                            email.setText(user.email)
                            phoneNumber.setText(user.phoneNumber)

                            val profileImageUrl = user.profileImage
                            if (!profileImageUrl.isNullOrEmpty()) {
                                Glide.with(this).load(profileImageUrl).into(profileImage)
                            }
                        }
                    } else {
                        Toast.makeText(this, "User profile not found.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileActivity", "Error fetching profile: ${e.message}")
                    Toast.makeText(this, "Error fetching profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUserProfile() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val updates = mutableMapOf<String, Any>()

            if (firstName.text.toString().trim().isNotEmpty()) {
                updates["firstName"] = firstName.text.toString().trim()
            }
            if (lastName.text.toString().trim().isNotEmpty()) {
                updates["lastName"] = lastName.text.toString().trim()
            }
            if (email.text.toString().trim().isNotEmpty()) {
                updates["email"] = email.text.toString().trim()
            }
            if (phoneNumber.text.toString().trim().isNotEmpty()) {
                updates["phoneNumber"] = phoneNumber.text.toString().trim()
            }

            if (updates.isNotEmpty()) {
                db.collection("users").document(userId).update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Profile updated successfully.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Log.e("ProfileActivity", "Error updating profile: ${e.message}")
                        Toast.makeText(this, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "No changes to update.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"

        imagePickerLauncher.launch(intent)
    }

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val selectedImageUri = result.data?.data
                selectedImageUri?.let { uri ->
                    uploadImageToFirebase(uri)
                } ?: run {
                    Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Image selection canceled", Toast.LENGTH_SHORT).show()
            }
        }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        val fileName = "profile_images/${userId}_${UUID.randomUUID()}.jpg"
        val storageRef = storage.reference.child(fileName)

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    saveImageUrlToFirestore(uri.toString())
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileActivity", "Error uploading image: ${e.message}")
                Toast.makeText(this, "Error uploading image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveImageUrlToFirestore(imageUrl: String) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .update("profileImage", imageUrl)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile image updated successfully.", Toast.LENGTH_SHORT).show()
                Glide.with(this).load(imageUrl).into(profileImage)

                finish()
            }
            .addOnFailureListener { e ->
                Log.e("ProfileActivity", "Error saving image URL: ${e.message}")
                Toast.makeText(this, "Error saving image URL: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

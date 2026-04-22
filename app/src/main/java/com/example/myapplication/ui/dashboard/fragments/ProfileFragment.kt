package com.example.myapplication.ui.dashboard.fragments
import com.example.myapplication.ui.models.UserModel
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {
    private lateinit var user: UserModel
    private lateinit var firstName: EditText
    private lateinit var lastName: EditText
    private lateinit var email: EditText
    private lateinit var phoneNumber: EditText
    private lateinit var updateButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val rootView = inflater.inflate(R.layout.profile, container, false)


        // Bind UI elements
        firstName = rootView.findViewById(R.id.etfname)
        lastName = rootView.findViewById(R.id.etlname)
        email = rootView.findViewById(R.id.etEmail)
        phoneNumber = rootView.findViewById(R.id.etphone)
        updateButton = rootView.findViewById(R.id.UpdateButton)
        updateButton.setOnClickListener {
            updateUserInFirestore()
        }
        // Fetch current user from Firestore
        fetchUserData()

        return rootView
    }


    private fun fetchUserData() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.data != null) {
                        // Use fromMap to create com.example.myapplication.ui.models.UserModel instance
                        user = UserModel.fromMap(document.data!!)

                        // Update UI with user data
                        firstName.setText(user.firstName)
                        lastName.setText(user.lastName)
                        email.setText(user.email)
                        phoneNumber.setText(user.phoneNumber)
                    } else {
                        Toast.makeText(context, "User data not found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error fetching user data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "No user logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUserInFirestore() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val db = FirebaseFirestore.getInstance()

            // Get updated values from UI
            val updatedUser = UserModel(
                firstName = firstName.text.toString(),
                lastName = lastName.text.toString(),
                email = email.text.toString(),
                phoneNumber = phoneNumber.text.toString(),
                username = user.username,
                profileImage = ""
            )

            // Update Firestore document
            db.collection("users").document(uid)
                .set(updatedUser.toSignUpMap())
                .addOnSuccessListener {
                    Toast.makeText(context, "User updated successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error updating user: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "No user logged in", Toast.LENGTH_SHORT).show()
        }
    }

}
package com.example.myapplication.ui.models
import com.google.firebase.Timestamp

data class UserModel(
    val userId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val username: String = "",
    val password: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val profileImage: String = "",
    val categories: List<String> = listOf("Work", "Family", "Sport")
) {
    // Updated logic for toSignUpMap()
    fun toSignUpMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "phoneNumber" to phoneNumber,
            "username" to username,
            "password" to password,
            "createdAt" to createdAt,
            "profileImage" to profileImage,
            "categories" to categories
        )
    }

    companion object {
        fun fromMap(data: Map<String, Any>): UserModel {
            return UserModel(
                userId = data["userId"] as? String ?: "",
                firstName = data["firstName"] as? String ?: "",
                lastName = data["lastName"] as? String ?: "",
                email = data["email"] as? String ?: "",
                phoneNumber = data["phoneNumber"] as? String ?: "",
                username = data["username"] as? String ?: "",
                password = data["password"] as? String ?: "",
                createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
                profileImage = data["profileImage"] as? String ?: "", // Default value used here
                categories = (data["categories"] as? List<*>)?.filterIsInstance<String>() ?: listOf("Work", "Family", "Sport")
            )
        }
    }
}

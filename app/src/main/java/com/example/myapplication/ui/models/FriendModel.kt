package com.example.myapplication.ui.models

import com.google.firebase.Timestamp

data class FriendModel (
    val id : String,
    val username: String,
    val status: String,
    val addedDate: Timestamp? = Timestamp.now()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "username" to username,
            "status" to status,
            "addedDate" to addedDate
        )
    }
    companion object {
        fun fromMap(map: Map<String, Any?>): FriendModel {
            return FriendModel(
                id = map["id"] as String,
                username = map["username"] as String,
                status = map["status"] as String,
                addedDate = map["addedDate"] as? Timestamp
            )
        }
    }
}
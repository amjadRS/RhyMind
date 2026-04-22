package com.example.myapplication.ui.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class NotificationModel(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "",
    val timestamp: Long = System.currentTimeMillis(),

    @get:PropertyName("read")
    val isRead: Boolean = false
)
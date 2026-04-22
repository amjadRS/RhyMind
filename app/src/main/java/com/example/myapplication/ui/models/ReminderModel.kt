package com.example.myapplication.ui.models

import android.os.Parcelable
import com.google.firebase.Timestamp

@kotlinx.parcelize.Parcelize
data class ReminderModel(
    var eventId: String = "",
    var eventTitle: String = "",               // Title of the event
    var eventDescription: String? = null,     // Description of the event (nullable)
    var eventCategory: String = "",
    var eventDate: String = "",
    var eventStartTime: String = "",
    var eventEndTime: String = "",
    var eventLocation: String? = null,        // Optional location
    var priority: String? = null,             // Priority: Low, Medium, High
    var creationTime: Timestamp? = Timestamp.now(), // Default to current timestamp
    var weather: String? = null,              // Weather (only for outdoor events)
    var status: String? = "OnGoing",          // Status: OnGoing, Completed, Canceled
    var creatorId: String = "",
    var groupIds: List<String>? = null     // Reference to the Group ID (nullable)
) : Parcelable {
    // Function to convert ReminderModel to a map suitable for Firestore
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "eventId" to eventId,
            "eventTitle" to eventTitle,
            "eventDescription" to eventDescription,
            "eventCategory" to eventCategory,
            "eventDate" to eventDate,
            "eventStartTime" to eventStartTime,
            "eventEndTime" to eventEndTime,
            "eventLocation" to eventLocation,
            "priority" to priority,
            "creationTime" to creationTime,
            "weather" to weather,
            "status" to status,
            "creatorId" to creatorId,
            "groupId" to groupIds
        )
    }

    companion object {

        // Function to convert a map (Firestore document) back to ReminderModel
        fun fromMap(map: Map<String, Any?>): ReminderModel {
            return ReminderModel(
                eventId = map["eventId"] as? String ?: "",
                eventTitle = map["eventTitle"] as? String ?: "",
                eventDescription = map["eventDescription"] as? String,
                eventCategory = map["eventCategory"] as? String ?: "",
                eventDate = map["eventDate"] as? String ?: "",
                eventStartTime = map["eventStartTime"] as? String ?: "",
                eventEndTime = map["eventEndTime"] as? String ?: "",
                eventLocation = map["eventLocation"] as? String,
                priority = map["priority"] as? String ?: "",
                creationTime = map["creationTime"] as? Timestamp ?: Timestamp.now(),
                weather = map["weather"] as? String,
                status = map["status"] as? String ?: "",
                creatorId = map["creatorId"] as? String ?: "",
            )
        }
    }
    fun getEventOverview(): Map<String, Any?> {
        return mapOf(
            "eventTitle" to eventTitle,
            "eventDescription" to eventDescription,
            "eventStartTime" to eventStartTime,
            "eventEndTime" to eventEndTime,
        )
    }

    fun getFullEventDetails(): Map<String, Any?> {
        return mapOf(
            "eventTitle" to eventTitle,
            "eventDescription" to eventDescription,
            "eventCategory" to eventCategory,
            "eventDate" to eventDate,
            "eventStartTime" to eventStartTime,
            "eventEndTime" to eventEndTime,
            "eventLocation" to eventLocation,
            "priority" to priority,
            "creationTime" to creationTime,
            "weather" to weather,
            "status" to status,
            "creatorId" to creatorId,
            "groupId" to groupIds
        )
    }

}

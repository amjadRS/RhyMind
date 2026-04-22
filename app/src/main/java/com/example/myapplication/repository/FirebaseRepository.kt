package com.example.myapplication.repository

import android.util.Log
import com.example.myapplication.ui.models.GroupModel
import com.example.myapplication.ui.models.NotificationModel
import com.example.myapplication.ui.models.UserModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await


class FirebaseRepository {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()


    suspend fun getUsers(): List<UserModel> {
        return try {
            val documents = firestore.collection("users").get().await()
            documents.mapNotNull { it.toObject(UserModel::class.java) }
        } catch (e: Exception) {
            Log.e("FirebaseError", "Error fetching users: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchFriends(currentUserId: String): List<UserModel> {
        return try {
            // Fetch the friend IDs from the "friends" subcollection
            val friendsRef = firestore.collection("users")
                .document(currentUserId)
                .collection("friends")
            val snapshot = friendsRef.get().await()

            //Filter friends id's based on status = "Friends"
            val friendIds = snapshot.documents
                .filter { document -> document.getString("status") == "Friends" }
                .map { it.id }

            if (friendIds.isNotEmpty()) {

                val userSnapshot = firestore.collection("users")
                    .whereIn(FieldPath.documentId(), friendIds)
                    .get().await()

                // Map the documents to UserModel objects
                userSnapshot.documents.mapNotNull { document ->
                    document.toObject(UserModel::class.java)
                }
            } else {
                emptyList()
            }
        } catch (exception: Exception) {
            // Log the error and return an empty list in case of failure
            Log.e("FirebaseRepository", "Error fetching friends: ${exception.message}")
            emptyList()
        }
    }


    fun getCurrentId(): String? {
        return try {
            auth.currentUser?.uid
        } catch (e: Exception) {
            Log.e("getCurrentId", "Error fetching user ID: ${e.message}")
            null
        }
    }

    suspend fun removeFriend(currentUserId: String, friendId: String): Result<String> {
        return try {

            val friendDocRef = firestore.collection("users")
                .document(currentUserId)
                .collection("friends")
                .document(friendId)


            friendDocRef.delete().await()

            val documentSnapshot = friendDocRef.get().await()
            if (documentSnapshot.exists()) {
                Result.failure(Exception("Friend was not removed"))
            } else {
                Result.success("Friend removed")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUser(userId: String): UserModel? {
        return try {

            val doc = firestore.collection("users").document(userId).get().await()

            if (doc.exists()) {
                doc.toObject(UserModel::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("Fetch User:", "Error fetching user", e)
            null
        }
    }

    suspend fun addFriend(currentUserId: String, user: UserModel): Result<String> {
        return try {
            val friendDoc = firestore.collection("users")
                .document(currentUserId)
                .collection("friends")
                .document(user.userId)
                .get()
                .await()
            if (friendDoc.exists()) {
                Result.failure(Exception("Friend is already added."))
            } else {
                val friend = mapOf(
                    "id" to user.userId,
                    "username" to user.username,
                    "addedDate" to FieldValue.serverTimestamp(),
                    "status" to "Pending"
                )

                firestore.collection("users")
                    .document(currentUserId)
                    .collection("friends")
                    .document(user.userId)
                    .set(friend)
                    .await()


                val currentUserDoc = firestore.collection("users")
                    .document(currentUserId)
                    .get()
                    .await()

                val firstname = currentUserDoc.getString("firstName")
                val lastname = currentUserDoc.getString("lastName")
                val senderName = "$firstname $lastname"

                sendFriendRequestNotification(currentUserId, senderName, user.userId)

                Result.success("Friend request sent")
            }
        } catch (e: Exception) {
            // Return a failure result in case of an exception
            Result.failure(e)
        }
    }

    suspend fun sendFriendRequestNotification(senderId: String, senderName: String, receiverId: String) {
        val notificationCollection = firestore.collection("notifications")
        val title = "New Friend Request"
        val type = "friend request"
        val message = "$senderName has sent a friend request !"

        val notification = NotificationModel(
            id = notificationCollection.document().id,
            senderId = senderId,
            receiverId = receiverId,
            title = title,
            message = message,
            type = type,
            timestamp = System.currentTimeMillis(),
            isRead= false,
        )

        try {
            notificationCollection
                .document(notification.id)
                .set(notification)
                .await()
        } catch (e: Exception) {
            // Handle the exception (e.g., log it, show a message to the user, etc.)
            Log.e("FriendRequestNotification", "Error sending friend request notification", e)
        }
    }

    suspend fun getUnReadNotifications(userId: String) : List<NotificationModel> {
        val notifications = mutableListOf<NotificationModel>()
        val db = FirebaseFirestore.getInstance()

        // Fetch unread notifications from Firestore
        val querySnapshot = db.collection("notifications")
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("read", false)
            .get()
            .await()

        Log.d("FirestoreQuery", "Fetched documents: ${querySnapshot.size()}")

        for (document in querySnapshot) {
            val notification = document.toObject(NotificationModel::class.java)
            notifications.add(notification)
        }
        return notifications
    }

    suspend fun getUserStatus(currentUserId: String, user: UserModel): String {
        val friendRef = firestore.collection("users")
            .document(currentUserId)
            .collection("friends")
            .document(user.userId)

        val document = friendRef.get().await()
        return if (document.exists()) {
            document.getString("status") ?: "not friends"
        } else {
            "not friends"
        }
    }

    suspend fun addGroupToFirebase(group: GroupModel) {
        try {
            // Create a reference for a new document
            val groupRef = firestore.collection("groups").document()

            // Copy the group object with a new groupId
            val groupWithId = group.copy(groupId = groupRef.id)

            // Save the group to Firestore
            groupRef.set(groupWithId).await()
            Log.d("Firebase Firestore", "Group added successfully: ${groupWithId.groupName}")
        } catch (e: Exception) {
            // Log the error with more context if needed
            Log.e("Firebase Firestore", "Error adding group: ${e.message}")
            throw e  // Optional: rethrow the exception if you want to propagate the error to the calling function
        }
    }

    suspend fun getGroupsFromFirebase(currentUserId: String): List<GroupModel>? {
        return try {
            // Query fetch groups that he created
            val snapshot = firestore.collection("groups")
                .whereEqualTo("creatorId", currentUserId)  // Filter by creatorId
                .get()
                .await()

            snapshot.documents.mapNotNull { document ->
                val group = document.toObject(GroupModel::class.java)
                group?.copy(groupId = document.id)
            }
        } catch (e: Exception) {
            Log.e("Firebase Firestore", "Error fetching groups for creatorId $currentUserId: ${e.message}")
            null
        }
    }

    suspend fun removeGroupFromFirebase(userId: String, groupId: String) {
        try {
            val snapshot = firestore.collection("groups")
                .document(groupId)
                .get()
                .await()
            if (snapshot.exists()) {
                val creatorId = snapshot.getString("creatorId")

                if (creatorId == userId) {
                    firestore.collection("groups")
                        .document(groupId)
                        .delete()
                        .await()
                    Log.d("Firebase Firestore", "Group removed successfully")
                } else
                    Log.e("Firebase Firestore", "Only creator of the group can delete group")
            } else
                Log.e("Firebase Firestore", "Group not found")
        } catch (e: Exception) {
            Log.e("Firebase Firestore", "Error removing group: ${e.message}")
        }
    }

    suspend fun getOtherGroupsFromFirebase(currentUserId: String): List<GroupModel>? {
        return try {
            // Query the groups the user is other groups
            val snapshot = firestore.collection("groups")
                .whereArrayContains(
                    "members.id",
                    currentUserId
                )
                .get()
                .await()

            snapshot.documents.mapNotNull { document ->
                val group = document.toObject(GroupModel::class.java)
                group?.copy(groupId = document.id)
            }
        } catch (e: Exception) {
            Log.e("Firebase Firestore", "Error fetching groups for user: ${e.message}")
            null
        }
    }
}

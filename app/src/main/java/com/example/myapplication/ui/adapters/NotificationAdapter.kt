package com.example.myapplication.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.ui.models.NotificationModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions


class NotificationAdapter(private var notifications: MutableList<NotificationModel>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]

        holder.notificationTitle.text = notification.title
        holder.notificationMessage.text = notification.message


        holder.acceptButton.setOnClickListener {
            handleRequestProcessed(holder)
            acceptFriendRequest(notification.senderId, notification.receiverId)
            updateNotificationStatus(notification.id)
            addUserToFriends(notification.senderId)
        }

        holder.declineButton.setOnClickListener {
            handleRequestProcessed(holder)
            declineFriendRequest(notification.senderId, notification.receiverId)
            updateNotificationStatus(notification.id)
        }
    }

    private fun addUserToFriends(senderId: String) {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser?.uid
                ?: throw Exception("Current user is not logged in")


            FirebaseFirestore.getInstance().collection("users")
                .document(senderId)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userSnapshot = task.result
                        if (userSnapshot != null && userSnapshot.exists()) {
                            val userId = userSnapshot.id
                            val username = userSnapshot.getString("username") ?: "Unknown"

                            val friend = mapOf(
                                "id" to userId,
                                "username" to username,
                                "addedDate" to FieldValue.serverTimestamp(),
                                "status" to "Friends"
                            )

                            // Add friend to current user's friends collection
                            FirebaseFirestore.getInstance().collection("users")
                                .document(currentUser)
                                .collection("friends")
                                .document(senderId)
                                .set(friend)
                                .addOnSuccessListener {
                                    Log.d("Friendship", "User $senderId added to friends.")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Friendship", "Error adding friend: ${e.message}")
                                }
                        } else {
                            Log.e("Friendship", "User not found in Firestore.")
                        }
                    } else {
                        Log.e("Friendship", "Failed to fetch user: ${task.exception?.message}")
                    }
                }
        } catch (e: Exception) {
            Log.e("AddUserToFriends", "Error adding user to friends: ${e.message}")
        }
    }

    private fun updateNotificationStatus(id: String) {
        val notifications = FirebaseFirestore.getInstance().collection("notifications")
        notifications.document(id).set(mapOf("read" to true), SetOptions.merge())
    }

    override fun getItemCount(): Int = notifications.size

    private fun handleRequestProcessed(holder: NotificationViewHolder) {
        holder.acceptButton.visibility = View.GONE
        holder.declineButton.visibility = View.GONE
    }

    private fun acceptFriendRequest(senderId: String, receiverId: String) {
        val friendRef = FirebaseFirestore.getInstance().collection("users")

        friendRef.document(senderId).collection("friends").document(receiverId)
            .set(mapOf("status" to "Friends"), SetOptions.merge())
            .addOnSuccessListener {
                Log.d("NotificationAdapter", "Friend request accepted")
            }
            .addOnFailureListener { e ->
                Log.e("NotificationAdapter", "Failed to accept friend request", e)
            }


    }

    private fun declineFriendRequest(senderId: String, receiverId: String) {
        val friendRef = FirebaseFirestore.getInstance().collection("users")

        friendRef.document(senderId).collection("friends").document(receiverId)
            .delete()
            .addOnSuccessListener {
                Log.d("NotificationAdapter", "Friend request declined")
            }
            .addOnFailureListener { e ->
                Log.e("NotificationAdapter", "Failed to decline friend request", e)
            }
    }

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val notificationTitle: TextView = itemView.findViewById(R.id.notification_title)
        val notificationMessage: TextView = itemView.findViewById(R.id.notification_message)
        val acceptButton: LinearLayout = itemView.findViewById(R.id.accept_layout)
        val declineButton: LinearLayout = itemView.findViewById(R.id.decline_layout)
    }

    fun updateNotifications(newNotifications: List<NotificationModel>) {
        Log.d("NotificationAdapter", "Updating notifications: ${newNotifications.size}")
        Log.d("NotificationAdapter", "Notifications: $newNotifications")  // Log the actual contents
        this.notifications.clear()  // Clear old notifications if necessary
        this.notifications.addAll(newNotifications)  // Add new notifications
        notifyDataSetChanged()
    }
}

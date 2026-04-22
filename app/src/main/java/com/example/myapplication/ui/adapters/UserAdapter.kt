package com.example.myapplication.ui.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.databinding.NewFriendsCardViewBinding
import com.example.myapplication.ui.models.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class UserAdapter(private val onAddFriendClicked: (UserModel) -> Unit) :
    ListAdapter<UserModel, UserAdapter.NewFriendViewHolder>(UserDiffCallback()) {

     val firestore = FirebaseFirestore.getInstance()
     private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewFriendViewHolder {
        val binding = NewFriendsCardViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NewFriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewFriendViewHolder, position: Int) {
        val user = getItem(position)  // Use getItem() to get the current item
        // Check friend status for the current user and this user
        if (currentUserId != null) {
            val friendRef = firestore.collection("users")
                .document(currentUserId)
                .collection("friends")
                .document(user.userId)

            friendRef.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    if (document.exists()) {
                        val status = document.getString("status") ?: "not friends"
                        holder.bind(user, status)
                    } else {
                        holder.bind(user, "not friends")
                    }
                } else {

                    print("Error getting friend status: ${task.exception}")
                    holder.bind(user, "not friends")
                }
            }
        }
    }
    override fun getItemCount(): Int = currentList.size  // Use currentList from ListAdapter

    // ViewHolder class to bind data
    inner class NewFriendViewHolder(private val binding: NewFriendsCardViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: UserModel,status: String) {
            binding.username.text = user.username
            binding.userFullName.text = "${user.firstName}  ${user.lastName}"
            binding.userEmail.text = user.email
            val profileImageUri = user.profileImage
            if (profileImageUri.isEmpty()) {
                // Set a default image if the URI is empty
                binding.profileIcon.setImageResource(R.drawable.profile_icon)
            } else {
                // Set the image from the URI
                Glide.with(binding.profileIcon.context)
                    .load(profileImageUri)
                    .circleCrop()
                    .into(binding.profileIcon)
            }

            when (status) {
                "Pending", "Friends" -> {
                    binding.addFriendButton.visibility = View.GONE
                    binding.statusLabel.text = status
                    binding.statusLabel.visibility = View.VISIBLE
                }
                else -> {
                    binding.addFriendButton.visibility = View.VISIBLE
                    binding.statusLabel.visibility = View.GONE
                    println("$status  ok")
                }
            }

            binding.addFriendButton.setOnClickListener {
                binding.addFriendButton.visibility = View.GONE
                binding.statusLabel.text = "Pending"
                binding.statusLabel.visibility = View.VISIBLE
                onAddFriendClicked(user)
            }
        }
    }


    class UserDiffCallback : DiffUtil.ItemCallback<UserModel>() {
        override fun areItemsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {

            return oldItem.userId == newItem.userId  // Compare by unique ID
        }

        override fun areContentsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {

            return oldItem == newItem  // Compare all fields
        }
    }
}
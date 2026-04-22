package com.example.myapplication.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.FriendCardViewBinding
import com.example.myapplication.ui.models.UserModel

class FriendsAdapter : ListAdapter<UserModel, FriendsAdapter.FriendsViewHolder>(FriendsDiffCallback()) {

    private val expandedPositions = mutableSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendsViewHolder {
        val binding = FriendCardViewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FriendsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: FriendsViewHolder, position: Int, payloads: MutableList<Any>) {
        super.onBindViewHolder(holder, position, payloads)
        // Handle the update only if there's a payload (expand/collapse state change)
        if (payloads.isNotEmpty()) {
            val isExpanded = expandedPositions.contains(position)
            holder.binding.expandableSection.visibility = if (isExpanded) View.VISIBLE else View.GONE
            holder.binding.expandCollapseIcon.setImageResource(
                if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )
        }
    }

    inner class FriendsViewHolder(val binding: FriendCardViewBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(friend: UserModel) {
            binding.friendFn.text = friend.firstName
            binding.friendLn.text = friend.lastName
            binding.friendEmail.text = friend.email
            binding.friendPhoneNo.text = friend.phoneNumber
            binding.friendRelationship.visibility = View.GONE
            binding.friendAddedDate.visibility = View.GONE

            // Set the initial visibility based on expandedPositions
            val isExpanded = expandedPositions.contains(bindingAdapterPosition)
            binding.expandableSection.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.expandCollapseIcon.setImageResource(
                if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )

            // Set click listener for expand/collapse icon
            binding.expandCollapseIcon.setOnClickListener {
                if (isExpanded) {
                    // Collapse the section
                    expandedPositions.remove(bindingAdapterPosition)
                    binding.expandableSection.visibility = View.GONE
                } else {
                    // Expand the section
                    expandedPositions.add(bindingAdapterPosition)
                    binding.expandableSection.visibility = View.VISIBLE
                }

                // Notify item change with a payload to trigger partial update
                notifyItemChanged(bindingAdapterPosition, "expand_collapse")
            }
        }
    }
    fun getItemAt(position: Int): UserModel {
        val item = getItem(position)
        return item
    }

    class FriendsDiffCallback : DiffUtil.ItemCallback<UserModel>() {
        override fun areItemsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem == newItem
        }
    }
}


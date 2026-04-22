package com.example.myapplication.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.GroupCardViewBinding
import com.example.myapplication.ui.models.GroupModel

class GroupAdapter(
    private val groupList: MutableList<GroupModel>,
    private val onDeleteGroup: (GroupModel) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    private val expandedPositions = mutableSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = GroupCardViewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groupList[position]
        val isExpanded = expandedPositions.contains(position)

        with(holder.binding) {
            groupName.text = group.groupName
            groupDescription.text = group.groupDescription
            groupCategory.text = group.groupCategory
            memberCount.text = "Members: ${group.memberCount}"
            groupTimestamp.text = "Created on: ${group.timeStamp?.toDate()}"

            // Expandable visibility
            expandableSection.visibility = if (isExpanded) View.VISIBLE else View.GONE
            expandCollapseIcon.setImageResource(
                if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )

            // Toggle expand/collapse
            expandCollapseIcon.setOnClickListener {
                if (isExpanded) {
                    expandedPositions.remove(position)
                } else {
                    expandedPositions.add(position)
                }
                notifyItemChanged(position)
            }

            // Delete icon functionality
            deleteGroupIcon.setOnClickListener {
                onDeleteGroup(group)
            }
        }
    }

    override fun getItemCount() = groupList.size

    fun removeGroupById(groupId: String) {
        groupList.removeAll { it.groupId == groupId }
        notifyDataSetChanged() // Notify RecyclerView of the change
    }

    fun updateData(groups: List<GroupModel>) {
        groupList.clear()
        groupList.addAll(groups)
        notifyDataSetChanged() // Notify RecyclerView that the data has changed
    }

    // Update the member count for a specific group
    fun updateMemberCount(groupId: String, newMemberCount: Int) {
        val group = groupList.find { it.groupId == groupId }
        group?.let {
            it.memberCount = newMemberCount
            val position = groupList.indexOf(it)
            notifyItemChanged(position)  // Notify that the item has been updated
        }
    }

    fun addGroup(group: GroupModel) {
        groupList.add(group)
        notifyItemInserted(groupList.size - 1)
    }

    class GroupViewHolder(val binding: GroupCardViewBinding) :
        RecyclerView.ViewHolder(binding.root)
}

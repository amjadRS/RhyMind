package com.example.myapplication.ui.adapters


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.MembersCardViewBinding
import com.example.myapplication.ui.models.MemberModel

class AddedMembersAdapter(
    private val members: MutableList<MemberModel>
) : RecyclerView.Adapter<AddedMembersAdapter.MemberViewHolder>() {

    inner class MemberViewHolder(val binding: MembersCardViewBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = MembersCardViewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = members[position]

        // Set member name
        holder.binding.memberName.text = member.name

        // Set up the Spinner for roles
        val roles = listOf("Admin", "Member")
        val roleAdapter = ArrayAdapter(
            holder.binding.root.context,
            R.layout.simple_spinner_item,
            roles
        )
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.binding.roleSpinner.adapter = roleAdapter

        // Handle null roles and default to "Member"
        val defaultRole = "Member"
        val currentRoleIndex = member.role.let { roles.indexOf(it).takeIf { index -> index >= 0 } }
            ?: roles.indexOf(defaultRole)
        holder.binding.roleSpinner.setSelection(currentRoleIndex)

        // Update member role when a new role is selected
        holder.binding.roleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                member.role = roles[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No action needed
            }
        }
    }

    override fun getItemCount(): Int {
        return members.size
    }
}
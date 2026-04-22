package com.example.myapplication.ui.dashboard.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentUserGroupsBinding
import com.example.myapplication.repository.FirebaseRepository
import com.example.myapplication.ui.adapters.GroupAdapter
import com.example.myapplication.ui.models.GroupModel
import com.example.myapplication.ui.models.MemberModel
import com.example.myapplication.ui.models.UserModel
import com.example.myapplication.viewmodel.GroupsViewModel
import com.example.myapplication.viewmodel.GroupsViewModelFactory
import com.google.android.material.tabs.TabLayout
import com.google.firebase.Timestamp


class UserGroupsFragment : Fragment() {

    private val viewModel: GroupsViewModel by viewModels {
        GroupsViewModelFactory(FirebaseRepository())
    }

    private var _binding: FragmentUserGroupsBinding? = null
    private val binding get() = _binding!!

    private lateinit var groupAdapter: GroupAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUserGroupsBinding.inflate(inflater, container, false)




        return binding.root  // Return the root view of the fragment (via ViewBinding)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val membersAdapter = MemberAdapter()

        groupAdapter = GroupAdapter(
            groupList = mutableListOf(),
            onDeleteGroup = { group ->

                AlertDialog.Builder(requireContext())
                    .setTitle("Confirm Deletion")
                    .setMessage("Are you sure you want to delete this group?")
                    .setPositiveButton("Yes") { dialog, _ ->
                        viewModel.removeGroup(group.groupId)
                        groupAdapter.removeGroupById(group.groupId)
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        // If user cancels, dismiss the dialog
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }
        )



        binding.groupsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.groupsRecyclerView.adapter = groupAdapter

        setupTabs()


        viewModel.friends.observe(viewLifecycleOwner) { friendsList ->
            print("Before: $friendsList")
            if (friendsList != null && friendsList.isNotEmpty()) {
                println(friendsList.toString())
                membersAdapter.setMembers(friendsList)
            } else {
                Toast.makeText(context, "No friends found", Toast.LENGTH_SHORT).show()
            }
        }


        viewModel.groups.observe(viewLifecycleOwner) { groupList ->
            groupAdapter.updateData(groupList)
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->

            Toast.makeText(requireContext(), "Error: $error", Toast.LENGTH_SHORT).show()
        }

        viewModel.success.observe(viewLifecycleOwner) { success ->
            Toast.makeText(requireContext(), " $success", Toast.LENGTH_SHORT).show()
        }

        viewModel.loadFriends()
        viewModel.loadMyGroups()
        viewModel.loadOtherGroups()

        binding.fabAddGroup.setOnClickListener{

            showAddGroupDialog(membersAdapter)
        }
    }

    private fun setupTabs() {
        val tabs = listOf("My Groups", "Other Groups", "Favorites")
        tabs.forEach { tabName ->
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(tabName))
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    when (it.position) {
                        0 -> viewModel.myGroups.observe(viewLifecycleOwner) { myGroups ->
                            viewModel.setCurrentGroups(myGroups)
                        }
                        1 -> viewModel.otherGroups.observe(viewLifecycleOwner) { otherGroups ->
                            viewModel.setCurrentGroups(otherGroups)
                        }
                        2 -> Toast.makeText(requireContext(), "Will be added Later", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Load default tab
        if (groupAdapter.itemCount == 0) {
            viewModel.myGroups.observe(viewLifecycleOwner) { myGroups ->
                viewModel.setCurrentGroups(myGroups)
            }
        }
    }

    private fun showAddGroupDialog(membersAdapter: MemberAdapter) {
        val dialogView = layoutInflater.inflate(R.layout.fragment_add_group, null)

        val groupNameEditText = dialogView.findViewById<EditText>(R.id.editTextGroupName)
        val groupDescriptionEditText = dialogView.findViewById<EditText>(R.id.editTextGroupDescription)
        val groupCategorySpinner = dialogView.findViewById<Spinner>(R.id.groupCategorySpinner)
        val membersRecyclerView = dialogView.findViewById<RecyclerView>(R.id.listViewMembers)
        val addedMembersRecyclerView = dialogView.findViewById<RecyclerView>(R.id.addedViewMembers)
        val saveButton = dialogView.findViewById<Button>(R.id.buttonSaveGroup)
        val addMembersButton = dialogView.findViewById<Button>(R.id.addMembers)

        // Populate Spinner with categories
        val categories = listOf("Work", "Friends", "Family", "Sports", "Other")
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        groupCategorySpinner.adapter = categoryAdapter
        groupCategorySpinner.setBackgroundResource(R.drawable.spinner_boarder)



        membersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        membersRecyclerView.adapter = membersAdapter

        val addedMembersAdapter = AddedMemberAdapter()
        addedMembersRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        addedMembersRecyclerView.adapter = addedMembersAdapter

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        addMembersButton.setOnClickListener {
            val selectedMembers = membersAdapter.getSelectedMembers()
            membersAdapter.removeMembers(selectedMembers)
            addedMembersAdapter.submitList(selectedMembers)
            Toast.makeText(context, "${selectedMembers.size} members added", Toast.LENGTH_SHORT).show()
        }

        saveButton.setOnClickListener {
            val groupName = groupNameEditText.text.toString()
            val groupDescription = groupDescriptionEditText.text.toString()
            val groupCategory = groupCategorySpinner.selectedItem.toString()
            val currentUserId = FirebaseRepository().getCurrentId()

            val newGroup = currentUserId?.let { userId ->
                GroupModel(
                    groupName = groupName,
                    groupDescription = groupDescription,
                    groupCategory = groupCategory,
                    memberCount = addedMembersAdapter.itemCount,
                    groupId = "",
                    eventId = "",
                    creatorId = userId,
                    members = addedMembersAdapter.getAddedMembers(),
                    timeStamp = Timestamp.now()
                )
            }

            newGroup?.let { viewModel.addGroup(it)
                groupAdapter.addGroup(it) }

            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


class AddedMemberAdapter : RecyclerView.Adapter<AddedMemberAdapter.AddedMemberViewHolder>() {

    private val addedMembers = mutableListOf<MemberModel>()

    fun getAddedMembers(): List<MemberModel> = addedMembers

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddedMemberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_added_member, parent, false)
        return AddedMemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: AddedMemberViewHolder, position: Int) {
        val member = addedMembers[position]
        holder.bind(member)
    }

    override fun getItemCount(): Int = addedMembers.size

    // Manually update the list
    fun submitList(newMembers: List<MemberModel>) {
        addedMembers.clear()  // Clear old list
        addedMembers.addAll(newMembers)  // Add new members to list
        notifyDataSetChanged()  // Notify the adapter to update the RecyclerView
    }

    inner class AddedMemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val memberNameTextView: TextView = itemView.findViewById(R.id.memberName)
        private val roleTextView: TextView = itemView.findViewById(R.id.memberRole)
        private val editButton: ImageButton = itemView.findViewById(R.id.edit_icon)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.delete_icon)

        fun bind(member: MemberModel) {
            memberNameTextView.text = member.name
            roleTextView.text = member.role

            // Handle edit action
            editButton.setOnClickListener {
                // This is where you handle the editing
                // You can either modify the member locally or open a new screen/dialog for editing
                editMember(member)
            }

            // Handle delete action
            deleteButton.setOnClickListener {
                val selectedMember = getMemberAt(bindingAdapterPosition)
                removeMemberAt(bindingAdapterPosition)
                //re-add member to MembersAdapter as UserModel  ------------------> using backup
            }
        }

        private fun editMember(member: MemberModel) {
            // Handle editing logic, e.g., open a dialog or an activity to edit the member
            // Example: Show a dialog to edit member details, or pass data to another screen
            Toast.makeText(itemView.context, "Editing ${member.name}", Toast.LENGTH_SHORT).show()

            // If you need to update the list, you can modify the item and notify the change
            val updatedMember = member.copy(name = "New Name") // Modify as needed
            val position = bindingAdapterPosition
            addedMembers[position] = updatedMember
            notifyItemChanged(position)  // Update the item in RecyclerView
        }

        private fun removeMemberAt(position: Int) {
            addedMembers.removeAt(position)
            notifyItemRemoved(position)
        }
        private fun getMemberAt(bindingAdapterPosition: Int): MemberModel {
            return addedMembers[bindingAdapterPosition]
        }
    }

}


class MemberAdapter : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

    private val members = mutableListOf<UserModel>()
    // ----------------------------->   back up for deleted UserModels
    private val backUpMembers = mutableListOf<UserModel>()
    private val selectedMembers = mutableListOf<MemberModel>()

    fun getSelectedMembers(): List<MemberModel> = selectedMembers

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.members_card_view, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = members[position]
        holder.bind(member)
    }

    override fun getItemCount(): Int = members.size

    fun setMembers(newMembers: List<UserModel>) {
        members.clear()
        members.addAll(newMembers)
        backUpMembers.addAll(newMembers)
        notifyDataSetChanged()
    }

    fun removeMembers(selectedMembers: List<MemberModel>) {
        // Filter out members whose id matches any selectedMember id
        val updatedMembers = members.filterNot { user ->
            selectedMembers.any { selectedMember -> selectedMember.id == user.userId}
        }

        // Update the adapter with the filtered list
        setMembers(updatedMembers)
    }

    inner class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val memberNameTextView: TextView = itemView.findViewById(R.id.memberName)
        private val roleSpinner: Spinner = itemView.findViewById(R.id.role_spinner)
        private val checkBoxSelectMember: CheckBox = itemView.findViewById(R.id.checkBoxSelectMember)

        fun bind(user: UserModel) {
            memberNameTextView.text = "${user.firstName} ${user.lastName}"
            val roles = arrayOf("Admin", "Member")
            val roleAdapter = ArrayAdapter(itemView.context, android.R.layout.simple_spinner_item, roles)
            roleSpinner.adapter = roleAdapter
            roleSpinner.setSelection(roles.indexOf("Member"))

            checkBoxSelectMember.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    val role = roles[roleSpinner.selectedItemPosition]
                    val memberModel = MemberModel(
                        name = "${user.firstName} ${user.lastName}",
                        role = role,
                        id = user.userId
                    )
                    selectedMembers.add(memberModel)
                } else {
                    selectedMembers.removeIf { it.id == user.userId }
                }
            }
        }
    }
}
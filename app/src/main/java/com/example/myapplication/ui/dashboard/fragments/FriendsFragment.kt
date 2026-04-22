package com.example.myapplication.ui.dashboard.fragments

import android.os.Bundle
import android.util.Log


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.FragmentFriendsBinding
import com.example.myapplication.repository.FirebaseRepository
import com.example.myapplication.ui.adapters.FriendsAdapter
import com.example.myapplication.ui.adapters.UserAdapter
import com.example.myapplication.viewmodel.FriendsViewModel
import com.example.myapplication.viewmodel.FriendsViewModelFactory



class FriendsFragment : Fragment() {

    private val viewModel: FriendsViewModel by viewModels {
        FriendsViewModelFactory(FirebaseRepository())
    }

    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var friendsRecyclerView: RecyclerView
    private lateinit var friendsAdapter: FriendsAdapter
    private lateinit var usersAdapter: UserAdapter
    private var fbRepository: FirebaseRepository = FirebaseRepository()

    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendsBinding.inflate(inflater, container, false)

        usersAdapter = UserAdapter { user ->
            val currentUserId = fbRepository.getCurrentId()
            currentUserId?.let {
                viewModel.addFriend(it, user)
            }
        }


        friendsAdapter = FriendsAdapter()

        val currentUserId = fbRepository.getCurrentId()
        currentUserId?.let{
            viewModel.fetchFriends(currentUserId)
        }

        friendsRecyclerView = binding.friendsListRecyclerView
        friendsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        friendsRecyclerView.adapter = friendsAdapter

        usersRecyclerView = binding.searchedFriendsListRecyclerView
        usersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        usersRecyclerView.adapter = usersAdapter

        //fix small flick with list adapter animator
        usersRecyclerView.itemAnimator = null




        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Get the position of the swiped item
                val position = viewHolder.layoutPosition
                val friendToRemove = friendsAdapter.getItemAt(position) // Fetch the friend object at the given position

                currentUserId?.let {
                    viewModel.removeFriend(it, friendToRemove.userId)
                }
            }
        })

        itemTouchHelper.attachToRecyclerView(friendsRecyclerView)





        viewModel.friendsList.observe(viewLifecycleOwner) { friends ->
            val nonNullFriends = friends ?: emptyList()
            Log.d("friends list", "$nonNullFriends")
            friendsAdapter.submitList(nonNullFriends)
        }


        viewModel.filteredUsers.observe(viewLifecycleOwner) { users ->
            if (users.isEmpty()) {
                toggleEmptyState(true)
            } else {
                toggleEmptyState(false)
            }
            print(users.toString())
            usersAdapter.submitList(users)
        }

        //Observe friend added button
        viewModel.addedFriendResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                },
                onFailure = { error ->
                    Toast.makeText(requireContext(), "Failed to add friend: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }


        //testing    -----------------------------------------------------------------------------
        viewModel.error.observe(viewLifecycleOwner) { error ->

            Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
        }

        if (viewModel.users.value.isNullOrEmpty()) {
            viewModel.fetchUsers()
        }

        // Handle closing the search view
        binding.searchFriends.setOnCloseListener {
            toggleEmptyState(true)
            true
        }

        setupSearchView()

        return binding.root
    }

    private fun setupSearchView() {
        binding.searchFriends.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    // Optionally handle query submission (if needed)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    // Clear user list and notify adapter if query is empty
                    usersAdapter.submitList(emptyList())
                    toggleEmptyState(true)
                } else {
                    // Perform search with query text
                    val currentUser = fbRepository.getCurrentId()
                    currentUser?.let {
                        viewModel.searchUsers(newText, it)
                        toggleEmptyState(false)
                    }
                }
                return true
            }
        })
    }


    private fun toggleEmptyState(isEmpty: Boolean) {
        binding.searchedFriendsListRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}

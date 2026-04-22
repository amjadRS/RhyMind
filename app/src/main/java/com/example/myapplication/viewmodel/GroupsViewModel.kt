package com.example.myapplication.viewmodel

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.repository.FirebaseRepository
import com.example.myapplication.ui.models.GroupModel
import com.example.myapplication.ui.models.UserModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GroupsViewModel(private val repository: FirebaseRepository) : ViewModel() {

    private val _groups = MutableLiveData<List<GroupModel>>()
    val groups: LiveData<List<GroupModel>> get() = _groups

    private val _myGroups = MutableLiveData<List<GroupModel>>()
    val myGroups: LiveData<List<GroupModel>> get() = _myGroups

    private val _otherGroups = MutableLiveData<List<GroupModel>>()
    val otherGroups: LiveData<List<GroupModel>> get() = _otherGroups

    private val _friends = MutableLiveData<List<UserModel>>()
    val friends: LiveData<List<UserModel>> get() = _friends

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private val _success = MutableLiveData<String>()
    val success: LiveData<String> get() = _success

    // Function to users created groups from Firebase
    fun loadMyGroups() {
        val userId = repository.getCurrentId()!!
        viewModelScope.launch {
            try {
                val groupsList = withContext(Dispatchers.IO) {
                    repository.getGroupsFromFirebase(userId)
                }
                _myGroups.postValue(groupsList ?: emptyList())
            } catch (e: Exception) {
                _error.postValue("Error loading my groups: ${e.message}")
            }
        }
    }

    // Function to load other groups the user is part of
    fun loadOtherGroups() {
        val userId = repository.getCurrentId()!!
        viewModelScope.launch {
            try {
                val groupsList = withContext(Dispatchers.IO) {
                    repository.getOtherGroupsFromFirebase(userId)
                }
                _otherGroups.postValue(groupsList ?: emptyList())
            } catch (e: Exception) {
                _error.postValue("Error loading other groups: ${e.message}")
            }
        }
    }

    // Function to add a new group
    fun addGroup(group: GroupModel) {
        viewModelScope.launch {
            try {
                repository.addGroupToFirebase(group)
                val updatedGroups = withContext(Dispatchers.IO) {
                    repository.getGroupsFromFirebase(currentUserId = repository.getCurrentId()!!)
                }
                _myGroups.postValue(updatedGroups ?: emptyList())
                _success.postValue("Group was added successfully")
            } catch (e: Exception) {
                _error.postValue("Error adding group: ${e.message}")
            }
        }
    }


    fun setCurrentGroups(groups: List<GroupModel>?) {
        _groups.postValue(groups ?: emptyList())
    }

    // Function to remove a group
    fun removeGroup(groupId: String) {
        val currentUserId = repository.getCurrentId()
        if (currentUserId != null) {
            viewModelScope.launch {
                try {
                    repository.removeGroupFromFirebase(currentUserId, groupId)
                    loadMyGroups() // Reload my groups after removal
                    _success.postValue("Group was removed successfully")
                } catch (e: Exception) {
                    _error.postValue("Error removing group: ${e.message}")
                }
            }
        } else {
            _error.postValue("User is not the creator of the group")
        }
    }

    // Function to load friends
    fun loadFriends() {
        val currentUserId = repository.getCurrentId()
        if (currentUserId != null) {
            viewModelScope.launch {
                try {
                    val list = withContext(Dispatchers.IO) {
                        repository.fetchFriends(currentUserId)
                    }
                    if (list.isNotEmpty()) {
                        _friends.postValue(list)
                    } else {
                        _error.postValue("You have no friends in your friends list")
                    }
                } catch (e: Exception) {
                    _error.postValue("Error loading friends: ${e.message}")
                }
            }
        } else {
            _error.postValue("User is not logged in, Please login!")
        }
    }
}

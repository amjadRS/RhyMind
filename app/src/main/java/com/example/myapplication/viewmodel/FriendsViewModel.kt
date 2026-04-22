package com.example.myapplication.viewmodel

import android.util.Log
import com.example.myapplication.ui.models.UserModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.repository.FirebaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class FriendsViewModel(private val repository: FirebaseRepository) : ViewModel() {

    private val _users = MutableLiveData<List<UserModel>>()
    val users: LiveData<List<UserModel>> get() = _users

    private val _filteredUsers = MutableLiveData<List<UserModel>>()
    val filteredUsers: LiveData<List<UserModel>> get() = _filteredUsers

    private val _addedFriendResult = MutableLiveData<Result<String>>()
    val addedFriendResult: LiveData<Result<String>> get() = _addedFriendResult

    private val _friendsList = MutableLiveData<List<UserModel>>()
    val friendsList : LiveData<List<UserModel>> get() = _friendsList

    private val _friendStatus = MutableLiveData<String>()
    val friendStatus : LiveData <String> get() = _friendStatus

    private val _error = MutableLiveData<Exception>()
    val error: LiveData<Exception> get() = _error

    fun fetchUsers() {
        viewModelScope.launch {
            val userList = withContext(Dispatchers.IO) {
                repository.getUsers()
            }

            if (userList.isEmpty()) {
                // Handle the case when the list is empty, you could update an error LiveData or notify UI
                _error.postValue(Exception("No users found"))
            } else {
                // Handle the case when the list has data
                _users.postValue(userList)
            }
        }
    }

   fun searchUsers(query: String, exclude: String) {
       val currentList = _users.value ?: emptyList()
       val lowerCaseQuery = query.lowercase()

       // Perform filtering, excluding the current user
       _filteredUsers.value = currentList.filter {
           // Exclude the current user by checking if the user ID does not match the exclude ID
           it.userId != exclude &&
                   (it.username.lowercase().contains(lowerCaseQuery) ||
                           it.email.lowercase().contains(lowerCaseQuery))
       }
   }

    fun fetchFriends(currentUserId: String) {
        viewModelScope.launch {
            try {
                val friendsList = withContext(Dispatchers.IO) {
                    repository.fetchFriends(currentUserId)
                }
                _friendsList.postValue(friendsList)
            } catch (exception: Exception) {
                Log.e("FriendsViewModel", "Error fetching friends: ${exception.message}")
            }
        }
    }

    fun addFriend(currentUserId: String, user: UserModel) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.addFriend(currentUserId, user)
                }
                _addedFriendResult.postValue(result)
                val currentList = _friendsList.value ?: emptyList()
                // Check if the user is already in the list
//                if (user !in currentList) {
//                    val updatedList = currentList.toMutableList().apply {
//                        add(user)
//                    }
//                    _friendsList.postValue(updatedList)
//                }
            } catch (e: Exception) {
                _error.postValue(e)
            }
        }
    }

    fun removeFriend(currentUserId: String, friendId: String) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.removeFriend(currentUserId, friendId)
                }
                _addedFriendResult.postValue(result)
                // Update the local list if needed
                if (_friendsList.value != null) {
                    val updatedList = _friendsList.value!!.filterNot { it.userId == friendId }
                    _friendsList.postValue(updatedList)
                }
            } catch (e: Exception) {
                _error.postValue(e)
            }
        }
    }

    fun fetchUserStatus(currentUserId: String, user: UserModel) {
        viewModelScope.launch {
            try {
                val result = repository.getUserStatus(currentUserId, user)
                _friendStatus.postValue(result)
            } catch (e: Exception) {
                _error.postValue(e)
            }
        }
    }
}


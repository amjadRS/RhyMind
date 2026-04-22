package com.example.myapplication.ui.models

data class MemberModel(
    val id: String = "",
    val name: String = "",
    var role: String = "Member", // Role of the member (e.g., "Admin", "Member")
)
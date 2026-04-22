package com.example.myapplication.ui.models

import com.google.firebase.Timestamp

data class GroupModel(
    val groupName: String = "",
    val groupDescription: String = "",
    val groupCategory: String = "",
    var memberCount: Int = 0,
    val groupId: String = "",
    val eventId: String = "",
    val creatorId: String = "",
    val members: List<MemberModel> = listOf(),
    val timeStamp: Timestamp? = Timestamp.now()
)




package com.pratham.todo

import com.google.gson.annotations.SerializedName

data class Todo(
    val __v: Int,
    val _id: String,
    val completed: Boolean,
    val createdAt: String,
    val title: String,
    val userId: String
)

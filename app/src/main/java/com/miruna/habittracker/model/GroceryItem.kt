package com.miruna.habittracker.model

import java.util.UUID

data class GroceryItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isChecked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

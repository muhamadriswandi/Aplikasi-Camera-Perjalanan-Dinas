package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val category: String = "Pajak & Retribusi",
    val colorHex: String = "#F59E0B",
    val createdAt: Long = System.currentTimeMillis()
)

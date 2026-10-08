package com.example.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "records",
    indices = [
        Index(value = ["category"]),
        Index(value = ["updatedAt"]),
        Index(value = ["title"])
    ]
)
data class RecordEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "category")
    val category: String,
    
    @ColumnInfo(name = "description")
    val description: String = "",
    
    @ColumnInfo(name = "referenceInfo")
    val referenceInfo: String = "",
    
    @ColumnInfo(name = "tags")
    val tags: String = "", // Comma-separated tags
    
    @ColumnInfo(name = "attachmentName")
    val attachmentName: String? = null,
    
    @ColumnInfo(name = "attachmentPath")
    val attachmentPath: String? = null,
    
    @ColumnInfo(name = "attachmentSize")
    val attachmentSize: Long = 0L,
    
    @ColumnInfo(name = "attachmentMimeType")
    val attachmentMimeType: String? = null,
    
    @ColumnInfo(name = "isEncrypted")
    val isEncrypted: Boolean = true,
    
    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
)

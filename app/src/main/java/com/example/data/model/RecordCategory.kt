package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class RecordCategory(
    val id: String,
    val displayName: String,
    val icon: ImageVector,
    val badgeColor: Color
) {
    PERSONAL_NOTES("personal_notes", "Personal Notes", Icons.Default.Description, Color(0xFF3B82F6)),
    DOCUMENTS("documents", "Documents & IDs", Icons.Default.Badge, Color(0xFF8B5CF6)),
    CONTACTS("contacts", "Contact Details", Icons.Default.Contacts, Color(0xFF10B981)),
    WORK("work", "Work Reminders", Icons.Default.Work, Color(0xFFF59E0B)),
    FINANCIAL("financial", "Financial & Assets", Icons.Default.AccountBalance, Color(0xFFEC4899)),
    GENERAL("general", "General Info", Icons.Default.Folder, Color(0xFF64748B));

    companion object {
        fun fromId(id: String): RecordCategory {
            return entries.find { it.id.equals(id, ignoreCase = true) || it.displayName.equals(id, ignoreCase = true) } ?: GENERAL
        }
    }
}

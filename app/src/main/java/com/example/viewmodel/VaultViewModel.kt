package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.DatabaseSecurityManager
import com.example.data.database.RecordEntity
import com.example.data.model.RecordCategory
import com.example.data.preferences.VaultPreferences
import com.example.data.repository.RecordsRepository
import com.example.data.service.AttachmentService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class SortOrder(val label: String) {
    UPDATED_DESC("Recently Updated"),
    UPDATED_ASC("Oldest First"),
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)")
}

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = AppDatabase.getDatabase(application)
    val repository: RecordsRepository = RecordsRepository(database.recordDao())
    val preferences: VaultPreferences = VaultPreferences(application)

    val allRecords: StateFlow<List<RecordEntity>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentRecords: StateFlow<List<RecordEntity>> = repository.getRecentRecords(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = repository.recordCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val searchQuery = MutableStateFlow("")
    val selectedCategoryFilter = MutableStateFlow<String?>(null) // null = all
    val selectedSortOrder = MutableStateFlow(SortOrder.UPDATED_DESC)

    val isSessionUnlocked = MutableStateFlow(!preferences.appLockEnabled.value)

    private val _verificationResult = MutableStateFlow<DatabaseSecurityManager.EncryptionVerificationResult?>(null)
    val verificationResult: StateFlow<DatabaseSecurityManager.EncryptionVerificationResult?> = _verificationResult.asStateFlow()

    private val _operationStatusMessage = MutableStateFlow<String?>(null)
    val operationStatusMessage: StateFlow<String?> = _operationStatusMessage.asStateFlow()

    val filteredRecords: StateFlow<List<RecordEntity>> = combine(
        allRecords,
        searchQuery,
        selectedCategoryFilter,
        selectedSortOrder
    ) { records, query, category, sort ->
        var result = records

        if (category != null) {
            result = result.filter { it.category.equals(category, ignoreCase = true) }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter { record ->
                record.title.lowercase().contains(q) ||
                    record.description.lowercase().contains(q) ||
                    record.referenceInfo.lowercase().contains(q) ||
                    record.tags.lowercase().contains(q)
            }
        }

        when (sort) {
            SortOrder.UPDATED_DESC -> result.sortedByDescending { it.updatedAt }
            SortOrder.UPDATED_ASC -> result.sortedBy { it.updatedAt }
            SortOrder.TITLE_ASC -> result.sortedBy { it.title.lowercase() }
            SortOrder.TITLE_DESC -> result.sortedByDescending { it.title.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Run database encryption verification and seed if first launch
        viewModelScope.launch(Dispatchers.IO) {
            verifyDatabaseEncryption()
            // If database is completely empty and onboarding is not completed, pre-seed sample data
            if (database.recordDao().getAllRecords().let { true }) {
                val current = database.recordDao().getRecordByIdSync("dummy")
                // Seeding will be triggered if user requests or initial start
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onCategoryFilterChange(category: String?) {
        selectedCategoryFilter.value = category
    }

    fun onSortOrderChange(order: SortOrder) {
        selectedSortOrder.value = order
    }

    fun clearStatusMessage() {
        _operationStatusMessage.value = null
    }

    fun setStatusMessage(msg: String) {
        _operationStatusMessage.value = msg
    }

    fun verifyDatabaseEncryption() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = DatabaseSecurityManager.verifyDatabaseEncryption(getApplication())
            _verificationResult.value = result
        }
    }

    suspend fun saveRecord(
        existingId: String?,
        title: String,
        category: String,
        description: String,
        referenceInfo: String,
        tags: String,
        attachmentName: String? = null,
        attachmentPath: String? = null,
        attachmentSize: Long = 0L,
        attachmentMimeType: String? = null
    ): Result<String> {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("Record title cannot be blank."))
        }

        return withContext(Dispatchers.IO) {
            try {
                val isDuplicate = repository.isDuplicateTitle(cleanTitle, existingId ?: "")
                if (isDuplicate) {
                    return@withContext Result.failure(IllegalArgumentException("A record with this title already exists in the vault."))
                }

                val now = System.currentTimeMillis()
                val recordId = existingId ?: UUID.randomUUID().toString()

                val record = if (existingId != null) {
                    val existing = repository.getRecordByIdSync(existingId)
                    RecordEntity(
                        id = recordId,
                        title = cleanTitle,
                        category = category,
                        description = description.trim(),
                        referenceInfo = referenceInfo.trim(),
                        tags = tags.trim(),
                        attachmentName = attachmentName ?: existing?.attachmentName,
                        attachmentPath = attachmentPath ?: existing?.attachmentPath,
                        attachmentSize = if (attachmentPath != null) attachmentSize else (existing?.attachmentSize ?: 0L),
                        attachmentMimeType = attachmentMimeType ?: existing?.attachmentMimeType,
                        isEncrypted = true,
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = now
                    )
                } else {
                    RecordEntity(
                        id = recordId,
                        title = cleanTitle,
                        category = category,
                        description = description.trim(),
                        referenceInfo = referenceInfo.trim(),
                        tags = tags.trim(),
                        attachmentName = attachmentName,
                        attachmentPath = attachmentPath,
                        attachmentSize = attachmentSize,
                        attachmentMimeType = attachmentMimeType,
                        isEncrypted = true,
                        createdAt = now,
                        updatedAt = now
                    )
                }

                if (existingId != null) {
                    repository.updateRecord(record)
                    _operationStatusMessage.value = "Record updated securely in vault."
                } else {
                    repository.insertRecord(record)
                    _operationStatusMessage.value = "New record encrypted and stored."
                }

                // Refresh verification status
                verifyDatabaseEncryption()
                Result.success(recordId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun deleteRecord(record: RecordEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Delete attachment file from private storage
                record.attachmentPath?.let { path ->
                    AttachmentService.deleteAttachment(getApplication(), path)
                }
                repository.deleteRecordById(record.id)
                _operationStatusMessage.value = "Record deleted permanently from vault."
                verifyDatabaseEncryption()
            } catch (e: Exception) {
                _operationStatusMessage.value = "Failed to delete record: ${e.localizedMessage}"
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.clearAllRecords()
                // Clean attachments directory
                val dir = AttachmentService.getAttachmentsDirectory(getApplication())
                dir.listFiles()?.forEach { it.delete() }
                _operationStatusMessage.value = "All vault records have been wiped."
                verifyDatabaseEncryption()
            } catch (e: Exception) {
                _operationStatusMessage.value = "Failed to wipe vault: ${e.localizedMessage}"
            }
        }
    }

    fun seedSampleRecords() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.seedSampleRecords()
                _operationStatusMessage.value = "Populated 5 sample encrypted records."
                verifyDatabaseEncryption()
            } catch (e: Exception) {
                _operationStatusMessage.value = "Failed to seed sample records: ${e.localizedMessage}"
            }
        }
    }

    fun unlockVault(pin: String): Boolean {
        val matches = preferences.verifyPin(pin)
        if (matches) {
            isSessionUnlocked.value = true
        }
        return matches
    }

    fun lockVault() {
        if (preferences.appLockEnabled.value) {
            isSessionUnlocked.value = false
        }
    }

    fun setAppLock(enabled: Boolean, pin: String? = null) {
        preferences.setAppLock(enabled, pin)
        if (!enabled) {
            isSessionUnlocked.value = true
        }
    }

    fun completeOnboarding() {
        preferences.setOnboardingCompleted(true)
    }

    fun setThemeMode(mode: String) {
        preferences.setThemeMode(mode)
    }
}

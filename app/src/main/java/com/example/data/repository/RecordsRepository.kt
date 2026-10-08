package com.example.data.repository

import com.example.data.database.RecordDao
import com.example.data.database.RecordEntity
import com.example.data.model.RecordCategory
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class RecordsRepository(private val recordDao: RecordDao) {

    val allRecords: Flow<List<RecordEntity>> = recordDao.getAllRecords()
    val recordCount: Flow<Int> = recordDao.getRecordCount()

    fun getRecentRecords(limit: Int = 5): Flow<List<RecordEntity>> = recordDao.getRecentRecords(limit)

    fun getRecordById(id: String): Flow<RecordEntity?> = recordDao.getRecordById(id)

    suspend fun getRecordByIdSync(id: String): RecordEntity? = recordDao.getRecordByIdSync(id)

    fun searchRecords(query: String): Flow<List<RecordEntity>> = recordDao.searchRecords(query)

    fun getRecordsByCategory(category: String): Flow<List<RecordEntity>> = recordDao.getRecordsByCategory(category)

    fun getRecordCountByCategory(category: String): Flow<Int> = recordDao.getRecordCountByCategory(category)

    suspend fun insertRecord(record: RecordEntity): Long = recordDao.insertRecord(record)

    suspend fun updateRecord(record: RecordEntity): Int = recordDao.updateRecord(record)

    suspend fun deleteRecordById(id: String): Int = recordDao.deleteRecordById(id)

    suspend fun clearAllRecords(): Int = recordDao.clearAllRecords()

    suspend fun isDuplicateTitle(title: String, excludeId: String = ""): Boolean {
        return recordDao.countDuplicateTitle(title.trim(), excludeId) > 0
    }

    suspend fun seedSampleRecords() {
        val sampleList = listOf(
            RecordEntity(
                id = UUID.randomUUID().toString(),
                title = "Home WiFi & Router Recovery Key",
                category = RecordCategory.PERSONAL_NOTES.id,
                description = "Primary mesh router configuration and 24-character WPA3 security recovery passkey. Keep offline.",
                referenceInfo = "SSID: Quantum_Vault_5G / IP: 192.168.1.1",
                tags = "Network, Passwords, Hardware",
                attachmentName = "Router_Config_Backup.txt",
                attachmentPath = "sample_router.txt",
                attachmentSize = 1024L,
                attachmentMimeType = "text/plain",
                isEncrypted = true,
                createdAt = System.currentTimeMillis() - 86400000L * 4,
                updatedAt = System.currentTimeMillis() - 86400000L * 1
            ),
            RecordEntity(
                id = UUID.randomUUID().toString(),
                title = "Passport & National ID Reference",
                category = RecordCategory.DOCUMENTS.id,
                description = "Fictional international passport document number and expiry records for travel filings.",
                referenceInfo = "Passport No: P-88291044 / Expiry: 2031-08-14",
                tags = "Identification, Travel, Official",
                attachmentName = "Passport_Scan_Metadata.txt",
                attachmentPath = "sample_passport.txt",
                attachmentSize = 2048L,
                attachmentMimeType = "text/plain",
                isEncrypted = true,
                createdAt = System.currentTimeMillis() - 86400000L * 7,
                updatedAt = System.currentTimeMillis() - 86400000L * 2
            ),
            RecordEntity(
                id = UUID.randomUUID().toString(),
                title = "Primary Physician & Emergency Contact",
                category = RecordCategory.CONTACTS.id,
                description = "Emergency medical contacts, blood group record, and family physician clinical extension.",
                referenceInfo = "Dr. H. Vance: +1 (555) 019-2834 / Clinic: Bay Medical Rm 402",
                tags = "Health, Emergency, Medical",
                isEncrypted = true,
                createdAt = System.currentTimeMillis() - 86400000L * 10,
                updatedAt = System.currentTimeMillis() - 86400000L * 3
            ),
            RecordEntity(
                id = UUID.randomUUID().toString(),
                title = "Quarterly IT Security Audit Checklist",
                category = RecordCategory.WORK.id,
                description = "Internal checklist for device sanitization, encrypted local backups, and certificate renewal.",
                referenceInfo = "Jira Ticket: SEC-4029 / Compliance Tier: Level 3",
                tags = "Work, Compliance, Security",
                isEncrypted = true,
                createdAt = System.currentTimeMillis() - 86400000L * 3,
                updatedAt = System.currentTimeMillis() - 3600000L * 6
            ),
            RecordEntity(
                id = UUID.randomUUID().toString(),
                title = "Retirement Account & Beneficiary Index",
                category = RecordCategory.FINANCIAL.id,
                description = "Fictional account references, institutional policy ID, and allocation percentages for records.",
                referenceInfo = "Policy: ACC-449-VLT / Custodian: Apex Mutual",
                tags = "Finance, Investments, Future",
                isEncrypted = true,
                createdAt = System.currentTimeMillis() - 86400000L * 14,
                updatedAt = System.currentTimeMillis() - 86400000L * 5
            )
        )

        for (item in sampleList) {
            recordDao.insertRecord(item)
        }
    }
}

package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.database.DatabaseSecurityManager
import com.example.data.database.RecordDao
import com.example.data.database.RecordEntity
import com.example.data.model.RecordCategory
import com.example.data.repository.RecordsRepository
import com.example.data.service.AttachmentService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var recordDao: RecordDao
    private lateinit var repository: RecordsRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        recordDao = database.recordDao()
        repository = RecordsRepository(recordDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context matches SecureVault`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("SecureVault", appName)
    }

    @Test
    fun `insert record and retrieve by id`() = runBlocking {
        val recordId = UUID.randomUUID().toString()
        val record = RecordEntity(
            id = recordId,
            title = "Test WiFi Key",
            category = RecordCategory.PERSONAL_NOTES.id,
            description = "Secret router passkey",
            referenceInfo = "SSID: TestNet",
            tags = "wifi, secret",
            isEncrypted = true,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        repository.insertRecord(record)

        val retrieved = repository.getRecordByIdSync(recordId)
        assertNotNull(retrieved)
        assertEquals("Test WiFi Key", retrieved?.title)
        assertEquals(RecordCategory.PERSONAL_NOTES.id, retrieved?.category)
        assertEquals("Secret router passkey", retrieved?.description)
        assertEquals("SSID: TestNet", retrieved?.referenceInfo)
        assertTrue(retrieved?.isEncrypted == true)
    }

    @Test
    fun `update record preserves created timestamp and updates modified timestamp`() = runBlocking {
        val recordId = UUID.randomUUID().toString()
        val originalCreatedAt = 10000L
        val original = RecordEntity(
            id = recordId,
            title = "Original Title",
            category = RecordCategory.WORK.id,
            description = "Original Desc",
            createdAt = originalCreatedAt,
            updatedAt = originalCreatedAt
        )
        repository.insertRecord(original)

        val newUpdatedAt = 20000L
        val updated = original.copy(
            title = "Updated Title",
            description = "Updated Desc",
            updatedAt = newUpdatedAt
        )
        repository.updateRecord(updated)

        val retrieved = repository.getRecordByIdSync(recordId)
        assertNotNull(retrieved)
        assertEquals("Updated Title", retrieved?.title)
        assertEquals("Updated Desc", retrieved?.description)
        assertEquals(originalCreatedAt, retrieved?.createdAt)
        assertEquals(newUpdatedAt, retrieved?.updatedAt)
    }

    @Test
    fun `delete record removes from database`() = runBlocking {
        val recordId = UUID.randomUUID().toString()
        val record = RecordEntity(
            id = recordId,
            title = "Delete Me",
            category = RecordCategory.GENERAL.id
        )
        repository.insertRecord(record)

        val countBefore = repository.recordCount.first()
        assertEquals(1, countBefore)

        repository.deleteRecordById(recordId)

        val countAfter = repository.recordCount.first()
        assertEquals(0, countAfter)
        assertNull(repository.getRecordByIdSync(recordId))
    }

    @Test
    fun `search records matches title, tags and reference info`() = runBlocking {
        val r1 = RecordEntity(
            id = "1",
            title = "Passport Card",
            category = RecordCategory.DOCUMENTS.id,
            referenceInfo = "DOC-9921",
            tags = "travel, official"
        )
        val r2 = RecordEntity(
            id = "2",
            title = "Doctor Contact",
            category = RecordCategory.CONTACTS.id,
            referenceInfo = "+1 555-0100",
            tags = "medical, health"
        )
        repository.insertRecord(r1)
        repository.insertRecord(r2)

        val searchTravel = repository.searchRecords("travel").first()
        assertEquals(1, searchTravel.size)
        assertEquals("Passport Card", searchTravel[0].title)

        val searchRef = repository.searchRecords("9921").first()
        assertEquals(1, searchRef.size)
        assertEquals("Passport Card", searchRef[0].title)

        val searchDoctor = repository.searchRecords("Doctor").first()
        assertEquals(1, searchDoctor.size)
        assertEquals("Doctor Contact", searchDoctor[0].title)
    }

    @Test
    fun `duplicate title check detects existing titles`() = runBlocking {
        val record = RecordEntity(
            id = "uuid-1",
            title = "Tax Return 2024",
            category = RecordCategory.FINANCIAL.id
        )
        repository.insertRecord(record)

        val isDuplicateSame = repository.isDuplicateTitle("Tax Return 2024", excludeId = "")
        assertTrue(isDuplicateSame)

        val isDuplicateCaseInsensitive = repository.isDuplicateTitle("tax return 2024", excludeId = "")
        assertTrue(isDuplicateCaseInsensitive)

        val isSelfUpdateAllowed = repository.isDuplicateTitle("Tax Return 2024", excludeId = "uuid-1")
        assertFalse(isSelfUpdateAllowed)

        val isDifferentAllowed = repository.isDuplicateTitle("Tax Return 2025", excludeId = "uuid-1")
        assertFalse(isDifferentAllowed)
    }

    @Test
    fun `sample records seeding populates multiple categories`() = runBlocking {
        repository.seedSampleRecords()
        val all = repository.allRecords.first()
        assertTrue(all.size >= 5)

        val categories = all.map { it.category }.toSet()
        assertTrue(categories.size >= 4)
    }

    @Test
    fun `sample attachment creation and deletion`() {
        val sample = AttachmentService.createSampleAttachment(context, "CONFIDENTIAL_NOTE")
        assertNotNull(sample)
        assertTrue(sample.fileSize > 0)
        assertEquals("text/plain", sample.mimeType)

        val preview = AttachmentService.readAttachmentPreview(context, sample.relativePath)
        assertNotNull(preview)
        assertTrue(preview!!.contains("CONFIDENTIAL RECORD ATTACHMENT"))

        val deleted = AttachmentService.deleteAttachment(context, sample.relativePath)
        assertTrue(deleted)
    }
}

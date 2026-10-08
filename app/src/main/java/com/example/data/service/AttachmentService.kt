package com.example.data.service

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object AttachmentService {

    private const val ATTACHMENTS_DIR = "vault_attachments"

    fun getAttachmentsDirectory(context: Context): File {
        val dir = File(context.filesDir, ATTACHMENTS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Copies an external or picked Uri into the app's private sandboxed attachments directory.
     */
    fun saveAttachmentFromUri(context: Context, uri: Uri, originalName: String?): StoredAttachmentResult? {
        return try {
            val contentResolver = context.contentResolver
            val extension = originalName?.substringAfterLast('.', "") ?: "dat"
            val safeExtension = if (extension.isNotEmpty()) ".$extension" else ""
            val uniqueFileName = "att_${UUID.randomUUID()}$safeExtension"

            val targetFile = File(getAttachmentsDirectory(context), uniqueFileName)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            val mimeType = contentResolver.getType(uri) ?: getMimeTypeFromExtension(extension)
            val displayName = originalName ?: uniqueFileName

            StoredAttachmentResult(
                fileName = displayName,
                relativePath = uniqueFileName,
                fileSize = targetFile.length(),
                mimeType = mimeType
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Creates a sample confidential attachment (e.g. for demonstration or testing).
     */
    fun createSampleAttachment(context: Context, sampleType: String): StoredAttachmentResult {
        val dir = getAttachmentsDirectory(context)
        val (fileName, content, mime) = when (sampleType) {
            "CONFIDENTIAL_NOTE" -> Triple(
                "Secure_Encrypted_Notes.txt",
                "CONFIDENTIAL RECORD ATTACHMENT\n\nSecurity Classification: RESTRICTED\nCreated: ${System.currentTimeMillis()}\nHash: SHA256-VLT-DEMO-RECORD\n\nThis attachment is stored securely in the app's private sandboxed storage.",
                "text/plain"
            )
            "TAX_DOCUMENT" -> Triple(
                "Tax_Filing_Confirmation_2025.txt",
                "RECEIPT OF TAX RETURN FILING\nDocument ID: TX-99281-2025\nStatus: Verified\nTimestamp: 2025-04-15\nVerification Code: VLT-SEC-8921-X",
                "text/plain"
            )
            else -> Triple(
                "Device_Identity_Card.txt",
                "DEVICE IDENTIFIER RECORD\nDevice Key Alias: master_vault_key\nStorage: SQLCipher AES-256 local encrypted partition.",
                "text/plain"
            )
        }

        val uniqueFileName = "sample_${UUID.randomUUID()}.txt"
        val targetFile = File(dir, uniqueFileName)
        targetFile.writeText(content)

        return StoredAttachmentResult(
            fileName = fileName,
            relativePath = uniqueFileName,
            fileSize = targetFile.length(),
            mimeType = mime
        )
    }

    /**
     * Reads text content from a text/plain attachment for preview.
     */
    fun readAttachmentPreview(context: Context, relativePath: String): String? {
        return try {
            val file = File(getAttachmentsDirectory(context), relativePath)
            if (file.exists() && file.length() < 100_000) {
                file.readText()
            } else if (file.exists()) {
                "Attachment is ${file.length() / 1024} KB. Preview is truncated."
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Deletes the attachment file from internal storage.
     */
    fun deleteAttachment(context: Context, relativePath: String?): Boolean {
        if (relativePath.isNullOrBlank()) return false
        return try {
            val file = File(getAttachmentsDirectory(context), relativePath)
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            false
        }
    }

    private fun getMimeTypeFromExtension(ext: String): String {
        return when (ext.lowercase()) {
            "txt" -> "text/plain"
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "json" -> "application/json"
            else -> "application/octet-stream"
        }
    }

    data class StoredAttachmentResult(
        val fileName: String,
        val relativePath: String,
        val fileSize: Long,
        val mimeType: String
    )
}

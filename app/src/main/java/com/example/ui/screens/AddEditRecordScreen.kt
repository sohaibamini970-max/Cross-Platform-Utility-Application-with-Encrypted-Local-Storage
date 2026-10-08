package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.RecordEntity
import com.example.data.model.RecordCategory
import com.example.data.service.AttachmentService
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SecureGreen
import com.example.viewmodel.VaultViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecordScreen(
    viewModel: VaultViewModel,
    recordIdToEdit: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isEditMode = !recordIdToEdit.isNullOrBlank()

    var title by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf(RecordCategory.PERSONAL_NOTES) }
    var description by remember { mutableStateOf("") }
    var referenceInfo by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }

    // Attachment fields
    var attachmentName by remember { mutableStateOf<String?>(null) }
    var attachmentPath by remember { mutableStateOf<String?>(null) }
    var attachmentSize by remember { mutableLongStateOf(0L) }
    var attachmentMimeType by remember { mutableStateOf<String?>(null) }

    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var isLoaded by remember { mutableStateOf(!isEditMode) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val result = AttachmentService.saveAttachmentFromUri(
                context = context,
                uri = it,
                originalName = it.lastPathSegment ?: "document_${System.currentTimeMillis()}.dat"
            )
            if (result != null) {
                attachmentName = result.fileName
                attachmentPath = result.relativePath
                attachmentSize = result.fileSize
                attachmentMimeType = result.mimeType
                scope.launch {
                    snackbarHostState.showSnackbar("Attachment saved to private vault storage.")
                }
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("Failed to copy attachment.")
                }
            }
        }
    }

    // Load record if editing
    LaunchedEffect(recordIdToEdit) {
        if (isEditMode && recordIdToEdit != null) {
            val existing = viewModel.repository.getRecordByIdSync(recordIdToEdit)
            if (existing != null) {
                title = existing.title
                selectedCategory = RecordCategory.fromId(existing.category)
                description = existing.description
                referenceInfo = existing.referenceInfo
                tagsInput = existing.tags
                attachmentName = existing.attachmentName
                attachmentPath = existing.attachmentPath
                attachmentSize = existing.attachmentSize
                attachmentMimeType = existing.attachmentMimeType
            }
            isLoaded = true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Edit Record" else "New Vault Record",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("add_edit_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (validateForm(title, { titleError = it })) {
                                isSaving = true
                                saveError = null
                                scope.launch {
                                    val result = viewModel.saveRecord(
                                        existingId = recordIdToEdit,
                                        title = title,
                                        category = selectedCategory.id,
                                        description = description,
                                        referenceInfo = referenceInfo,
                                        tags = tagsInput,
                                        attachmentName = attachmentName,
                                        attachmentPath = attachmentPath,
                                        attachmentSize = attachmentSize,
                                        attachmentMimeType = attachmentMimeType
                                    )
                                    isSaving = false
                                    if (result.isSuccess) {
                                        onNavigateBack()
                                    } else {
                                        saveError = result.exceptionOrNull()?.localizedMessage ?: "Failed to save record"
                                        snackbarHostState.showSnackbar(saveError!!)
                                    }
                                }
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier.testTag("add_edit_save_btn")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save Record",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Security reminder banner
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SecureGreen.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = SecureGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Encrypted On Save: All fields are stored in the SQLCipher AES-256 database.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp
                    )
                }
            }

            // Error alert if duplicate or validation error
            AnimatedVisibility(visible = saveError != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DangerRed.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = DangerRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = saveError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = DangerRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (titleError != null) titleError = null
                    if (saveError != null) saveError = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("record_title_input"),
                label = { Text("Title *") },
                placeholder = { Text("e.g. Master WiFi Passcode, Passport Copy") },
                isError = titleError != null,
                supportingText = {
                    if (titleError != null) {
                        Text(text = titleError!!, color = DangerRed)
                    } else {
                        Text("Required. Must be unique.")
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Category selector dropdown
            ExposedDropdownMenuBox(
                expanded = isCategoryDropdownExpanded,
                onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedCategory.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                    leadingIcon = {
                        Icon(
                            imageVector = selectedCategory.icon,
                            contentDescription = null,
                            tint = selectedCategory.badgeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("category_dropdown_trigger"),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = isCategoryDropdownExpanded,
                    onDismissRequest = { isCategoryDropdownExpanded = false }
                ) {
                    RecordCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.displayName) },
                            leadingIcon = {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    tint = category.badgeColor
                                )
                            },
                            onClick = {
                                selectedCategory = category
                                isCategoryDropdownExpanded = false
                            },
                            modifier = Modifier.testTag("cat_menu_item_${category.id}")
                        )
                    }
                }
            }

            // Reference Info
            OutlinedTextField(
                value = referenceInfo,
                onValueChange = { referenceInfo = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("record_ref_input"),
                label = { Text("Contact or Reference Information") },
                placeholder = { Text("e.g. Account #129348, Phone: +1 555-0199, ID Ref") },
                supportingText = { Text("Optional identifier, phone, account number, or external code.") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Tags
            OutlinedTextField(
                value = tagsInput,
                onValueChange = { tagsInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("record_tags_input"),
                label = { Text("Tags (comma-separated)") },
                placeholder = { Text("e.g. travel, urgent, medical, work") },
                supportingText = { Text("Optional keywords for quick search filtering.") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .testTag("record_desc_input"),
                label = { Text("Description & Notes") },
                placeholder = { Text("Enter detailed records, steps, confidential instructions...") },
                shape = RoundedCornerShape(12.dp),
                maxLines = 6
            )

            // Attachment Section
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Local File Attachment",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (attachmentPath != null) {
                            IconButton(
                                onClick = {
                                    attachmentName = null
                                    attachmentPath = null
                                    attachmentSize = 0L
                                    attachmentMimeType = null
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove attachment",
                                    tint = DangerRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (attachmentPath != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = attachmentName ?: "Attached File",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${attachmentSize / 1024 + 1} KB • ${attachmentMimeType ?: "Document"} • Sandboxed Private Storage",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Attach documents or confidential text records to this item. Files are isolated inside the app's sandboxed private storage directory.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    filePickerLauncher.launch(arrayOf("*/*"))
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pick File", fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val sample = AttachmentService.createSampleAttachment(context, "CONFIDENTIAL_NOTE")
                                    attachmentName = sample.fileName
                                    attachmentPath = sample.relativePath
                                    attachmentSize = sample.fileSize
                                    attachmentMimeType = sample.mimeType
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Sample confidential doc attached.")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NoteAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sample Doc", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Action Button
            Button(
                onClick = {
                    if (validateForm(title, { titleError = it })) {
                        isSaving = true
                        saveError = null
                        scope.launch {
                            val result = viewModel.saveRecord(
                                existingId = recordIdToEdit,
                                title = title,
                                category = selectedCategory.id,
                                description = description,
                                referenceInfo = referenceInfo,
                                tags = tagsInput,
                                attachmentName = attachmentName,
                                attachmentPath = attachmentPath,
                                attachmentSize = attachmentSize,
                                attachmentMimeType = attachmentMimeType
                            )
                            isSaving = false
                            if (result.isSuccess) {
                                onNavigateBack()
                            } else {
                                saveError = result.exceptionOrNull()?.localizedMessage ?: "Failed to save record"
                                snackbarHostState.showSnackbar(saveError!!)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_record_submit_btn"),
                shape = RoundedCornerShape(14.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Encrypting & Storing...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEditMode) "Update Record" else "Save Record To Vault",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun validateForm(
    title: String,
    onTitleError: (String?) -> Unit
): Boolean {
    val clean = title.trim()
    if (clean.isEmpty()) {
        onTitleError("Title is required and cannot be empty.")
        return false
    }
    if (clean.length < 2) {
        onTitleError("Title must be at least 2 characters long.")
        return false
    }
    onTitleError(null)
    return true
}

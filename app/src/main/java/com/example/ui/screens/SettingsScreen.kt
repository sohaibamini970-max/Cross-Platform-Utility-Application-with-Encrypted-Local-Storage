package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.DatabaseSecurityManager
import com.example.ui.components.WipeAllConfirmationDialog
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SecureGreen
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.VaultViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: VaultViewModel,
    onNavigateBack: () -> Unit,
    onLockAppNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val themeMode by viewModel.preferences.themeMode.collectAsStateWithLifecycle()
    val isAppLockEnabled by viewModel.preferences.appLockEnabled.collectAsStateWithLifecycle()
    val verificationResult by viewModel.verificationResult.collectAsStateWithLifecycle()

    var showWipeDialog by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinConfirmInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    val keyFingerprint = remember { DatabaseSecurityManager.getKeyFingerprint(context) }

    if (showWipeDialog) {
        WipeAllConfirmationDialog(
            onConfirm = {
                viewModel.clearAllData()
                showWipeDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("All vault records have been wiped.")
                }
            },
            onDismiss = { showWipeDialog = false }
        )
    }

    if (showSetPinDialog) {
        AlertDialog(
            onDismissRequest = { showSetPinDialog = false },
            title = {
                Text(
                    text = "Configure 4-Digit Vault PIN",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Set a 4-digit numeric code to unlock your vault session.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinInput = it },
                        label = { Text("4-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_pin_input")
                    )

                    OutlinedTextField(
                        value = pinConfirmInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinConfirmInput = it },
                        label = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_pin_confirm_input")
                    )

                    pinError?.let {
                        Text(text = it, color = DangerRed, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length != 4) {
                            pinError = "PIN must be exactly 4 digits."
                        } else if (pinInput != pinConfirmInput) {
                            pinError = "PIN confirmation does not match."
                        } else {
                            viewModel.setAppLock(true, pinInput)
                            showSetPinDialog = false
                            pinInput = ""
                            pinConfirmInput = ""
                            pinError = null
                            scope.launch {
                                snackbarHostState.showSnackbar("Vault PIN lock enabled.")
                            }
                        }
                    },
                    modifier = Modifier.testTag("settings_save_pin_btn")
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSetPinDialog = false
                    pinInput = ""
                    pinConfirmInput = ""
                    pinError = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings & Security", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
            // Theme Settings
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = CyanAccent
                        )
                        Text(
                            text = "Appearance & Theme",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = themeMode == "SYSTEM",
                            onClick = { viewModel.setThemeMode("SYSTEM") },
                            label = { Text("System") },
                            leadingIcon = { Icon(Icons.Default.BrightnessAuto, null, Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = themeMode == "DARK",
                            onClick = { viewModel.setThemeMode("DARK") },
                            label = { Text("Dark") },
                            leadingIcon = { Icon(Icons.Default.DarkMode, null, Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = themeMode == "LIGHT",
                            onClick = { viewModel.setThemeMode("LIGHT") },
                            label = { Text("Light") },
                            leadingIcon = { Icon(Icons.Default.LightMode, null, Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Cryptographic Security Audit Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = SecureGreen
                            )
                            Text(
                                text = "Database Security Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { viewModel.verifyDatabaseEncryption() },
                            modifier = Modifier.size(32.dp).testTag("verify_encryption_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh verification",
                                tint = CyanAccent
                            )
                        }
                    }

                    // Security metrics
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SecurityItemRow("Storage Engine", "SQLCipher 4.6.1 + Room")
                            SecurityItemRow("Encryption Cipher", "AES-256-CBC (PBKDF2 HMAC-SHA512)")
                            SecurityItemRow("Key Vault", "Android Keystore (Hardware TEE)")
                            SecurityItemRow("Key Fingerprint", keyFingerprint)
                            SecurityItemRow("Database File", DatabaseSecurityManager.DB_NAME)
                            SecurityItemRow(
                                "Database Size",
                                "${(verificationResult?.fileSizeBytes ?: 0L) / 1024 + 1} KB"
                            )
                        }
                    }

                    // Verification outcome banner
                    val isVerified = verificationResult?.isCipherVerified == true
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isVerified) SecureGreen.copy(alpha = 0.12f) else DangerRed.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isVerified) Icons.Default.VerifiedUser else Icons.Default.Security,
                                contentDescription = null,
                                tint = if (isVerified) SecureGreen else DangerRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = if (isVerified) "Active Encryption Verified" else "Verification Warning",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isVerified) SecureGreen else DangerRed
                                )
                                Text(
                                    text = verificationResult?.message ?: "Running on-disk file header inspection...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.verifyDatabaseEncryption()
                            scope.launch {
                                snackbarHostState.showSnackbar("Disk header inspection complete.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Re-Verify Physical File Encryption")
                    }
                }
            }

            // App Lock PIN Settings
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = CyanAccent
                            )
                            Column {
                                Text(
                                    text = "Vault PIN Lock",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isAppLockEnabled) "Enabled (PIN Protected)" else "Disabled (Immediate Access)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isAppLockEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    showSetPinDialog = true
                                } else {
                                    viewModel.setAppLock(false)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Vault PIN lock disabled.")
                                    }
                                }
                            },
                            modifier = Modifier.testTag("app_lock_switch")
                        )
                    }

                    if (isAppLockEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showSetPinDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Change PIN")
                            }

                            Button(
                                onClick = onLockAppNow,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Lock, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Lock Now")
                            }
                        }
                    }
                }
            }

            // Data Management Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = CyanAccent
                        )
                        Text(
                            text = "Data Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.seedSampleRecords()
                            scope.launch {
                                snackbarHostState.showSnackbar("Fictional demo records added.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("settings_seed_data_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Seed Fictional Demo Records")
                    }

                    Button(
                        onClick = { showWipeDialog = true },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = DangerRed
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("settings_wipe_all_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Wipe All Data (Permanent)")
                    }
                }
            }

            // Local Storage & Privacy Policy Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Local Storage & Privacy Statement",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• All data is held strictly inside application-private storage on this device.\n• No cloud sync, telemetric trackers, or remote servers are contacted.\n• Deleting records wipes both database rows and associated private attachments.\n• Backups require manual encrypted export; uninstalling the app removes local encryption keys.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            // Application Version Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SecureVault v1.0.0",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Personal Records Manager • Task 2 Academic Security Build",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SecurityItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

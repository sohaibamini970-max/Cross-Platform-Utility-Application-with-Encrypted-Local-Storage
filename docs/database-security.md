# Database Security & Cryptographic Key Management

## 1. SQLCipher Implementation Details
SecureVault implements true database-level page encryption rather than simple field-level obfuscation or unencrypted SQLite:
- **Library:** `net.zetetic:sqlcipher-android:4.6.1`
- **Algorithm:** AES-256 in CBC mode with HMAC-SHA512 per page integrity check and PBKDF2 key derivation (64,000 iterations).
- **Page Size:** 4096 bytes per page.
- **Scope of Encryption:** Every B-tree page, index, table schema, and cell in the SQLite database file on disk is encrypted.

## 2. Secure Key Lifecycle & Key Generation
```
[Device First Launch]
       │
       ▼
SecureRandom (32 bytes = 256 bits)
       │
       ▼
Android Keystore (MasterKey AES-256-GCM)
       │
       ▼
EncryptedSharedPreferences ("securevault_keystore_prefs")
       │
       ▼
DatabaseSecurityManager (SupportFactory)
       │
       ▼
Room DatabaseBuilder (openHelperFactory)
       │
       ▼
SQLCipher Native Engine (PRAGMA key)
```

- **No Hardcoding:** The source code and version control contain zero encryption keys or hardcoded passphrases.
- **No Insecure Logging:** Passphrases are never output to `Logcat`, console streams, or analytics.
- **Fingerprinting:** Users and auditors can verify key integrity via a non-reversible SHA-256 digest preview on the Settings screen without exposing raw secret material.

## 3. Cryptographic Verification Procedure
A core requirement of this project is avoiding false security claims. Unencrypted SQLite databases always begin with the standardized 16-byte magic header:
`"SQLite format 3\0"` (`0x53 0x51 0x4C 0x69 0x74 0x65 0x20 0x66 0x6F 0x72 0x6D 0x61 0x74 0x20 0x33 0x00`).

In contrast, SQLCipher databases have pseudorandom ciphertext bytes across the entire file header because the salt and initial initialization vector (IV) occupy the page header.

### Automated Diagnostic Check:
The application includes `DatabaseSecurityManager.verifyDatabaseEncryption(context)`, exposed via a live verification button in the Settings screen:
1. Reads the first 16 bytes of `/data/user/0/<package>/databases/securevault_encrypted.db`.
2. Evaluates byte equality against `SQLite format 3\0`.
3. If the magic header is detected: reports `UNENCRYPTED_PLAINTEXT` error.
4. If the magic header is absent and file size > 0: reports `ENCRYPTED_SQLCIPHER` with raw ciphertext hex sample.

## 4. Local File Attachment Security
- File attachments are saved inside `context.filesDir/vault_attachments`.
- Android's Linux kernel enforces UID application sandbox isolation, preventing other third-party apps from reading this directory.
- Deleting a record executes an automatic file wipe in `AttachmentService.deleteAttachment()`.

## 5. Backup Implications & Limitations
- **Backup Exclusion:** By default, Android cloud backups cannot decrypt SQLCipher files on a restored device unless the Android Keystore hardware keys are also preserved.
- **Hardware Bound:** If the application is uninstalled or the device undergoes a factory reset, the hardware-backed keystore entry is permanently destroyed, rendering the `.db` file permanently unrecoverable (forward secrecy).

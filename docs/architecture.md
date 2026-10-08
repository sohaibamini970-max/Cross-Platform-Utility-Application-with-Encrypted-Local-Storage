# Technical Architecture — SecureVault

## 1. Architectural Overview
SecureVault follows **Modern Android Architecture (MVVM + Repository Pattern)** with **Jetpack Compose** as the declarative UI framework and **Room with SQLCipher** as the persistent encrypted local data store.

```
┌────────────────────────────────────────────────────────┐
│                   UI Layer (Compose)                   │
│  WelcomeScreen │ HomeScreen │ RecordsList │ Details    │
│       AddEditRecordScreen │ Settings │ VaultLock       │
└───────────────────────────▲────────────────────────────┘
                            │ (StateFlow / Actions)
┌───────────────────────────┴────────────────────────────┐
│                    VaultViewModel                      │
│     StateFlows (filteredRecords, recent, totalCount)   │
│     Business Validation, Duplicate Checks, PIN State   │
└───────────────────────────▲────────────────────────────┘
                            │ (suspend / Flow<T>)
┌───────────────────────────┴────────────────────────────┐
│                  RecordsRepository                     │
│       Coordinates Room DAO & Attachment Storage        │
└─────────────────▲─────────────────────────▲────────────┘
                  │                         │
┌─────────────────┴─────────────┐ ┌─────────┴────────────┐
│    Database Layer (Room)      │ │   AttachmentService   │
│  RecordEntity │ RecordDao     │ │ Sandboxed Private     │
│  AppDatabase                  │ │ Storage (filesDir)    │
└─────────────────▲─────────────┘ └──────────────────────┘
                  │
┌─────────────────┴──────────────────────────────────────┐
│            DatabaseSecurityManager (SQLCipher)         │
│  - Android Keystore AES-256 Master Key                 │
│  - EncryptedSharedPreferences Passphrase Storage       │
│  - SupportFactory OpenHelper Integration               │
│  - On-Disk Physical Header Verification                │
└────────────────────────────────────────────────────────┘
```

## 2. Screen Architecture & Navigation
Navigation is managed using **Jetpack Navigation Compose**:
1. **`welcome` (Screen A):** Visual brand hero banner, privacy architecture highlights, "Get Started" and "Load Demo Records" actions.
2. **`home` (Screen B):** Summary dashboard, total record counts, category overview pills, quick search, recent items list, and FAB.
3. **`records?category={cat}` (Screen C):** Full list with live multi-field filtering, category tabs, and sorting (Updated Desc, Updated Asc, Title A-Z, Title Z-A).
4. **`add_record` (Screen D):** Input form with title uniqueness validation, category selection, reference info, tags, and local file attachment picker.
5. **`details/{id}` (Screen E):** Complete record viewer with category pill, copy-to-clipboard reference action, attachment preview, and delete confirmation.
6. **`edit_record/{id}` (Screen F):** Pre-filled editing form preserving `createdAt` while refreshing `updatedAt`.
7. **`settings` (Screen G):** Appearance theme switcher, live SQLCipher on-disk cryptographic header verification, Vault PIN lock management, and database wipe.
8. **`VaultLockScreen`:** Full-screen overlay triggered whenever PIN lock is active and the session is locked.

## 3. Database Schema
Room manages the `records` table in `securevault_encrypted.db`:
- `id` (TEXT PRIMARY KEY): Stable UUID v4 string.
- `title` (TEXT NOT NULL): Record title (indexed).
- `category` (TEXT NOT NULL): Category identifier (indexed).
- `description` (TEXT): Detailed notes.
- `referenceInfo` (TEXT): Document numbers, phone numbers, or external identifiers.
- `tags` (TEXT): Comma-delimited search keywords.
- `attachmentName` (TEXT): Original file display name.
- `attachmentPath` (TEXT): Relative path in `context.filesDir/vault_attachments`.
- `attachmentSize` (INTEGER): File size in bytes.
- `attachmentMimeType` (TEXT): MIME type (e.g., `text/plain`, `application/pdf`).
- `isEncrypted` (INTEGER): Boolean flag indicating cryptographic protection.
- `createdAt` (INTEGER NOT NULL): Epoch milliseconds.
- `updatedAt` (INTEGER NOT NULL): Epoch milliseconds (indexed for sorting).

## 4. Key Management & Hardware Security
- Master keys are provisioned in the **Android Keystore** using AES-256 in Galois/Counter Mode (GCM).
- A 256-bit cryptographically secure random passphrase is generated using `java.security.SecureRandom`.
- The passphrase is stored in `EncryptedSharedPreferences`, requiring hardware-backed authentication before retrieval.
- SQLCipher's `SupportFactory(passphrase)` is attached to Room's `databaseBuilder`.

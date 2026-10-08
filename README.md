# SecureVault — Personal Records Manager

[![Android Build](https://img.shields.io/badge/Platform-Android%20%28Kotlin%20%2B%20Compose%29-brightgreen)]()
[![Database](https://img.shields.io/badge/Database-SQLCipher%20256--bit%20AES-blue)]()
[![Key Management](https://img.shields.io/badge/Key%20Protection-Android%20Keystore-orange)]()
[![Offline First](https://img.shields.io/badge/Network-100%25%20Offline%20Ready-success)]()

**SecureVault** is an offline-first mobile utility application designed for secure local management of sensitive personal records. Built with **Kotlin**, **Jetpack Compose**, and **Room**, it leverages **SQLCipher** and the **Android Keystore** to deliver verified cryptographic protection for personal documents, passcodes, contacts, and notes directly on the user's device.

---

## 1. Project Objective & Assignment Requirements

This application was developed to fulfill **Task 2: Cross-Platform Utility Application with Encrypted Local Storage**:
- **CRUD Operations:** Complete, validated database-backed Create, Read, Update, and Delete operations.
- **Genuine Local Encryption:** Full SQLite database encryption using SQLCipher (AES-256-CBC) rather than unencrypted SQLite or fake encryption claims.
- **Secure Key Management:** Dynamic 256-bit key generation and storage backed by the platform's hardware-secured keystore (Android Keystore).
- **Responsive UI & Forms:** Modern Material Design 3 interface with real-time field validation, duplicate detection, and empty states.
- **Offline Reliability:** Zero dependency on remote APIs or third-party servers.
- **Local Attachments:** Sandboxed private file management for confidential document references.
- **Comprehensive Testing & Documentation:** Automated Robolectric test suites and architectural documentation.

---

## 2. Technology Stack

- **Framework & Language:** Kotlin (2.2) with Jetpack Compose & Material Design 3.
- **Database Engine:** Room (2.7.0) with SQLCipher (`net.zetetic:sqlcipher-android:4.6.1`).
- **Key Storage:** Android Keystore (`AndroidKeyStore`) with `androidx.security:security-crypto:1.1.0-alpha06`.
- **Navigation:** Jetpack Navigation Compose (`androidx.navigation:navigation-compose:2.8.9`).
- **Architecture:** MVVM (Model-View-ViewModel) with reactive Kotlin Coroutines & Flow.
- **Preferences:** Shared preferences for theme and session PIN settings.
- **Testing:** JUnit 4, AndroidX Test, and Robolectric (4.16.1).

---

## 3. Architecture & Project Structure

The project strictly follows clean modular architecture:

```
app/src/main/java/com/example/
├── MainActivity.kt                      # Edge-to-edge entry point and theme host
├── data/
│   ├── database/
│   │   ├── AppDatabase.kt               # Room database definition with SQLCipher factory
│   │   ├── DatabaseSecurityManager.kt   # Keystore key retrieval & on-disk header audit
│   │   ├── RecordDao.kt                 # Reactive Room DAO queries and mutations
│   │   └── RecordEntity.kt              # Room entity schema with multi-column indexes
│   ├── model/
│   │   └── RecordCategory.kt            # 6 Record categories with bespoke icons and badges
│   ├── preferences/
│   │   └── VaultPreferences.kt          # Theme, onboarding, and PIN lock state
│   ├── repository/
│   │   └── RecordsRepository.kt         # Data repository, duplicate checks & demo seed
│   └── service/
│       └── AttachmentService.kt         # Private sandboxed file attachment management
├── ui/
│   ├── components/
│   │   └── CommonComponents.kt          # RecordItemCard, StatCard, Badge, Dialogs
│   ├── navigation/
│   │   └── VaultNavigation.kt           # Type-safe Jetpack Compose routing
│   ├── screens/
│   │   ├── WelcomeScreen.kt             # Onboarding with privacy statement & hero banner
│   │   ├── HomeScreen.kt                # Summary dashboard, category counts & recents
│   │   ├── RecordsListScreen.kt         # Full records list with live search, filter & sort
│   │   ├── AddEditRecordScreen.kt       # Validated form for creating and updating records
│   │   ├── RecordDetailsScreen.kt       # Full field view, copy reference & attachments
│   │   ├── SettingsScreen.kt            # Security diagnostics, theme toggle, PIN & wipe
│   │   └── VaultLockScreen.kt           # 4-Digit numpad screen for locked sessions
│   └── theme/
│       ├── Color.kt                     # Deep navy (#0F172A) & Cyber cyan (#00ADB5)
│       ├── Theme.kt                     # Centralized Material 3 Light/Dark color schemes
│       └── Type.kt                      # Clean typography definitions
└── viewmodel/
    └── VaultViewModel.kt                # Reactive StateFlows, CRUD handlers, search & sort
```

---

## 4. Database Schema & CRUD Implementation

### Schema Definition (`records` table):
| Column | Type | Description |
|---|---|---|
| `id` | `TEXT PRIMARY KEY` | Stable UUID v4 identifier |
| `title` | `TEXT NOT NULL` | Record title (indexed, unique check enforced) |
| `category` | `TEXT NOT NULL` | Category ID (indexed) |
| `description` | `TEXT` | Detailed multi-line notes |
| `referenceInfo` | `TEXT` | Document reference, account number, or contact info |
| `tags` | `TEXT` | Comma-separated search tags |
| `attachmentName` | `TEXT` | Display name of the attached file |
| `attachmentPath` | `TEXT` | Relative path inside `filesDir/vault_attachments` |
| `attachmentSize` | `INTEGER` | File size in bytes |
| `attachmentMimeType` | `TEXT` | File MIME type |
| `isEncrypted` | `INTEGER` | Security flag (always true for SQLCipher) |
| `createdAt` | `INTEGER NOT NULL` | Millisecond epoch timestamp (preserved on edit) |
| `updatedAt` | `INTEGER NOT NULL` | Millisecond epoch timestamp (refreshed on edit, indexed) |

### CRUD Operations:
- **Create:** Inserts record with validation; prevents duplicate titles; records timestamp.
- **Read:** Reactive `Flow<List<RecordEntity>>` streams; live search against title, description, tags, and references; filter by category; sort by title or updated date.
- **Update:** Pre-fills form fields; preserves original `createdAt` timestamp; updates `updatedAt`.
- **Delete:** Displays a modal confirmation dialog; purges the database row and deletes any associated file attachment from the device's private storage.

---

## 5. Genuine Database Encryption & Key Management

### SQLCipher 4.6.1 Integration
Rather than relying on unencrypted SQLite, SecureVault attaches `net.zetetic.database.sqlcipher.SupportFactory` to Room's `openHelperFactory`:
- **Algorithm:** AES-256 in CBC mode with HMAC-SHA512 page integrity checking.
- **Page Size:** 4096-byte pages, each encrypted with an individual initialization vector.
- **Zero Plaintext:** All indexes, table structures, and record rows are encrypted directly on disk.

### Hardware Keystore Key Management
1. A 256-bit cryptographically secure random passphrase is generated via `SecureRandom`.
2. The passphrase is encrypted and stored in `EncryptedSharedPreferences`, sealed with a master key in the hardware-backed **Android Keystore** (`MasterKey.KeyScheme.AES256_GCM`).
3. Keys are never printed to `Logcat`, console streams, or hardcoded in source code.
4. The Settings screen displays a non-reversible truncated SHA-256 fingerprint for verification.

### Physical On-Disk Verification Procedure
Unencrypted SQLite files always begin with the 16-byte magic header:
`"SQLite format 3\0"` (`0x53 0x51 0x4C 0x69 0x74 0x65 0x20 0x66 0x6F 0x72 0x6D 0x61 0x74 0x20 0x33 0x00`).

In `DatabaseSecurityManager.verifyDatabaseEncryption()`, the app opens `securevault_encrypted.db` on disk, reads the first 16 bytes, and checks for this header:
- If `"SQLite format 3\0"` is detected: **Unencrypted Plaintext Failure**.
- If ciphertext bytes are found with no SQLite magic string: **Verified Encrypted Status**.

---

## 6. Installation, Build & Testing

### Compilation:
To compile the Android project:
```bash
gradle :app:assembleDebug
```

### Running Automated Tests:
To run unit and Robolectric test suites:
```bash
gradle :app:testDebugUnitTest
```

### Test Suite Summary:
- `read string from context matches SecureVault`: Launcher resource verified.
- `insert record and retrieve by id`: Room insertion & retrieval verified.
- `update record preserves created timestamp`: Immutable `createdAt` confirmed.
- `delete record removes from database`: Cascade delete and count drop confirmed.
- `search records matches title, tags and reference info`: Multi-field search confirmed.
- `duplicate title check detects existing titles`: Collision prevention confirmed.
- `sample records seeding populates multiple categories`: Demo seeding confirmed.
- `sample attachment creation and deletion`: Private sandbox attachment confirmed.

---

## 7. Security Documentation Links

Detailed architectural documentation is provided in the `docs/` directory:
- [docs/scope.md](docs/scope.md) — Business scope, boundaries, and assumptions.
- [docs/architecture.md](docs/architecture.md) — Architectural diagram and data flows.
- [docs/database-security.md](docs/database-security.md) — Cryptographic details and on-disk audit.
- [docs/testing-report.md](docs/testing-report.md) — Test cases and problem resolution.
- [docs/limitations.md](docs/limitations.md) — Known security boundaries and future roadmap.

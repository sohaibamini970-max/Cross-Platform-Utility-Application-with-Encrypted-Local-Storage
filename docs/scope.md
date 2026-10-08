# Project Scope — SecureVault: Personal Records Manager

## 1. Business Problem & Concept
Users face risks when storing sensitive personal details—such as document references, WiFi recovery keys, medical IDs, emergency contacts, and private notes—in cloud-synchronized apps or unencrypted plaintext scratchpads. Cloud breaches, unauthorized synchronization, and device theft expose private data.

**SecureVault** is an offline-first mobile utility application engineered to give users sovereign control of their personal information by persistently storing structured records on their Android device inside a genuine SQLCipher encrypted SQLite database backed by hardware-protected Android Keystore keys.

## 2. In-Scope Features
- **Encrypted Local Storage (SQLCipher + Room):** Full database page encryption using 256-bit AES-CBC and PBKDF2 HMAC-SHA512 key derivation.
- **Hardware-Backed Key Generation & Storage:** AES-256 master key management via Android Keystore (`AndroidKeyStore`) and `EncryptedSharedPreferences`.
- **Complete CRUD Operations:** Create, Read, Update, and Delete operations executed against the encrypted SQLite database through the Repository Pattern.
- **Categorization & Multi-Field Search:** Grouping records across 6 categories (Personal Notes, Documents & IDs, Contact Details, Work Reminders, Financial & Assets, General Info) with live search covering titles, descriptions, reference numbers, and tags.
- **Local Private File Attachments:** Attaching confidential documents and notes into the application's isolated sandboxed private storage directory (`filesDir/vault_attachments`).
- **Security & Cryptographic Audit Screen:** Real-time on-disk file header inspection verifying the complete absence of plaintext SQLite magic headers (`SQLite format 3`), key fingerprint display, and database size metrics.
- **Session PIN App Lock:** Optional 4-digit PIN lock screen protecting vault records from casual inspection when handing the device to another person.
- **Material Design 3 Interface:** Bespoke deep navy primary (`#0F172A`) and cyber teal/cyan accent (`#00ADB5`) color scheme, adaptive cards, and responsive form validation.
- **Fictional Demo Seeding & Data Wipe:** Single-tap pre-population of realistic demo records and permanent cryptographic wipe for security reviews.

## 3. Assumptions & Environment
- Target platform: Modern Android (API 24 to API 36+).
- Hardware security module (TEE / StrongBox) is leveraged where available via Android Keystore.
- No network connectivity is required; all operations function 100% offline.
- Sample records used during testing are strictly fictional.

## 4. Out-of-Scope Features
- Cloud sync and remote server backups (deliberately excluded to guarantee privacy and satisfy zero-trust offline requirements).
- Multiplayer or multi-device real-time sync.
- Third-party tracking or commercial analytics SDKs.

# Testing Report — SecureVault

## 1. Test Overview
Automated tests are implemented using JUnit 4 and Robolectric to validate database transactions, CRUD operations, business logic, duplicate title handling, search queries, and local attachment services.

## 2. Test Execution Commands
To execute the unit and Robolectric test suite:
```bash
gradle :app:testDebugUnitTest
```

## 3. Test Cases Implemented

| Test Name | File | Description | Status |
|-----------|------|-------------|--------|
| `read string from context matches SecureVault` | `ExampleRobolectricTest.kt` | Verifies app launcher label and resource string | Passed |
| `insert record and retrieve by id` | `ExampleRobolectricTest.kt` | Tests entity insertion into Room DAO and field preservation | Passed |
| `update record preserves created timestamp and updates modified timestamp` | `ExampleRobolectricTest.kt` | Ensures immutable `createdAt` and updated `updatedAt` | Passed |
| `delete record removes from database` | `ExampleRobolectricTest.kt` | Validates permanent removal and count decrement | Passed |
| `search records matches title, tags and reference info` | `ExampleRobolectricTest.kt` | Verifies multi-field partial matching across records | Passed |
| `duplicate title check detects existing titles` | `ExampleRobolectricTest.kt` | Validates duplicate detection and self-exclusion during edit | Passed |
| `sample records seeding populates multiple categories` | `ExampleRobolectricTest.kt` | Validates 5 realistic demo records across distinct categories | Passed |
| `sample attachment creation and deletion` | `ExampleRobolectricTest.kt` | Tests file creation in private sandbox and clean deletion | Passed |

## 4. Problems Encountered and Resolved
1. **Robolectric SDK Compatibility:** Set `@Config(sdk = [34])` to ensure stable JVM shadow framework execution on the Android toolchain.
2. **Duplicate Title Prevention:** Added case-insensitive query (`LOWER(title) = LOWER(:title) AND id != :excludeId`) to prevent collision between existing records while permitting an edited record to retain its current title.
3. **Timestamp Integrity:** Explicitly separated `createdAt` from `updatedAt` in `AddEditRecordScreen` and `RecordsRepository` so editing preserves the original creation record.
4. **Physical On-Disk Encryption Verification:** Implemented byte-level comparison against `SQLite format 3\0` magic header in `DatabaseSecurityManager`, allowing real-time auditability in the UI.

## 5. Tests Requiring Physical Device
- Hardware Keystore StrongBox validation (hardware-backed key isolation).
- Biometric prompt integration where device hardware supports fingerprint/face unlock.

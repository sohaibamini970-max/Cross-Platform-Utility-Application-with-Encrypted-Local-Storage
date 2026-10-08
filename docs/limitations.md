# Known Limitations & Future Improvements — SecureVault

## 1. Honest Technical Limitations
1. **Device Factory Reset / Uninstall Loss:** Because encryption keys are tied to the hardware-backed Android Keystore, uninstalling the app permanently purges the key. Any raw backup of `securevault_encrypted.db` cannot be recovered without this key.
2. **Local Attachments vs Page Encryption:** While database records are 100% page-encrypted with SQLCipher, file attachments saved in `filesDir/vault_attachments` rely on Android Linux UID sandboxing. If an attacker gains root access to the device file system, unencrypted individual attachment files could theoretically be read.
3. **Absence of Cloud Sync:** By design, the app does not feature automated cloud backup or multi-device sync to maintain zero-knowledge privacy.
4. **Passphrase Export:** The app currently does not provide an external recovery seed phrase (e.g., BIP-39 mnemonic) for transferring the vault database to another device.

## 2. Proposed Future Improvements
1. **Passphrase-Protected Encrypted Backup Archive:** Export all records and attachments into an AES-256-GCM encrypted ZIP archive protected by a user-chosen master password.
2. **Biometric Unlock via Jetpack BiometricPrompt:** Allow users with fingerprint or face recognition hardware to unlock their vault session alongside the existing 4-digit PIN.
3. **Encrypted Attachment Streaming:** Implement cipher streams (`CipherInputStream`/`CipherOutputStream`) so file attachments are encrypted on write and decrypted only in memory during preview.
4. **Auto-Lock Timeout:** Automatically lock the vault after 60 seconds of application backgrounding.

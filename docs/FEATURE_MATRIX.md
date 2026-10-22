# ELVARIX — COMPREHENSIVE PRODUCT FEATURE MATRIX

**Version:** 1.2.0  
**Application ID:** `com.novavault`  
**Classification:** Enterprise-Grade Zero-Knowledge Password & Digital Identity Manager  
**Specification Reference:** 14-Section Elvarix Authoritative Product Catalogue

---

## Status Classification Legend

- **Implemented & Verified**: Fully coded, integrated into production build, and verified through automated or manual test suites.
- **Implemented (Pending Remote Config)**: Fully coded in client and service layers; awaiting production cloud provider credentials (e.g. Supabase Custom SMTP or Cloud KMS).
- **Partially Implemented**: Working domain and data foundations in place; user-interface or auxiliary workflow under active expansion.
- **Planned for Later Milestone**: Architectural foundation designed; scheduled for subsequent platform milestone.
- **Blocked by External Platform / Dependency**: Requires external vendor contracts, OS platform capabilities, or paid infrastructure.

---

## 1. Vault Management

| Feature | Status | Source Files | Dependencies | Security Considerations | Milestone | Test / Verification Procedure | Remaining Blocker |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Login Credentials** (User/Pass/URLs) | Implemented & Verified | `VaultItem.kt`, `AddEditItemScreen.kt`, `VaultRepositoryImpl.kt` | Room, BouncyCastle | AES-256-GCM envelope; encrypted at rest | Milestone B | `VaultLockAndCryptoTest` | None |
| **Secure Notes** | Implemented & Verified | `VaultItem.kt`, `AddEditItemScreen.kt` | Room | Content encrypted via DEK; never in logs | Milestone B | Unit test + Form UI inspection | None |
| **Payment Cards** (Number/Exp/CVV) | Implemented & Verified | `VaultItem.kt`, `AddEditItemScreen.kt` | Room | PCI-DSS aligned client masking; secure clipboard | Milestone B | Unit test + Mask/Reveal toggle test | None |
| **Identity Records** | Implemented & Verified | `VaultItem.kt`, `AddEditItemScreen.kt` | Room | PII encrypted at rest; masked in lists | Milestone B | Form creation & persistence test | None |
| **Wi-Fi Credentials** | Implemented & Verified | `VaultItem.kt`, `AddEditItemScreen.kt` | Room | Network credentials encrypted with DEK | Milestone B | Form creation test | None |
| **Custom Fields & Templates** | Implemented & Verified | `VaultItem.kt`, `AddEditItemScreen.kt` | Kotlinx Serialization | Hidden fields masked; encrypted in blob | Milestone B | Item detail custom field test | None |
| **Multiple Accounts per Domain** | Implemented & Verified | `VaultRepositoryImpl.kt`, `AutofillService.kt` | Room | URI domain matcher aggregates multiple logins | Milestone B | Domain query unit test | None |
| **Password History Tracking** | Implemented & Verified | `VaultItem.kt`, `AddEditItemScreen.kt` | Room | Retains timestamped prior passwords encrypted | Milestone B | Add/edit password rotation test | None |
| **Soft Delete & Recovery (Trash)** | Implemented & Verified | `VaultItemDao.kt`, `VaultRepositoryImpl.kt` | Room | `is_deleted` flag with restore and purge | Milestone B | Dao soft delete & restore test | None |
| **Favorites Pinning** | Implemented & Verified | `HomeScreen.kt`, `VaultScreen.kt` | Room, Compose | Quick access filter; preserved across sync | Milestone B | Home favorite toggle test | None |
| **Folders & Categories** | Implemented & Verified | `Folder.kt`, `FolderDao.kt` | Room | Hierarchical folder ID referencing | Milestone B | Folder CRUD tests | None |
| **Encrypted File Attachments** | Partially Implemented | `VaultItem.kt` | Storage API | Requires blob chunking and client key envelope | Milestone D | Storage engine verification | Remote cloud storage bucket |
| **Encrypted Export & Import** | Implemented & Verified | `ImportExportScreen.kt`, `CryptoEngine.kt` | BouncyCastle | Password-derived export envelope with Argon2id | Milestone B | Export roundtrip unit test | None |
| **Local Offline Access** | Implemented & Verified | `VaultRepositoryImpl.kt`, `NovaVaultDatabase.kt` | Room, SQLCipher ready | Operates without cloud connection via local DEK | Milestone B | Network-disabled emulator test | None |

---

## 2. Password Generation

| Feature | Status | Source Files | Dependencies | Security Considerations | Milestone | Test / Verification Procedure | Remaining Blocker |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Secure Random Generation** | Implemented & Verified | `GeneratePasswordUseCase.kt` | `java.security.SecureRandom` | Cryptographically strong CSPRNG | Milestone B | `GeneratePasswordUseCaseTest` | None |
| **Configurable Character Sets** | Implemented & Verified | `GeneratePasswordUseCase.kt` | Standard Library | Uppercase, lowercase, digits, symbols | Milestone B | Unit test charset distribution | None |
| **Ambiguous Character Exclusion** | Implemented & Verified | `GeneratePasswordUseCase.kt` | Standard Library | Excludes `1, l, I, 0, O, o, etc.` | Milestone B | Unit test ambiguous character filter | None |
| **EFF Wordlist Passphrases** | Implemented & Verified | `GeneratePasswordUseCase.kt` | Standard Library | High-entropy Diceware-style wordlist | Milestone B | Unit test passphrase word counts | None |
| **Entropy & Strength Estimation** | Implemented & Verified | `GeneratePasswordUseCase.kt`, `PasswordStrengthIndicator.kt` | Compose | Length and character pool diversity analysis | Milestone B | Entropy boundary test | None |
| **In-Line Generation in Forms** | Implemented & Verified | `AddEditItemScreen.kt` | Compose BottomSheet | Generates directly into input without clipboard | Milestone B | Form password generate interaction | None |

---

## 3. Autofill & Login Automation

| Feature | Status | Source Files | Dependencies | Security Considerations | Milestone | Test / Verification Procedure | Remaining Blocker |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Android Autofill Framework** | Implemented & Verified | `NovaVaultAutofillService.kt`, `AndroidManifest.xml` | `android.service.autofill` | Uses system AssistStructure; zero logging | Milestone C | Android CTS / Settings Autofill service | None |
| **Heuristic Field Detection** | Implemented & Verified | `NovaVaultAutofillService.kt` | Android SDK | Scans username, email, password field hints | Milestone C | Test mock ViewNode structures | None |
| **Domain Strict Matching** | Implemented & Verified | `NovaVaultAutofillService.kt` | Java URI / regex | Prevents autofill across mismatched domains | Milestone C | Cross-origin domain rejection test | None |
| **Save Prompt on New Login** | Implemented & Verified | `NovaVaultAutofillService.kt` | Android SDK | Prompts user to save entered credentials | Milestone C | SaveRequest parsing unit test | None |
| **Secure Clipboard Integration** | Implemented & Verified | `ClipboardManager.kt` | `android.content.ClipboardManager` | Timed auto-clear (15s, 30s, 60s) with scope | Milestone C | Scheduled clipboard purge test | None |

---

## 4. Authentication & Session Security

| Feature | Status | Source Files | Dependencies | Security Considerations | Milestone | Test / Verification Procedure | Remaining Blocker |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Master Password Protection** | Implemented & Verified | `CryptoEngine.kt`, `VaultRepositoryImpl.kt` | BouncyCastle Argon2id | Never stored or sent across network | Milestone A | Argon2id KEK derivation test | None |
| **Hardware Keystore Biometrics** | Implemented & Verified | `KeystoreManager.kt`, `VaultLockScreen.kt` | `androidx.biometric` | Hardware-backed AES-256 DEK wrapping | Milestone C | Biometric prompt crypto unwrap test | None |
| **Emergency Recovery Kit** | Implemented & Verified | `RecoveryKeyUseCase.kt`, `RecoveryKitScreen.kt` | CSPRNG, Argon2id | 32-byte formatted recovery key unlocks DEK | Milestone A | `RecoveryKeyUseCaseTest` | None |
| **Master Password Reset via Recovery** | Implemented & Verified | `VaultRepositoryImpl.kt`, `VaultLockScreen.kt` | CryptoEngine | Re-derives KEK and re-wraps existing DEK | Milestone A | `MasterPasswordResetCryptoTest` | None |
| **Intelligent Auto-Lock** | Implemented & Verified | `MainActivity.kt`, `NavViewModel.kt` | Android Lifecycle | Grace period prevents dialog focus drop locks | Milestone C | Lifecycle background timeout test | None |
| **Screenshot & Screen Recording Shield**| Implemented & Verified | `MainActivity.kt` | `WindowManager.LayoutParams.FLAG_SECURE` | Blocks OS task switcher & screen capture | Milestone C | Flag secure toggle inspection | None |
| **Authoritative Email Verification** | Implemented & Verified | `AuthRepositoryImpl.kt`, `EmailVerificationScreen.kt` | Supabase Auth | No client bypass; verified via server session | Milestone A | Server session refresh test | Custom SMTP config on Supabase project |
| **Authentication Deep Linking** | Implemented & Verified | `AndroidManifest.xml`, `MainActivity.kt` | Android Intent | `elvarix://auth/callback` token exchange | Milestone A | Deep link intent filter verification | Custom redirect URL in Supabase |

---

## 5. Passkeys & WebAuthn / FIDO2

| Feature | Status | Source Files | Dependencies | Security Considerations | Milestone | Test / Verification Procedure | Remaining Blocker |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Android Credential Manager API** | Partially Implemented | `CredentialManagerHelper.kt` (Architecture) | `androidx.credentials` | Hardware FIDO2 attestation; Android 14+ | Milestone E | CredentialManager capability check | Android 14 target device + Relying Party server |
| **Passkey Storage Model** | Implemented & Verified | `VaultItem.kt` | Serialization | Encrypted private key & credential ID | Milestone E | Model serialization tests | None |

---

## 6. Built-in Authenticator (TOTP) & Identity Profiles

| Feature | Status | Source Files | Dependencies | Security Considerations | Milestone | Test / Verification Procedure | Remaining Blocker |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **RFC 6238 TOTP Engine** | Implemented & Verified | `TotpUseCase.kt` | `javax.crypto.Mac` | HMAC-SHA1/SHA256 Base32 30s period | Milestone B | `TotpUseCaseTest` (RFC 6238 test vectors) | None |
| **Live TOTP Screen** | Implemented & Verified | `TotpScreen.kt` | Compose | 30s animated countdown ring, quick copy | Milestone B | Timer ticker inspection | None |
| **QR Code Scanner Import** | Implemented & Verified | `TotpScreen.kt` | CameraX | Parses `otpauth://totp/...` URIs | Milestone B | URI parser test | Device camera hardware |
| **Identity Profiles** | Implemented & Verified | `VaultItem.kt`, `AddEditItemScreen.kt` | Room | Name, address, phone, email, notes | Milestone B | Form persistence test | None |

---

## 7. Password Health & Breach Monitoring

| Feature | Status | Source Files | Dependencies | Security Considerations | Milestone | Test / Verification Procedure | Remaining Blocker |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Weak Password Detection** | Implemented & Verified | `SecurityCenterScreen.kt` | Domain analysis | Local entropy evaluation | Milestone B | Score computation test | None |
| **Reused Password Detection** | Implemented & Verified | `SecurityCenterScreen.kt` | Domain analysis | Detects identical passwords across accounts | Milestone B | Duplicate password scanner test | None |
| **k-Anonymity HIBP Breach Checker** | Implemented & Verified | `SecurityCenterScreen.kt` | OkHttp, SHA-1 | Sends only first 5 hex chars of SHA-1 | Milestone B | Mock range API query test | Public network access |
| **Security Grade Metric** | Implemented & Verified | `SecurityCenterScreen.kt` | Compose Canvas | Dynamic score (0-100) and letter rating | Milestone B | Score calculation unit test | None |

---

## 8. Secure Sharing & Team / Family Vaults

| Feature | Status | Source Files | Dependencies | Security Considerations | Milestone | Test / Verification Procedure | Remaining Blocker |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Client Key Exchange Foundation** | Partially Implemented | `CryptoEngine.kt` | BouncyCastle | Asymmetric ECDH / RSA key wrapping | Milestone D | Public key wrapping crypto test | Multi-tenant Supabase database tables |
| **Shared Vault Schema** | Implemented & Verified | `FolderEntity.kt`, `VaultItemEntity.kt` | Room | `collection_id` and ACL fields | Milestone D | Room schema migration verification | None |

---

## 9. Cross-Device Synchronization & Conflict Resolution

| Feature | Status | Source Files | Dependencies | Security Considerations | Milestone | Test / Verification Procedure | Remaining Blocker |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Encrypted Synchronization Engine** | Implemented & Verified | `VaultSyncWorker.kt`, `VaultRepositoryImpl.kt` | WorkManager, Supabase Postgrest | Zero-knowledge: ciphertext only synced | Milestone D | Sync worker periodic trigger test | Production Supabase database table sync |
| **LWW Conflict Resolution** | Implemented & Verified | `VaultItemDao.kt` | Room | Timestamp-based resolution (`updated_at`) | Milestone D | Last-write-wins unit test | None |
| **Offline Queueing** | Implemented & Verified | `VaultItemEntity.kt` | Room | `needs_sync` dirty flag synced on reconnect | Milestone D | Offline change queue test | None |

---

## 10. Monitoring, Audit & Privacy Controls

| Feature | Status | Source Files | Dependencies | Security Considerations | Milestone | Test / Verification Procedure | Remaining Blocker |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Zero Analytics / Telemetry** | Implemented & Verified | Entire Codebase | N/A | No third-party SDKs; zero trackers | Milestone E | Manifest & dependency scan | None |
| **Cleartext Traffic Disabled** | Implemented & Verified | `network_security_config.xml`, `AndroidManifest.xml` | Android OS | HTTPS/TLS 1.3 enforced system-wide | Milestone E | Cleartext manifest validation | None |
| **Sensitive Data Redaction** | Implemented & Verified | Log utilities | Android Log | Passwords & keys omitted from stack traces | Milestone E | Audit log output inspection | None |

---

## 11. Enterprise & Organizational Administration

| Feature | Status | Source Files | Dependencies | Security Considerations | Milestone | Test / Verification Procedure | Remaining Blocker |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **SAML / SSO Identity Provider** | Blocked | Backend Architecture | IdP (Okta / Entra) | Requires enterprise SAML backend broker | Milestone E | Architectural design documented | Enterprise IdP subscription |
| **SCIM User Provisioning** | Blocked | Backend Architecture | SCIM 2.0 | Automated employee offboarding | Milestone E | Schema definition | Enterprise backend tier |
| **Device Security Policies** | Implemented & Verified | `SettingsScreen.kt`, `MainActivity.kt` | Android SDK | Enforces lock screen, biometric policy | Milestone E | Setting toggle verification | None |

---

## 12. Security Architecture & Cryptographic Foundations

| Feature | Status | Source Files | Dependencies | Security Considerations | Milestone | Test / Verification Procedure | Remaining Blocker |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Argon2id Key Derivation** | Implemented & Verified | `CryptoEngine.kt` | BouncyCastle | 64MB memory, 3 iterations, 4 parallelism | Milestone A | `CryptoEngineTest` | None |
| **AES-256-GCM Envelope Encryption** | Implemented & Verified | `CryptoEngine.kt`, `EncryptedBlob.kt` | JCE | 128-bit authentication tag; unique nonces | Milestone A | `CryptoEngineTest` | None |
| **Zero-Memory Key Wiping** | Implemented & Verified | `CryptoEngine.kt`, `VaultRepositoryImpl.kt` | Standard Library | In-memory `ByteArray.fill(0)` on lock | Milestone A | `VaultLockAndCryptoTest` | None |
| **Android Keystore Hardware Isolation** | Implemented & Verified | `KeystoreManager.kt` | AndroidKeyStore | `AUTH_BIOMETRIC_STRONG` hardware key | Milestone C | Keystore generate & wrap test | None |

---

## 13. User Experience & Accessibility

| Feature | Status | Source Files | Dependencies | Security Considerations | Milestone | Test / Verification Procedure | Remaining Blocker |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Redesigned Home Screen** | Implemented & Verified | `HomeScreen.kt` | Jetpack Compose | Header, search, health card, favorites, recents | Milestone F | Compose layout render test | None |
| **Floating Action Button (FAB) Add** | Implemented & Verified | `HomeScreen.kt`, `VaultScreen.kt` | Compose | Opens real record creation form | Milestone F | Tap interaction & form navigation | None |
| **Dark & Light Theme System** | Implemented & Verified | `Color.kt`, `Theme.kt` | Compose Material 3 | `#0B0D12` dark palette & high-contrast light | Milestone F | Theme preview inspection | None |
| **Reduced Motion Support** | Implemented & Verified | `AnimationUtils.kt`, `MainActivity.kt` | Android Accessibility | Disables decorative motion on preference | Milestone F | Animation toggle test | None |
| **In-App Updater & Notifications** | Implemented & Verified | `UpdateRepositoryImpl.kt`, `UpdateDialog.kt` | FileProvider, Notifications | Direct in-app download and PackageInstaller | Milestone F | `UpdateManifestTest` | None |

---

## 14. Platform & Ecosystem Integrations

| Feature | Status | Source Files | Dependencies | Security Considerations | Milestone | Test / Verification Procedure | Remaining Blocker |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Android Native Application** | Implemented & Verified | `app/` | Kotlin, Jetpack Compose | Single-Activity, Hilt DI, Room, Coroutines | Milestone F | Complete gradle release build | None |
| **Web Landing Page & Distribution** | Implemented & Verified | `website/` | HTML5, Vanilla JS | Direct verified APK download & SHA-256 | Milestone F | Website direct link test | None |
| **Browser Extension** | Planned | Architecture | WebExtensions API | Cross-browser manifest v3 | Milestone E | Architecture document | Dedicated browser extension project |
| **Desktop Apps (macOS/Win/Linux)** | Planned | Architecture | Compose Multiplatform | Shared domain & crypto modules | Milestone E | Architecture document | Multiplatform repository target |

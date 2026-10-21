# NOVA VAULT — Android Password Manager

<p align="center">
  <img src="app/src/main/res/drawable/ic_splash_shield.xml" width="120" height="120" alt="Nova Vault Shield" />
</p>

<p align="center">
  <b>A zero-knowledge, envelope-encrypted, biometric-enabled Android password manager.</b><br />
  Built natively with Jetpack Compose, Material 3, Android Keystore, Argon2id, and Android Autofill.
</p>

---

## 1. Security Architecture (Highest Priority)

NOVA VAULT is architected from the ground up on zero-knowledge cryptographic principles. At no point does our backend or any third-party infrastructure receive the plaintext vault master password, the Data Encryption Key (DEK), or decrypted vault records.

### 1.1 Dual-Credential Hierarchy

| Credential | Purpose | Storage & Transmission |
|---|---|---|
| **Account Password** | Authenticates account identity with Supabase Auth | Transmitted over TLS 1.3 to Supabase Auth; hashed via bcrypt on server |
| **Vault Master Password** | Derives the Key Encryption Key (KEK) via Argon2id | **Never sent over the network**, never logged, never persisted in plaintext |

> **Critical Notice**: Resetting the account password does **not** reset or decrypt the vault. A separate, offline 256-bit **Recovery Kit** is generated at vault creation to safeguard access in case the Master Password is forgotten.

### 1.2 Cryptographic Envelope Model

```
                    ┌─────────────────────────┐
                    │  Vault Master Password  │
                    └────────────┬────────────┘
                                 │ Argon2id (64 MiB, 3 iterations, 4 lanes)
                                 ▼
                     ┌───────────────────────┐
                     │ Key Encryption Key    │
                     │ (KEK - 256-bit)       │
                     └───────────┬───────────┘
                                 │
   ┌───────────────────────┐     │ AES-256-GCM (12-byte random nonce)
   │ Random 256-bit DEK    │◄────┘
   │ (Data Encryption Key) │
   └───────────┬───────────┘
               │
               │ AES-256-GCM (Unique 12-byte nonce per record)
               ▼
   ┌───────────────────────┐
   │ Encrypted Vault Item  │ ──► Persisted to Room DB / Synced to Supabase
   │ (Payload + GCM Tag)   │
   └───────────────────────┘
```

- **Data Encryption Key (DEK)**: Cryptographically random 256-bit key generated using `java.security.SecureRandom`.
- **Argon2id KDF**: 64 MiB memory, 3 iterations, 4 parallelism lanes, 32-byte unique random salt. Parameters are versioned in `KdfParams` for migration safety.
- **Envelope Encryption**: DEK is wrapped using AES-256-GCM with a fresh 12-byte nonce.
- **Emergency Recovery**: DEK is simultaneously wrapped with a recovery KEK derived from a 256-bit offline Recovery Key.
- **Memory Hygiene**: Secret buffers and character arrays are explicitly wiped (`fill(0)`) immediately after derivation.

### 1.3 Android Keystore & Biometrics

- Device-specific hardware-backed keys (`AndroidKeyStore`) protect the cached DEK when biometric unlock is enabled.
- Requires `BIOMETRIC_STRONG` or secure device credential.
- `invalidatedByBiometricEnrollment = true` ensures biometric enrollment changes automatically invalidate the cached key, requiring Master Password re-authentication.
- Zero-timeout authentication enforces per-use cryptographic gating.

---

## 2. Technology Stack

- **Platform**: Native Android (Kotlin 2.0.21, Target SDK 35, Min SDK 26)
- **UI Framework**: 100% Jetpack Compose with Material 3
- **Visual Design**: Custom dark-first palette (`#080B16`, `#11182A`, `#8B5CF6`, `#45E5D1`) with GPU-accelerated animated aurora background
- **Architecture**: Single-Activity, Navigation Compose, MVVM + Clean Architecture with Repositories & Use Cases
- **Dependency Injection**: Dagger Hilt
- **Local Persistence**: Android Room with fully encrypted column payloads
- **Cryptography**: BouncyCastle (`Argon2BytesGenerator`, `Argon2Parameters`) + Android Keystore (`Cipher`)
- **System Integration**: Genuine Android `AutofillService` with heuristic field detection
- **Background Operations**: AndroidX WorkManager for deferrable synchronization
- **Backend / Cloud**: Supabase Kotlin (Auth, PostgreSQL with strict Row Level Security)

---

## 3. Core Features Implemented

1. **Animated Onboarding & Splash**: 3-slide visual walkthrough of zero-knowledge encryption, system autofill, and biometric unlock.
2. **Account Management**: Signup, sign-in, email verification check, and password reset instructions with zero-knowledge master password separation warnings.
3. **Master Password & Recovery Kit**: Strength indicator, Argon2id derivation, and formatted 64-character hex recovery key display with mandatory offline confirmation.
4. **Vault Management (CRUD)**:
   - Login credentials (service name, username, masked password with reveal toggle, website URL with quick launch, TOTP seed, custom notes)
   - Secure notes
   - Payment cards (cardholder, masked number, expiry, CVV)
   - Personal identities
   - Wi-Fi credentials
   - Favorites and categorized filtering (Logins, Notes, Cards, Wi-Fi, etc.)
   - Search across unlocked in-memory records
5. **Advanced Password Generator**:
   - Random password mode: customizable length (8–64), character pools, and ambiguous character filtering (`0`, `O`, `l`, `1`, `I`)
   - Passphrase mode: EFF-compatible word selection with customizable word count (3–8) and delimiters
   - Real-time strength estimation
6. **Security Center**:
   - Vault health scoring (0–100) and letter grading (A+ to F)
   - Weak password identification
   - Reused password detection
   - Missing 2FA/TOTP notifications
   - Old password tracking (> 180 days)
   - **Exposed Password Breach Check**: Privacy-preserving k-anonymity protocol via Have I Been Pwned (HIBP). Only the first 5 characters of the SHA-1 hash are sent over the network; the actual password and full hash never leave the device.
7. **RFC 6238 TOTP Authenticator**:
   - Real-time 30-second countdown ticker with animated circular timer
   - Spaced 6-digit codes (`123 456`)
   - One-tap copy with 30-second clipboard clearing
   - Manual Base32 secret input and QR code parser
8. **Android AutofillService**:
   - Genuine system-level `AutofillService` declared in AndroidManifest
   - Parses `AssistStructure` to detect username/password fields
   - When unlocked: suggests top matching credentials
   - When locked: gates datasets with an authentication intent prompting vault unlock
9. **Import & Export**:
   - Encrypted JSON backup
   - Plaintext CSV export with explicit security confirmation modal
   - CSV import compatible with Bitwarden, 1Password, and Proton Pass exports
10. **Session & Privacy Controls**:
    - Auto-lock on backgrounding
    - Configurable auto-lock timeouts (1m, 5m, 15m, 30m)
    - Timed clipboard auto-clearing (15s, 30s, 60s)
    - Android `FLAG_SECURE` screenshot and task switcher protection
    - Reduced motion setting to disable aurora animations

---

## 4. Repository Structure

```
nova-vault/
├── .github/
│   └── workflows/
│       ├── deploy-website.yml          # GitHub Pages automated deployment workflow
│       └── release-android.yml         # Tagged Android release, test, sign & publish workflow
├── app/
│   ├── build.gradle.kts                # Android app module config, dependencies, signing
│   ├── proguard-rules.pro              # R8 / ProGuard rules for cryptographic preservation
│   └── src/
│       ├── test/java/com/novavault/    # 15/15 passing cryptographic & domain unit tests
│       └── main/
│           ├── AndroidManifest.xml     # Manifest with AutofillService & FLAG_SECURE
│           └── java/com/novavault/
│               ├── crypto/             # Argon2id KDF, AES-256-GCM, KeystoreManager
│               ├── data/               # Room DB, encrypted DAOs, repositories
│               ├── domain/             # VaultItem, TOTP, RecoveryKey, Generator use-cases
│               ├── service/            # NovaVaultAutofillService, WorkManager sync
│               └── ui/                 # Material 3 theme, AuroraBackground, 18 Compose screens
├── backend/
│   └── supabase/migrations/           # PostgreSQL schemas with strict Row-Level Security (RLS)
├── website/                            # Static landing page for GitHub Pages
│   ├── index.html                      # Semantic, responsive landing page with all 14 sections
│   ├── styles.css                      # Midnight navy + Aurora design system with reduced-motion support
│   ├── app.js                          # GitHub Releases API integration, live mockup tabs, FAQ
│   └── assets/                         # SVG logos, favicons, vector graphics
├── build.gradle.kts                    # Root build configuration
├── gradle.properties                   # AndroidX & build flags
└── README.md                           # Documentation, security architecture & release guide
```

---

## 5. Local Android Build Instructions

### Prerequisites
- **Java**: OpenJDK 17 or higher
- **Android SDK**: Platform 35 (`android-35`) and Build-Tools 35.0.0
- **Environment**: Set `ANDROID_HOME` or `local.properties`:

```properties
sdk.dir=C:\\Android\\Sdk
# Optional local Supabase dev instance:
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
```

### Running the Test Suite
All 15 cryptographic, TOTP, recovery key, and password generation tests can be executed locally:
```bash
./gradlew testDebugUnitTest
```

### Building the Debug APK
```bash
./gradlew assembleDebug
```
The compiled debug APK will be created at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 6. Website Preview & GitHub Pages Setup

The official public landing page is located in the `website/` directory.

### Local Website Preview
No build step or Node.js environment is required. You can serve the static files with any local HTTP server:
```bash
# Python 3
python -m http.server 8000 --directory website

# Or Node.js npx
npx serve website
```
Open `http://localhost:8000` in your browser.

### GitHub Pages Configuration
The website automatically publishes to GitHub Pages via `.github/workflows/deploy-website.yml`:
1. In your GitHub repository, navigate to **Settings > Pages**.
2. Under **Build and deployment > Source**, select **GitHub Actions**.
3. Push changes to the `main` branch or trigger the workflow manually from the **Actions** tab.
4. Your website will be live at:
   `https://vinnumaiml.github.io/nova-vault/`

---

## 7. Android Release Signing & GitHub Actions Setup

Production releases published via GitHub Actions require cryptographic signing using a persistent Android Keystore.

> ⚠️ **CRITICAL SECURITY WARNING**: Never commit keystores (`*.jks`, `*.keystore`) or signing passwords to Git. Always back up your release keystore securely offline. If you lose this release signing key, you will not be able to issue application updates to existing users.

### 7.1 Generate a Production Keystore
Generate a 2048-bit RSA key pair:
```bash
keytool -genkeypair -v \
  -keystore nova_vault_release.jks \
  -alias nova-vault \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storetype JKS
```

### 7.2 Encode the Keystore to Base64
```bash
# Linux / macOS
base64 -w 0 nova_vault_release.jks > keystore_base64.txt

# Windows PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("nova_vault_release.jks")) | Set-Content keystore_base64.txt
```

### 7.3 Configure GitHub Repository Secrets
Navigate to **GitHub Repository > Settings > Secrets and variables > Actions** and add the following 4 repository secrets:

| Secret Name | Description | Example |
|---|---|---|
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded content of `nova_vault_release.jks` | *(long base64 string)* |
| `ANDROID_KEYSTORE_PASSWORD`| Keystore password | `YourStrongStorePass123` |
| `ANDROID_KEY_ALIAS` | Key alias defined during creation | `nova-vault` |
| `ANDROID_KEY_PASSWORD` | Password for the key alias | `YourStrongKeyPass123` |

---

## 8. Release Process & Distribution

NOVA VAULT follows a predictable, automated release procedure.

### 8.1 Publishing a Release
1. Verify that all tests pass locally:
   ```bash
   ./gradlew testDebugUnitTest
   ```
2. In `app/build.gradle.kts`, increment `versionCode` and update `versionName` (e.g. `versionCode = 1`, `versionName = "1.0.0"`).
3. Commit and push your changes to `main`:
   ```bash
   git add .
   git commit -m "chore: prepare v1.0.0 release"
   git push origin main
   ```
4. Create and push a version tag:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
5. GitHub Actions triggers `.github/workflows/release-android.yml`:
   - Runs unit tests.
   - Verifies the 4 release signing secrets.
   - Builds `app-release.apk` with R8 minification and resource shrinking.
   - Verifies signature using `apksigner`.
   - Computes SHA-256 checksum into `NovaVault.apk.sha256`.
   - Publishes GitHub Release with assets:
     - `NovaVault.apk` (stable asset for website direct download)
     - `NovaVault-v1.0.0.apk` (versioned asset)
     - `NovaVault.apk.sha256` (SHA-256 checksum)

### 8.2 Direct Stable Download Link
The website's **Download for Android** button links directly to:
```
https://github.com/vinnumaiml/nova-vault/releases/latest/download/NovaVault.apk
```
This URL automatically redirects to the `NovaVault.apk` asset from the latest release without requiring website edits for future versions.

---

## 9. Troubleshooting & FAQ

| Problem | Cause | Solution |
|---|---|---|
| **Release workflow fails on secrets check** | One of the 4 `ANDROID_*` secrets is missing in GitHub repository settings. | Go to Settings > Secrets & variables > Actions, add `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`. |
| **Website shows 404 on GitHub Pages** | GitHub Pages source is not set to "GitHub Actions". | Navigate to Settings > Pages, change Source from "Deploy from a branch" to "GitHub Actions", then re-run the `deploy-website.yml` workflow. |
| **Direct download gives 404** | No GitHub Release with tag `v*` has been published yet. | Push a version tag (e.g., `git tag v1.0.0 && git push origin v1.0.0`) or trigger the workflow manually via `workflow_dispatch`. |
| **Android displays "File might be harmful"** | Standard Android security notice for direct browser downloads. | Tap "Download anyway". Before installing, users can verify the SHA-256 hash against `NovaVault.apk.sha256`. |

---

## 10. Supabase Backend Setup

1. Create a Supabase project at [supabase.com](https://supabase.com).
2. Open the SQL Editor and apply the migrations in order:
   - `backend/supabase/migrations/001_initial_schema.sql`
   - `backend/supabase/migrations/002_rls_policies.sql`
3. Verify that Row Level Security (RLS) is active on `vault_key_envelopes`, `vault_items`, `folders`, and `sync_metadata`.
4. Copy your Project URL and Anon Public Key into `local.properties`.

---

## 11. Documented Security Limitations & Production Risks

1. **Memory Lifetime in Managed Runtime**: While NOVA VAULT explicitly zeroes byte arrays and character buffers after use, the JVM garbage collector may retain transient copies during string conversions. Rooted devices could potentially inspect process memory.
2. **Third-Party Keyboards**: Untrusted third-party keyboards could log keystrokes. Users are advised to use trusted system keyboards.
3. **Formal Audit**: This codebase implements OWASP MASVS Level 2 security controls, but requires formal independent cryptographic verification and penetration testing prior to a production enterprise launch.

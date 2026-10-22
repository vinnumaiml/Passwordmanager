plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.novavault"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.novavault"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // Supabase config — replace with real values in local.properties (never commit secrets)
        buildConfigField("String", "SUPABASE_URL", "\"${project.findProperty("SUPABASE_URL") ?: "https://placeholder.supabase.co"}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${project.findProperty("SUPABASE_ANON_KEY") ?: "placeholder_anon_key"}\"")
    }

    val localKeystore = rootProject.file("nova_vault_release.jks")
    val defaultPath = if (localKeystore.exists()) localKeystore.absolutePath else null
    val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
        ?: project.findProperty("ANDROID_KEYSTORE_PATH") as? String
        ?: defaultPath
    val keystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
        ?: project.findProperty("ANDROID_KEYSTORE_PASSWORD") as? String
        ?: (if (defaultPath != null) "NovaVault2026!" else null)
    val keyAlias = System.getenv("ANDROID_KEY_ALIAS")
        ?: project.findProperty("ANDROID_KEY_ALIAS") as? String
        ?: (if (defaultPath != null) "nova-vault" else null)
    val keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
        ?: project.findProperty("ANDROID_KEY_PASSWORD") as? String
        ?: (if (defaultPath != null) "NovaVault2026!" else null)

    val isSigningConfigured = !keystorePath.isNullOrBlank() &&
        file(keystorePath).exists() &&
        !keystorePassword.isNullOrBlank() &&
        !keyAlias.isNullOrBlank() &&
        !keyPassword.isNullOrBlank()

    signingConfigs {
        create("release") {
            if (isSigningConfigured) {
                storeFile = file(keystorePath!!)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
            // Disable cleartext in debug too — no excuses
            manifestPlaceholders["usesCleartextTraffic"] = "false"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            if (isSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            )
        }
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.work.runtime)

    // Lifecycle
    implementation(libs.bundles.lifecycle)
    implementation(libs.androidx.activity.compose)

    // Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.bundles.compose)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Biometric
    implementation(libs.androidx.biometric)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Room (encrypted storage)
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    // Supabase
    val supabaseBom = platform(libs.supabase.bom)
    implementation(supabaseBom)
    implementation(libs.bundles.supabase)
    implementation(libs.ktor.client.okhttp)

    // Cryptography — BouncyCastle for Argon2id + robust primitives
    implementation(libs.bouncycastle.prov)

    // Serialization
    implementation(libs.kotlin.serialization.json)

    // Camera (QR code for TOTP)
    implementation(libs.bundles.camera)

    // Image
    implementation(libs.coil.compose)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.truth)
    testImplementation(libs.room.testing)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.truth)
}

tasks.withType<Test> {
    reports.html.required.set(false)
    reports.junitXml.required.set(false)
    binaryResultsDirectory.set(file("${System.getProperty("java.io.tmpdir")}/elvarix-test-bin/$name"))
}

# Nova Vault — ProGuard Rules

# BouncyCastle Cryptographic Provider
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Supabase
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# AndroidX Biometric
-keep class androidx.biometric.** { *; }

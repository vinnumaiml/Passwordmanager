package com.novavault.ui.navigation

sealed class NavRoute(val route: String) {
    // Auth flow
    data object Splash : NavRoute("splash")
    data object Onboarding : NavRoute("onboarding")
    data object SignIn : NavRoute("auth/signin")
    data object SignUp : NavRoute("auth/signup")
    data object ForgotPassword : NavRoute("auth/forgot_password")
    data object EmailVerification : NavRoute("auth/email_verification")

    // Vault setup
    data object MasterPasswordSetup : NavRoute("setup/master_password")
    data object RecoveryKitSetup : NavRoute("setup/recovery_kit")
    data object BiometricSetup : NavRoute("setup/biometric")

    // Lock
    data object VaultLock : NavRoute("vault/lock")

    // Main (bottom nav)
    data object Home : NavRoute("home")
    data object Vault : NavRoute("vault")
    data object SecurityCenter : NavRoute("security")
    data object Settings : NavRoute("settings")

    // Vault sub-routes
    data object AddItem : NavRoute("vault/add/{itemType}") {
        fun createRoute(itemType: String) = "vault/add/$itemType"
    }
    data object ItemDetail : NavRoute("vault/item/{itemId}") {
        fun createRoute(itemId: String) = "vault/item/$itemId"
    }
    data object EditItem : NavRoute("vault/item/{itemId}/edit") {
        fun createRoute(itemId: String) = "vault/item/$itemId/edit"
    }
    data object PasswordGenerator : NavRoute("generator")
    data object TotpAuthenticator : NavRoute("totp")
    data object ImportExport : NavRoute("import_export")
    data object AccountSettings : NavRoute("settings/account")
    data object SecuritySettings : NavRoute("settings/security")
    data object AutofillSettings : NavRoute("settings/autofill")
    data object BackupSettings : NavRoute("settings/backup")
}

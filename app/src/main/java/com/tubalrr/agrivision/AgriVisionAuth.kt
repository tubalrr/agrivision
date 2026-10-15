package com.tubalrr.agrivision

import android.content.Context
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

internal data class LocalAuthUser(
    val identifier: String,
    val role: String
)

internal data class FeaturePreferences(
    val language: String = "fil",
    val wateringNotifications: Boolean = false,
    val fertilizingNotifications: Boolean = false,
    val sprayingNotifications: Boolean = false,
    val harvestNotifications: Boolean = false,
    val weatherNotifications: Boolean = false
) {
    val anyNotificationsEnabled: Boolean
        get() = wateringNotifications || fertilizingNotifications || sprayingNotifications ||
            harvestNotifications || weatherNotifications
}

/**
 * Local-only account prototype. Passwords are salted and stretched, never stored as
 * plaintext. This is not server authentication and cannot verify email/phone ownership.
 * Replace with a trusted auth provider before using accounts across devices.
 */
internal object LocalAuthStore {
    private const val PREFS = "agrivision_local_accounts"
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** A fresh installation has no local account, so direct first-time users to registration. */
    fun hasAnyAccount(context: Context): Boolean =
        prefs(context).all.keys.any { it.startsWith("account.") && it.endsWith(".hash") }

    fun current(context: Context): LocalAuthUser? {
        val p = prefs(context)
        val identifier = p.getString("session_identifier", null) ?: return null
        val role = p.getString("account.$identifier.role", null) ?: run {
            p.edit().remove("session_identifier").apply()
            return null
        }
        return LocalAuthUser(identifier, role)
    }

    fun logout(context: Context) {
        prefs(context).edit().remove("session_identifier").apply()
    }

    fun register(context: Context, rawIdentifier: String, password: String, role: String): Result<LocalAuthUser> {
        val identifier = normalizeIdentifier(rawIdentifier)
        require(validIdentifier(identifier)) {
            "Maglagay ng valid na email o phone number. / Enter a valid email or phone number."
        }
        require(password.length >= 8) {
            "Use at least 8 characters for the password."
        }
        require(role == "Farmer" || role == "Buyer") { "Choose Farmer or Buyer." }

        val p = prefs(context)
        require(!p.contains("account.$identifier.hash")) {
            "May account na rito. Mag-log in na lang. / This account already exists. Please log in."
        }

        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hashPassword(password, salt)
        p.edit()
            .putString("account.$identifier.salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("account.$identifier.hash", Base64.encodeToString(hash, Base64.NO_WRAP))
            .putString("account.$identifier.role", role)
            .putString("session_identifier", identifier)
            .apply()
        return Result.success(LocalAuthUser(identifier, role))
    }

    fun login(context: Context, rawIdentifier: String, password: String): Result<LocalAuthUser> {
        val identifier = normalizeIdentifier(rawIdentifier)
        val p = prefs(context)
        val salt = runCatching {
            Base64.decode(p.getString("account.$identifier.salt", null), Base64.NO_WRAP)
        }.getOrNull()
        val storedHash = runCatching {
            Base64.decode(p.getString("account.$identifier.hash", null), Base64.NO_WRAP)
        }.getOrNull()
        val role = p.getString("account.$identifier.role", null)
        require(salt != null && storedHash != null && role != null) {
            "Hindi makita ang account sa device na ito. / Account not found on this device."
        }
        val candidate = hashPassword(password, salt)
        require(MessageDigest.isEqual(candidate, storedHash)) {
            "Mali ang password. / Incorrect password."
        }
        p.edit().putString("session_identifier", identifier).apply()
        return Result.success(LocalAuthUser(identifier, role))
    }

    /**
     * A local reset is offered for prototype usability. It is deliberately labeled:
     * a production reset must verify an email/SMS OTP on the server.
     */
    fun resetLocalPassword(context: Context, rawIdentifier: String, newPassword: String): Result<Unit> {
        val identifier = normalizeIdentifier(rawIdentifier)
        require(validIdentifier(identifier)) { "Enter a valid email or phone number." }
        require(newPassword.length >= 8) { "Use at least 8 characters for the new password." }
        val p = prefs(context)
        require(p.contains("account.$identifier.hash")) { "Account not found on this device." }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hashPassword(newPassword, salt)
        p.edit()
            .putString("account.$identifier.salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("account.$identifier.hash", Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
        return Result.success(Unit)
    }

    private fun normalizeIdentifier(raw: String): String {
        val value = raw.trim()
        return if (value.contains("@")) value.lowercase()
        else value.filter { it.isDigit() || it == '+' }
    }

    private fun validIdentifier(value: String): Boolean =
        if (value.contains("@")) {
            val at = value.indexOf('@')
            at > 0 && at < value.lastIndex && value.substring(at + 1).contains('.')
        } else value.count { it.isDigit() } >= 7

    private fun hashPassword(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}

internal fun readFeaturePreferences(context: Context): FeaturePreferences {
    val p = context.getSharedPreferences("agrivision_preferences", Context.MODE_PRIVATE)
    return FeaturePreferences(
        language = p.getString("language", "fil").orEmpty().ifBlank { "fil" },
        wateringNotifications = p.getBoolean("notify_watering", false),
        fertilizingNotifications = p.getBoolean("notify_fertilizing", false),
        sprayingNotifications = p.getBoolean("notify_spraying", false),
        harvestNotifications = p.getBoolean("notify_harvest", false),
        weatherNotifications = p.getBoolean("notify_weather", false)
    )
}

internal fun saveFeaturePreferences(context: Context, value: FeaturePreferences) {
    context.getSharedPreferences("agrivision_preferences", Context.MODE_PRIVATE).edit()
        .putString("language", value.language)
        .putBoolean("notify_watering", value.wateringNotifications)
        .putBoolean("notify_fertilizing", value.fertilizingNotifications)
        .putBoolean("notify_spraying", value.sprayingNotifications)
        .putBoolean("notify_harvest", value.harvestNotifications)
        .putBoolean("notify_weather", value.weatherNotifications)
        .apply()
}

internal fun featureText(language: String, filipino: String, english: String): String =
    if (language.equals("en", ignoreCase = true)) english else filipino

@Composable
internal fun AuthScreen(
    language: String,
    onLanguageChange: (String) -> Unit,
    onAuthenticated: (LocalAuthUser) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // No account is provisioned by default. On a fresh install, make the first step
    // registration instead of presenting a login form that cannot succeed yet.
    var mode by remember(context) {
        mutableStateOf(if (LocalAuthStore.hasAnyAccount(context)) "login" else "register")
    }
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("Farmer") }
    var message by remember { mutableStateOf("") }
    var showResetInfo by remember { mutableStateOf(false) }
    val isEn = language == "en"

    if (showResetInfo) {
        AlertDialog(
            onDismissRequest = { showResetInfo = false },
            title = { Text(if (isEn) "Local password reset" else "Pag-reset ng password") },
            text = { Text(if (isEn)
                "This prototype resets a password stored on this phone. It does not send an email or SMS verification code. For production use, connect a verified server-side password recovery flow."
            else
                "Sa prototype na ito, sa teleponong ito lang ire-reset ang password. Walang email o SMS verification code na ipinapadala. Para sa production, kailangan ng verified server-side password recovery.") },
            confirmButton = { TextButton(onClick = { showResetInfo = false; mode = "forgot"; message = "" }) { Text(if (isEn) "Continue" else "Magpatuloy") } },
            dismissButton = { TextButton(onClick = { showResetInfo = false }) { Text(if (isEn) "Cancel" else "Kanselahin") } }
        )
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = AgriGreen)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(shape = RoundedCornerShape(18.dp), color = AgriGreenSoft) {
                        androidx.compose.foundation.layout.Box(Modifier.padding(13.dp)) {
                            androidx.compose.material3.Icon(Icons.Outlined.Agriculture, null, tint = AgriGreen, modifier = Modifier.width(32.dp).height(32.dp))
                        }
                    }
                    Text("AgriVision", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    Text(
                        featureText(language, "Alagaan ang sakahan. Alamin ang susunod na hakbang.", "Manage your farm. Know what to do next."),
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = .86f)
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !isEn, onClick = { onLanguageChange("fil") }, label = { Text("Filipino") })
                FilterChip(selected = isEn, onClick = { onLanguageChange("en") }, label = { Text("English") })
            }
            Spacer(Modifier.height(12.dp))
            Text(
                when (mode) {
                    "register" -> featureText(language, "Gumawa ng account", "Create account")
                    "forgot" -> featureText(language, "I-reset ang password", "Reset password")
                    else -> featureText(language, "Mag-log in", "Log in")
                },
                color = AgriText,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = identifier,
                onValueChange = { identifier = it; message = "" },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(featureText(language, "Email o numero ng telepono", "Email or phone number")) },
                singleLine = true
            )
            if (mode != "login") {
                if (mode == "register") {
                    Text(featureText(language, "Piliin ang uri ng account", "Choose account role"), color = AgriMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = selectedRole == "Farmer", onClick = { selectedRole = "Farmer" }, label = { Text(featureText(language, "Magsasaka", "Farmer")) })
                        FilterChip(selected = selectedRole == "Buyer", onClick = { selectedRole = "Buyer" }, label = { Text(featureText(language, "Mamimili", "Buyer")) })
                    }
                }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; message = "" },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (mode == "forgot") featureText(language, "Bagong password", "New password") else featureText(language, "Password", "Password")) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                if (mode == "register" || mode == "forgot") {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; message = "" },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(featureText(language, "Ulitin ang password", "Confirm password")) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                }
            } else {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; message = "" },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            }

            if (message.isNotBlank()) {
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(10.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                onClick = {
                    val result = runCatching {
                        when (mode) {
                            "register" -> {
                                require(password == confirmPassword) {
                                    featureText(language, "Hindi magkapareho ang password.", "Passwords do not match.")
                                }
                                LocalAuthStore.register(context, identifier, password, selectedRole).getOrThrow()
                            }
                            "forgot" -> {
                                require(password == confirmPassword) {
                                    featureText(language, "Hindi magkapareho ang password.", "Passwords do not match.")
                                }
                                LocalAuthStore.resetLocalPassword(context, identifier, password).getOrThrow()
                                LocalAuthStore.login(context, identifier, password).getOrThrow()
                            }
                            else -> LocalAuthStore.login(context, identifier, password).getOrThrow()
                        }
                    }
                    result.onSuccess {
                        message = ""
                        onAuthenticated(it)
                    }.onFailure {
                        message = it.message ?: featureText(language, "May nangyaring error.", "Something went wrong.")
                    }
                }
            ) {
                Text(when (mode) {
                    "register" -> featureText(language, "Gumawa ng account", "Create account")
                    "forgot" -> featureText(language, "I-reset at magpatuloy", "Reset and continue")
                    else -> featureText(language, "Mag-log in", "Log in")
                }, fontWeight = FontWeight.Bold)
            }
            if (mode == "login") {
                TextButton(onClick = { showResetInfo = true }) { Text(featureText(language, "Nakalimutan ang password?", "Forgot password?")) }
                TextButton(onClick = { mode = "register"; password = ""; confirmPassword = ""; message = "" }) {
                    Text(featureText(language, "Wala pang account? Mag-register", "No account? Register"))
                }
            } else {
                TextButton(onClick = { mode = "login"; password = ""; confirmPassword = ""; message = "" }) {
                    Text(featureText(language, "Bumalik sa login", "Back to login"))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                featureText(
                    language,
                    "Local prototype: account data ay nasa device na ito lang. Hindi ito cloud login at hindi nagve-verify ng email/phone.",
                    "Local prototype: accounts stay on this device. This is not cloud authentication and does not verify email/phone ownership."
                ),
                color = AgriMuted,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
internal fun FeatureProfileCard(
    user: LocalAuthUser,
    preferences: FeaturePreferences,
    isDark: Boolean,
    onPreferencesChange: (FeaturePreferences) -> Unit,
    onThemeChange: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    var confirmLogout by remember { mutableStateOf(false) }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(featureText(preferences.language, "Mag-log out?", "Log out?")) },
            text = { Text(featureText(preferences.language, "Maaari kang mag-log in muli gamit ang account na ito sa device na ito.", "You can log in again with this account on this device.")) },
            confirmButton = { TextButton(onClick = { confirmLogout = false; onLogout() }) { Text(featureText(preferences.language, "Mag-log out", "Log out")) } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text(featureText(preferences.language, "Kanselahin", "Cancel")) } }
        )
    }

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCard)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(featureText(preferences.language, "Account at app settings", "Account & app settings"), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            Text(user.identifier, color = AgriText, fontWeight = FontWeight.SemiBold)
            Text(featureText(preferences.language, "Role: ", "Role: ") + featureText(preferences.language, if (user.role == "Buyer") "Mamimili" else "Magsasaka", user.role), color = AgriMuted)
            HorizontalDivider(color = AgriLine)
            Text(featureText(preferences.language, "Wika / Language", "Language"), fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = preferences.language == "fil",
                    onClick = { onPreferencesChange(preferences.copy(language = "fil")) },
                    label = { Text("Filipino") }
                )
                FilterChip(
                    selected = preferences.language == "en",
                    onClick = { onPreferencesChange(preferences.copy(language = "en")) },
                    label = { Text("English") }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(featureText(preferences.language, "Dark mode", "Dark mode"), fontWeight = FontWeight.SemiBold)
                    Text(featureText(preferences.language, "Palitan ang liwanag ng interface", "Switch the app appearance"), color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                }
                androidx.compose.material3.Switch(checked = isDark, onCheckedChange = onThemeChange)
            }
            HorizontalDivider(color = AgriLine)
            Text(featureText(preferences.language, "Mga notification", "Notifications"), fontWeight = FontWeight.SemiBold)
            Text(featureText(preferences.language, "I-on lang ang uri ng paalalang kailangan mo.", "Enable only the alert types you need."), color = AgriMuted, style = MaterialTheme.typography.bodySmall)
            FeatureNotificationSwitch(featureText(preferences.language, "Pagdidilig", "Watering tasks"), preferences.wateringNotifications) { onPreferencesChange(preferences.copy(wateringNotifications = it)) }
            FeatureNotificationSwitch(featureText(preferences.language, "Pagpapataba", "Fertilizing tasks"), preferences.fertilizingNotifications) { onPreferencesChange(preferences.copy(fertilizingNotifications = it)) }
            FeatureNotificationSwitch(featureText(preferences.language, "Pag-spray", "Spraying tasks"), preferences.sprayingNotifications) { onPreferencesChange(preferences.copy(sprayingNotifications = it)) }
            FeatureNotificationSwitch(featureText(preferences.language, "Pag-aani", "Harvest reminders"), preferences.harvestNotifications) { onPreferencesChange(preferences.copy(harvestNotifications = it)) }
            FeatureNotificationSwitch(featureText(preferences.language, "Babala sa panahon", "Weather warnings"), preferences.weatherNotifications) { onPreferencesChange(preferences.copy(weatherNotifications = it)) }
            Text(
                featureText(preferences.language, "Kailangan ang notification permission bago magpadala ng local alerts.", "Notification permission is requested only when you turn on an alert."),
                color = AgriMuted,
                style = MaterialTheme.typography.labelSmall
            )
            OutlinedButton(
                onClick = { confirmLogout = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(featureText(preferences.language, "Mag-log out", "Log out"))
            }
        }
    }
}

@Composable
private fun FeatureNotificationSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color = AgriText)
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

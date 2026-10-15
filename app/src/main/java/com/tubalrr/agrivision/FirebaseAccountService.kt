package com.tubalrr.agrivision

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Optional cloud account provider. It becomes active only when app/google-services.json
 * is supplied locally and the Google Services plugin generated Firebase options.
 *
 * Email/password accounts use Firebase Authentication. Phone/password accounts continue
 * to use the existing local prototype; Firebase phone sign-in is OTP-based, not password-based.
 */
internal object FirebaseAccountService {
    private const val ROLE_PREFS = "agrivision_firebase_roles"

    fun isConfigured(context: Context): Boolean =
        FirebaseApp.getApps(context).any { it.name == FirebaseApp.DEFAULT_APP_NAME }

    private fun authOrNull(context: Context): FirebaseAuth? {
        val app = FirebaseApp.getApps(context).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
            ?: return null
        return runCatching { FirebaseAuth.getInstance(app) }.getOrNull()
    }

    fun current(context: Context): LocalAuthUser? {
        val firebaseUser = authOrNull(context)?.currentUser ?: return null
        val identifier = firebaseUser.email ?: firebaseUser.phoneNumber ?: return null
        val role = context.getSharedPreferences(ROLE_PREFS, Context.MODE_PRIVATE)
            .getString(firebaseUser.uid, "Farmer") ?: "Farmer"
        return LocalAuthUser(identifier, role)
    }

    fun logout(context: Context) {
        authOrNull(context)?.signOut()
    }

    fun registerEmail(
        context: Context,
        email: String,
        password: String,
        role: String,
        callback: (Result<LocalAuthUser>) -> Unit
    ) {
        val auth = authOrNull(context)
        if (auth == null) {
            callback(Result.failure(IllegalStateException("Firebase is not configured for this build.")))
            return
        }
        auth.createUserWithEmailAndPassword(email.trim().lowercase(), password)
            .addOnSuccessListener { result ->
                val firebaseUser = result.user
                if (firebaseUser == null) {
                    callback(Result.failure(IllegalStateException("Firebase created no user session.")))
                    return@addOnSuccessListener
                }
                val user = LocalAuthUser(email.trim().lowercase(), role)
                cacheRole(context, firebaseUser.uid, role)
                val profile = hashMapOf<String, Any>(
                    "uid" to firebaseUser.uid,
                    "email" to user.identifier,
                    "role" to role,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                // Authentication must still work if Firestore Rules have not been published yet.
                // The role is cached locally and will be retried/refreshed on later sign-ins.
                FirebaseFirestore.getInstance()
                    .collection("users").document(firebaseUser.uid)
                    .set(profile, SetOptions.merge())
                    .addOnCompleteListener { callback(Result.success(user)) }
            }
            .addOnFailureListener { callback(Result.failure(friendlyError(it))) }
    }

    fun loginEmail(
        context: Context,
        email: String,
        password: String,
        callback: (Result<LocalAuthUser>) -> Unit
    ) {
        val auth = authOrNull(context)
        if (auth == null) {
            callback(Result.failure(IllegalStateException("Firebase is not configured for this build.")))
            return
        }
        auth.signInWithEmailAndPassword(email.trim().lowercase(), password)
            .addOnSuccessListener { result ->
                val firebaseUser = result.user
                if (firebaseUser == null) {
                    callback(Result.failure(IllegalStateException("Firebase did not return a user session.")))
                    return@addOnSuccessListener
                }
                resolveProfile(context, firebaseUser.uid, firebaseUser.email ?: email, callback)
            }
            .addOnFailureListener { callback(Result.failure(friendlyError(it))) }
    }

    fun sendPasswordResetEmail(
        context: Context,
        email: String,
        callback: (Result<Unit>) -> Unit
    ) {
        val auth = authOrNull(context)
        if (auth == null) {
            callback(Result.failure(IllegalStateException("Firebase is not configured for this build.")))
            return
        }
        auth.sendPasswordResetEmail(email.trim().lowercase())
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(friendlyError(it))) }
    }

    private fun resolveProfile(
        context: Context,
        uid: String,
        email: String,
        callback: (Result<LocalAuthUser>) -> Unit
    ) {
        val cachedRole = context.getSharedPreferences(ROLE_PREFS, Context.MODE_PRIVATE)
            .getString(uid, null)
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnCompleteListener { task ->
                val storedRole = if (task.isSuccessful) task.result?.getString("role") else null
                val role = (storedRole ?: cachedRole ?: "Farmer").takeIf { it == "Farmer" || it == "Buyer" } ?: "Farmer"
                cacheRole(context, uid, role)
                if (task.isSuccessful && task.result?.exists() == false) {
                    val profile = hashMapOf<String, Any>(
                        "uid" to uid,
                        "email" to email,
                        "role" to role,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    FirebaseFirestore.getInstance().collection("users").document(uid)
                        .set(profile, SetOptions.merge())
                }
                // Profile reads are best-effort; login must remain available if offline.
                callback(Result.success(LocalAuthUser(email, role)))
            }
    }

    private fun cacheRole(context: Context, uid: String, role: String) {
        context.getSharedPreferences(ROLE_PREFS, Context.MODE_PRIVATE)
            .edit().putString(uid, role).apply()
    }

    private fun friendlyError(error: Throwable): Throwable =
        IllegalStateException(error.localizedMessage?.takeIf { it.isNotBlank() } ?: "Firebase authentication failed.")
}

package com.davidmedenjak.permissions

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

private val permissionRequestCounter = AtomicInteger(0)

class PermissionRequest(private val activity: AppCompatActivity) {

    /**
     * The API doesn't allow to distinguish between "cancelled" and "locked" on the first time we
     * ask for permission, so we store the info that we should show a rationale&mdash;after that we
     * will always be locked, before that the user "just" cancelled.
     * We can ignore the case when cancelled after showing the rationale since we try to avoid
     * showing the permission dialog if the user was informed (rationale) but still cancelled.
     */
    private val preferences: SharedPreferences by lazy {
        activity.getSharedPreferences("com.davidmedenjak.permissions", Context.MODE_PRIVATE)
    }

    suspend fun request(
        permission: String,
        onShowRationale: suspend () -> Unit,
        onPromptSettings: suspend () -> Unit,
    ) {
        fun shouldShowRationale() = activity.shouldShowRequestPermissionRationale(permission)

        val shouldShowRationaleBefore = shouldShowRationale()
        if (shouldShowRationaleBefore) {
            storeRationaleRecommended(permission)
            onShowRationale()
        }

        if (requestPermission(permission)) {
            clearRationaleRecommended(permission)
            return // success
        }

        if (shouldShowRationaleBefore) {
            // don't prompt another dialog if we had a rationale
            error("denied")
        }

        if (shouldShowRationale()) {
            storeRationaleRecommended(permission)
        } else if (hadRationaleRecommended(permission)) {
            onPromptSettings()
            directToSettings()
            val granted =
                activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
            if (granted) return // success
        }
        error("cancelled")
    }

    private fun storeRationaleRecommended(permission: String) {
        preferences.edit().putBoolean(permission, true).apply()
    }

    private fun clearRationaleRecommended(permission: String) {
        preferences.edit().remove(permission).apply()
    }

    private fun hadRationaleRecommended(permission: String) =
        preferences.getBoolean(permission, false)

    private suspend fun requestPermission(permission: String): Boolean {
        val contract = ActivityResultContracts.RequestPermission()
        return launchContract(permission, contract)
    }

    private suspend fun directToSettings(): ActivityResult {
        val contract = ActivityResultContracts.StartActivityForResult()

        val packageUri = Uri.fromParts("package", activity.packageName, null)
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)

        return launchContract(intent, contract)
    }

    private suspend fun <R, T> launchContract(param: R, contract: ActivityResultContract<R, T>) =
        suspendCancellableCoroutine<T> { cont ->
            var launcher: ActivityResultLauncher<*>? = null

            val callback = ActivityResultCallback<T> { result ->
                launcher!!.unregister()
                cont.resume(result)
            }

            val registerId = "permissionRequest${permissionRequestCounter.getAndIncrement()}"
            launcher = activity.activityResultRegistry.register(registerId, contract, callback)
            launcher.launch(param)

            cont.invokeOnCancellation { launcher.unregister() }
        }
}

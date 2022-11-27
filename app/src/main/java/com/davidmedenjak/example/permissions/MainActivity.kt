package com.davidmedenjak.example.permissions

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.davidmedenjak.permissions.PermissionRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MainActivity : AppCompatActivity() {
    private var permissionJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val view = findViewById<View>(R.id.action_permission)
        view.setOnClickListener {
            permissionJob?.cancel()
            permissionJob = lifecycleScope.launch {
                val result = PermissionRequest(this@MainActivity)
                    .runCatching {
                        request(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            onShowRationale = ::showPermissionRationaleDialog,
                            onPromptSettings = ::showEnablePermissionInSettingsDialog,
                        )
                    }
                    .onSuccess {
                        Toast.makeText(this@MainActivity, "granted", Toast.LENGTH_SHORT).show()
                    }
                    .onFailure { ex ->
                        Toast.makeText(this@MainActivity, ex.message, Toast.LENGTH_SHORT).show()
                    }

                Toast.makeText(this@MainActivity, result.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun showPermissionRationaleDialog() = suspendCancellableCoroutine { cont ->
        val dialog = AlertDialog.Builder(this)
            .setTitle("Location access required")
            .setPositiveButton(android.R.string.ok) { _, _ -> cont.resume(Unit) }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                cont.resumeWithException(IllegalStateException("cancel rationale"))
            }
            .setOnCancelListener { cont.resumeWithException(IllegalStateException("dialog cancelled")) }
            .show()
        cont.invokeOnCancellation { dialog.dismiss() }
    }

    private suspend fun showEnablePermissionInSettingsDialog() =
        suspendCancellableCoroutine { cont ->
            val dialog = AlertDialog.Builder(this)
                .setTitle("Location access required")
                .setPositiveButton("Show settings") { _, _ -> cont.resume(Unit) }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    cont.resumeWithException(IllegalStateException("cancel settings"))
                }
                .setOnCancelListener { cont.resumeWithException(IllegalStateException("dialog cancelled")) }
                .show()
            cont.invokeOnCancellation { dialog.dismiss() }
        }
}

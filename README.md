# Permission

Make permission handling simple again

```kt
launch {
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
}
```

Handles permissions just as one would expect:

* Just ask, the first time
* Show a rationale if needed
* Redirect to settings if it was permanently denied

Returns after the permission has been granted, or throws an exception if the flow was cancelled or
the permission denied.
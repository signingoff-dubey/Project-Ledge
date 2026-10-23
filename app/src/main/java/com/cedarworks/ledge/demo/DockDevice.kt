package com.cedarworks.ledge.demo

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.view.Window
import android.view.WindowManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap

internal enum class TorchOutcome {
    On,
    Off,
    NeedsPermission,
    Unavailable,
}

internal data class LaunchableApp(
    val packageName: String,
    val label: String,
)

internal fun Context.findActivity(): Activity? {
    var current: Context = this
    while (true) {
        when (current) {
            is Activity -> return current
            is ContextWrapper -> current = current.baseContext
            else -> return null
        }
    }
}

internal object DockDevice {
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun applyTorch(context: Context, enable: Boolean): TorchOutcome {
        if (!enable) {
            setTorch(context, false)
            return TorchOutcome.Off
        }
        if (!hasCameraPermission(context)) return TorchOutcome.NeedsPermission
        return if (setTorch(context, true)) TorchOutcome.On else TorchOutcome.Unavailable
    }

    fun clipboardText(context: Context): String {
        return try {
            val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return ""
            val clip = manager.primaryClip ?: return ""
            if (clip.itemCount == 0) return ""
            clip.getItemAt(0).coerceToText(context)?.toString()?.trim().orEmpty()
        } catch (_: SecurityException) {
            ""
        }
    }

    fun copyText(context: Context, text: String) {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        manager.setPrimaryClip(ClipData.newPlainText("Ledge", text))
    }

    fun launchableApps(context: Context): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val flags = PackageManager.MATCH_DEFAULT_ONLY
        val resolved = if (android.os.Build.VERSION.SDK_INT >= 33) {
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(flags.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(intent, flags)
        }
        return resolved.mapNotNull { info ->
            val name = info.activityInfo?.packageName ?: return@mapNotNull null
            val label = info.loadLabel(context.packageManager)?.toString()?.trim().orEmpty()
            LaunchableApp(name, label.ifEmpty { name })
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }

    fun appIcon(context: Context, packageName: String): ImageBitmap? {
        return try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val size = (24 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
            drawable.toBitmap(width = size, height = size).asImageBitmap()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun launch(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    fun applyActivityWindow(window: Window, demo: DemoState, modules: List<DockModule>) {
        val params = window.attributes
        params.screenBrightness = brightnessFor(demo, modules)
        window.attributes = params
        if (DockModule.Caffeine in modules && demo.caffeine) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    fun applyOverlayChrome(
        params: WindowManager.LayoutParams,
        demo: DemoState,
        modules: List<DockModule>,
        parked: Boolean,
    ) {
        params.screenBrightness = if (parked) -1f else brightnessFor(demo, modules)
        val keepOn = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        params.flags = if (DockModule.Caffeine in modules && demo.caffeine) {
            params.flags or keepOn
        } else {
            params.flags and keepOn.inv()
        }
    }

    private fun brightnessFor(demo: DemoState, modules: List<DockModule>): Float {
        return if (DockModule.Brightness in modules) demo.brightness.coerceIn(0.2f, 1f) else -1f
    }

    private fun setTorch(context: Context, enabled: Boolean): Boolean {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return false
        return try {
            val id = manager.cameraIdList.firstOrNull { cameraId ->
                manager.getCameraCharacteristics(cameraId)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return false
            manager.setTorchMode(id, enabled)
            true
        } catch (_: CameraAccessException) {
            false
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }
}

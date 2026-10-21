package com.cedarworks.ledge.demo.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

data class PermissionSnapshot(
    val spec: DockPermissionSpec,
    val access: PermissionAccess,
    val declared: Boolean,
)

/**
 * Reads the real permission state for [capability]. A grant is recorded only
 * after Android reports it. Pressing a button does not mark anything granted.
 */
class PermissionCoordinator(
    context: Context,
    private val store: SetupStore,
    private val capability: DockCapability = DockPermissionPolicy.sample,
) {
    private val appContext = context.applicationContext

    fun snapshot(activity: Activity?): List<PermissionSnapshot> {
        return DockPermissionPolicy.required(capability).map { spec ->
            val declared = spec.platformPermission?.let(::isDeclared) ?: true
            val granted = declared && isGranted(spec)
            val rationale = activity != null &&
                spec.platformPermission != null &&
                ActivityCompat.shouldShowRequestPermissionRationale(activity, spec.platformPermission)
            PermissionSnapshot(
                spec = spec,
                access = DockPermissionPolicy.access(
                    granted = granted,
                    askedBefore = store.wasAsked(spec.id),
                    shouldShowRationale = rationale,
                ),
                declared = declared,
            )
        }
    }

    /**
     * Opens the system UI for [id], or invokes [launchRuntime] for a normal
     * runtime dialog. Does nothing when the permission is not in this build's
     * manifest. Never enables an accessibility service by itself.
     */
    fun request(id: DockPermissionId, activity: Activity, launchRuntime: (String) -> Unit) {
        val spec = DockPermissionPolicy.required(capability).firstOrNull { it.id == id } ?: return
        val permission = spec.platformPermission
        if (permission != null && !isDeclared(permission)) return
        store.markAsked(id)
        val current = snapshot(activity).firstOrNull { it.spec.id == id } ?: return
        when (spec.route) {
            DockPermissionRoute.Runtime -> {
                if (current.access == PermissionAccess.NeedsSettings) {
                    activity.startActivity(appDetails(activity))
                } else if (permission != null) {
                    launchRuntime(permission)
                }
            }
            DockPermissionRoute.OverlaySettings -> {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${activity.packageName}"),
                )
                activity.startActivity(intent)
            }
            DockPermissionRoute.AccessibilitySettings -> {
                activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            DockPermissionRoute.NotificationSettings -> {
                if (
                    Build.VERSION.SDK_INT >= 33 &&
                    permission != null &&
                    current.access != PermissionAccess.NeedsSettings
                ) {
                    launchRuntime(permission)
                } else {
                    activity.startActivity(notificationSettings(activity))
                }
            }
        }
    }

    private fun isGranted(spec: DockPermissionSpec): Boolean = when (spec.id) {
        DockPermissionId.Overlay -> Settings.canDrawOverlays(appContext)
        DockPermissionId.Accessibility -> accessibilityEnabled()
        DockPermissionId.Notifications -> notificationsGranted(spec.platformPermission)
        else -> {
            val permission = spec.platformPermission ?: return false
            ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun notificationsGranted(permission: String?): Boolean {
        if (!NotificationManagerCompat.from(appContext).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < 33 || permission == null) return true
        return ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun accessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            appContext.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        return enabled.split(':').any { it.startsWith(appContext.packageName) }
    }

    private fun isDeclared(permission: String): Boolean = declaredPermissions().contains(permission)

    private fun declaredPermissions(): Set<String> {
        val info = if (Build.VERSION.SDK_INT >= 33) {
            appContext.packageManager.getPackageInfo(
                appContext.packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            appContext.packageManager.getPackageInfo(
                appContext.packageName,
                PackageManager.GET_PERMISSIONS,
            )
        }
        return info.requestedPermissions?.toSet().orEmpty()
    }

    private fun appDetails(activity: Activity): Intent {
        return Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${activity.packageName}"),
        )
    }

    private fun notificationSettings(activity: Activity): Intent {
        return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
        }
    }
}

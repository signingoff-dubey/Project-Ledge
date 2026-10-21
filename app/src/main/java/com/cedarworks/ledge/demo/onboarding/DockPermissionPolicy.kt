package com.cedarworks.ledge.demo.onboarding

/**
 * Capabilities that need an Android permission. The sample dock outside this
 * app draws over other screens, keeps an accessibility service so that edge
 * handle stays available, and may post one ongoing notice. The service does
 * not read windows or keystrokes. The sample does not use the microphone,
 * place calls, or read contacts.
 */
data class DockCapability(
    val drawsOverOtherApps: Boolean = false,
    val holdsEdgeOutsideApp: Boolean = false,
    val usesMicrophone: Boolean = false,
    val placesCalls: Boolean = false,
    val readsContacts: Boolean = false,
    val usesNearbyDevices: Boolean = false,
    val postsNotifications: Boolean = false,
)

enum class DockPermissionId {
    Overlay,
    Accessibility,
    Microphone,
    Phone,
    Contacts,
    NearbyDevices,
    Notifications,
}

enum class DockPermissionRoute {
    Runtime,
    OverlaySettings,
    AccessibilitySettings,
    NotificationSettings,
}

enum class PermissionAccess {
    Granted,
    CanAsk,
    NeedsSettings,
}

data class DockPermissionSpec(
    val id: DockPermissionId,
    val route: DockPermissionRoute,
    val platformPermission: String?,
    val optional: Boolean,
)

object DockPermissionPolicy {
    /** Overlay and accessibility are required outside this app. The notice is optional. */
    val sample = DockCapability(
        drawsOverOtherApps = true,
        holdsEdgeOutsideApp = true,
        postsNotifications = true,
    )

    const val OVERLAY = "android.permission.SYSTEM_ALERT_WINDOW"
    const val RECORD_AUDIO = "android.permission.RECORD_AUDIO"
    const val CALL_PHONE = "android.permission.CALL_PHONE"
    const val READ_CONTACTS = "android.permission.READ_CONTACTS"
    const val BLUETOOTH_CONNECT = "android.permission.BLUETOOTH_CONNECT"
    const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"

    fun required(capability: DockCapability): List<DockPermissionSpec> = buildList {
        if (capability.drawsOverOtherApps) {
            add(DockPermissionSpec(DockPermissionId.Overlay, DockPermissionRoute.OverlaySettings, OVERLAY, optional = false))
        }
        if (capability.holdsEdgeOutsideApp) {
            add(
                DockPermissionSpec(
                    DockPermissionId.Accessibility,
                    DockPermissionRoute.AccessibilitySettings,
                    platformPermission = null,
                    optional = false,
                ),
            )
        }
        if (capability.usesMicrophone) {
            add(DockPermissionSpec(DockPermissionId.Microphone, DockPermissionRoute.Runtime, RECORD_AUDIO, optional = true))
        }
        if (capability.placesCalls) {
            add(DockPermissionSpec(DockPermissionId.Phone, DockPermissionRoute.Runtime, CALL_PHONE, optional = false))
        }
        if (capability.readsContacts) {
            add(DockPermissionSpec(DockPermissionId.Contacts, DockPermissionRoute.Runtime, READ_CONTACTS, optional = true))
        }
        if (capability.usesNearbyDevices) {
            add(
                DockPermissionSpec(
                    DockPermissionId.NearbyDevices,
                    DockPermissionRoute.Runtime,
                    BLUETOOTH_CONNECT,
                    optional = true,
                ),
            )
        }
        if (capability.postsNotifications) {
            add(
                DockPermissionSpec(
                    DockPermissionId.Notifications,
                    DockPermissionRoute.NotificationSettings,
                    POST_NOTIFICATIONS,
                    optional = true,
                ),
            )
        }
    }

    /**
     * [askedBefore] is remembered by the app. Android reports the same
     * "don't show a rationale" signal before the first request and after a
     * permanent denial, so the first request is still allowed.
     */
    fun access(granted: Boolean, askedBefore: Boolean, shouldShowRationale: Boolean): PermissionAccess {
        if (granted) return PermissionAccess.Granted
        if (!askedBefore || shouldShowRationale) return PermissionAccess.CanAsk
        return PermissionAccess.NeedsSettings
    }
}

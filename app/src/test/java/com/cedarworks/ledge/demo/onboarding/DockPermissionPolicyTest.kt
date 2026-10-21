package com.cedarworks.ledge.demo.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DockPermissionPolicyTest {
    @Test
    fun theSampleDockAsksForWhatTheOutsideEdgeNeeds() {
        val required = DockPermissionPolicy.required(DockPermissionPolicy.sample)
        assertEquals(
            listOf(DockPermissionId.Overlay, DockPermissionId.Accessibility, DockPermissionId.Notifications),
            required.map { it.id },
        )
        assertTrue(required.first { it.id == DockPermissionId.Overlay }.optional.not())
        assertTrue(required.first { it.id == DockPermissionId.Accessibility }.optional.not())
        assertTrue(required.first { it.id == DockPermissionId.Notifications }.optional)
    }

    @Test
    fun eachCapabilityAddsOnlyItsOwnPermission() {
        assertEquals(
            listOf(DockPermissionId.Overlay),
            DockPermissionPolicy.required(DockCapability(drawsOverOtherApps = true)).map { it.id },
        )
        assertEquals(
            listOf(DockPermissionId.Accessibility),
            DockPermissionPolicy.required(DockCapability(holdsEdgeOutsideApp = true)).map { it.id },
        )
        assertEquals(
            listOf(DockPermissionId.Microphone),
            DockPermissionPolicy.required(DockCapability(usesMicrophone = true)).map { it.id },
        )
        assertEquals(
            listOf(DockPermissionId.Phone),
            DockPermissionPolicy.required(DockCapability(placesCalls = true)).map { it.id },
        )
        assertEquals(
            listOf(DockPermissionId.Contacts),
            DockPermissionPolicy.required(DockCapability(readsContacts = true)).map { it.id },
        )
        assertEquals(
            listOf(DockPermissionId.NearbyDevices),
            DockPermissionPolicy.required(DockCapability(usesNearbyDevices = true)).map { it.id },
        )
        assertEquals(
            listOf(DockPermissionId.Notifications),
            DockPermissionPolicy.required(DockCapability(postsNotifications = true)).map { it.id },
        )
    }

    @Test
    fun aDenialIsNotAskedAgainUntilTheUserChoosesSettings() {
        assertEquals(
            PermissionAccess.CanAsk,
            DockPermissionPolicy.access(granted = false, askedBefore = false, shouldShowRationale = false),
        )
        assertEquals(
            PermissionAccess.CanAsk,
            DockPermissionPolicy.access(granted = false, askedBefore = true, shouldShowRationale = true),
        )
        assertEquals(
            PermissionAccess.NeedsSettings,
            DockPermissionPolicy.access(granted = false, askedBefore = true, shouldShowRationale = false),
        )
        assertEquals(
            PermissionAccess.Granted,
            DockPermissionPolicy.access(granted = true, askedBefore = true, shouldShowRationale = false),
        )
    }
}
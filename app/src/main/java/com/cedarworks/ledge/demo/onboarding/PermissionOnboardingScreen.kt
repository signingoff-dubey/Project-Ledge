package com.cedarworks.ledge.demo.onboarding

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cedarworks.ledge.CallLedge
import com.cedarworks.ledge.LedgeDefaults
import com.cedarworks.ledge.LedgeToggleItem
import com.cedarworks.ledge.LocalLedgeColors
import com.cedarworks.ledge.LocalLedgeMetrics
import com.cedarworks.ledge.QuickSettingsLedge
import com.cedarworks.ledge.demo.R
import androidx.compose.material3.Icon

private const val StepCount = 3

@Composable
fun PermissionOnboardingScreen(
    coordinator: PermissionCoordinator,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var snapshot by remember { mutableStateOf(coordinator.snapshot(activity)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        snapshot = coordinator.snapshot(activity)
    }
    DisposableEffect(lifecycleOwner, coordinator, activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                snapshot = coordinator.snapshot(activity)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    BackHandler(enabled = step > 0) { step -= 1 }

    val requiredStillMissing = snapshot.any { !it.spec.optional && it.access != PermissionAccess.Granted }
    Surface(
        modifier = modifier
            .fillMaxSize()
            .consumeTouches(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            StepMeter(step = step)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (step) {
                    0 -> WelcomeStep()
                    1 -> PermissionStep(
                        snapshot = snapshot,
                        onRequest = { item ->
                            val host = activity ?: return@PermissionStep
                            coordinator.request(item.spec.id, host, launcher::launch)
                        },
                    )
                    else -> DoneStep()
                }
            }
            Button(
                onClick = {
                    if (step < StepCount - 1) {
                        step += 1
                    } else {
                        onFinished()
                    }
                },
                enabled = step != 1 || !requiredStillMissing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (step == StepCount - 1) R.string.setup_start else R.string.setup_continue,
                    ),
                )
            }
            if (step < StepCount - 1) {
                TextButton(
                    onClick = onFinished,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally),
                ) {
                    Text(stringResource(R.string.setup_not_now))
                }
            }
        }
    }
}

@Composable
private fun StepMeter(step: Int) {
    val description = stringResource(R.string.setup_progress, step + 1, StepCount)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(StepCount) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (index <= step) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
            )
        }
    }
}

@Composable
private fun WelcomeStep() {
    Text(
        text = stringResource(R.string.setup_welcome_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Medium,
    )
    Text(
        text = stringResource(R.string.setup_welcome_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    DockPreview()
}

@Composable
private fun PermissionStep(
    snapshot: List<PermissionSnapshot>,
    onRequest: (PermissionSnapshot) -> Unit,
) {
    Text(
        text = stringResource(R.string.setup_permission_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Medium,
    )
    Text(
        text = stringResource(R.string.setup_permission_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (snapshot.isEmpty()) {
        UnusedPermissionList()
    } else {
        snapshot.forEach { item ->
            PermissionCard(item = item, onRequest = { onRequest(item) })
        }
    }
}

@Composable
private fun UnusedPermissionList() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        UnusedRow(stringResource(R.string.setup_overlay), stringResource(R.string.setup_overlay_reason))
        UnusedRow(stringResource(R.string.setup_accessibility), stringResource(R.string.setup_accessibility_reason))
        UnusedRow(stringResource(R.string.setup_microphone), stringResource(R.string.setup_microphone_reason))
        UnusedRow(stringResource(R.string.setup_phone), stringResource(R.string.setup_phone_reason))
        UnusedRow(stringResource(R.string.setup_notifications), stringResource(R.string.setup_notifications_reason))
    }
    Text(
        text = stringResource(R.string.setup_privacy),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun UnusedRow(title: String, reason: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        Text(
            text = reason,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PermissionCard(item: PermissionSnapshot, onRequest: () -> Unit) {
    val copy = permissionCopy(item.spec.id)
    val status = when {
        !item.declared -> stringResource(R.string.setup_not_in_build)
        item.access == PermissionAccess.Granted -> stringResource(R.string.setup_granted)
        item.access == PermissionAccess.NeedsSettings -> stringResource(R.string.setup_needs_settings)
        else -> stringResource(R.string.setup_needed)
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = copy.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = copy.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = copy.privacy,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = status, style = MaterialTheme.typography.labelLarge)
            if (item.declared && item.access != PermissionAccess.Granted) {
                val opensSettings = item.access == PermissionAccess.NeedsSettings ||
                    item.spec.route == DockPermissionRoute.OverlaySettings ||
                    item.spec.route == DockPermissionRoute.AccessibilitySettings
                Button(onClick = onRequest) {
                    Text(
                        stringResource(
                            if (opensSettings) R.string.setup_open_settings else R.string.setup_grant,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun DoneStep() {
    Text(
        text = stringResource(R.string.setup_done_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Medium,
    )
    Text(
        text = stringResource(R.string.setup_done_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DockPreview() {
    val previewDescription = stringResource(R.string.setup_preview)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = previewDescription },
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF141416),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
                .clipToBounds(),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(
                LocalLedgeColors provides LedgeDefaults.colors(),
                LocalLedgeMetrics provides LedgeDefaults.metricsFor(640.dp),
            ) {
                Row(
                    modifier = Modifier.graphicsLayer {
                        scaleX = 0.62f
                        scaleY = 0.62f
                        transformOrigin = TransformOrigin.Center
                    },
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CallLedge(
                        title = stringResource(R.string.call),
                        muted = false,
                        onMute = {},
                        onEnd = {},
                        muteDescription = stringResource(R.string.mute),
                        endDescription = stringResource(R.string.end_call),
                        muteIcon = { PreviewIcon(Icons.Rounded.Mic) },
                        endIcon = { PreviewIcon(Icons.Rounded.CallEnd) },
                    )
                    QuickSettingsLedge(
                        toggles = listOf(
                            previewToggle("wifi", stringResource(R.string.wifi), checked = true, Icons.Rounded.Wifi),
                            previewToggle(
                                "bluetooth",
                                stringResource(R.string.bluetooth),
                                checked = true,
                                Icons.Rounded.Bluetooth,
                            ),
                            previewToggle(
                                "airplane",
                                stringResource(R.string.airplane),
                                checked = false,
                                Icons.Rounded.AirplanemodeActive,
                            ),
                            previewToggle(
                                "flashlight",
                                stringResource(R.string.flashlight),
                                checked = false,
                                Icons.Rounded.FlashlightOn,
                            ),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun previewToggle(
    key: String,
    description: String,
    checked: Boolean,
    icon: ImageVector,
): LedgeToggleItem {
    return LedgeToggleItem(
        key = key,
        contentDescription = description,
        checked = checked,
        onCheckedChange = {},
        icon = { PreviewIcon(icon) },
    )
}

@Composable
private fun PreviewIcon(image: ImageVector) {
    Icon(
        imageVector = image,
        contentDescription = null,
        modifier = Modifier.size(LocalLedgeMetrics.current.iconSize),
    )
}

private data class PermissionCopy(val title: String, val reason: String, val privacy: String)

@Composable
private fun permissionCopy(id: DockPermissionId): PermissionCopy = when (id) {
    DockPermissionId.Overlay -> PermissionCopy(
        stringResource(R.string.setup_overlay),
        stringResource(R.string.setup_overlay_reason),
        stringResource(R.string.setup_privacy),
    )
    DockPermissionId.Accessibility -> PermissionCopy(
        stringResource(R.string.setup_accessibility),
        stringResource(R.string.setup_accessibility_reason),
        stringResource(R.string.setup_accessibility_privacy),
    )
    DockPermissionId.Microphone -> PermissionCopy(
        stringResource(R.string.setup_microphone),
        stringResource(R.string.setup_microphone_reason),
        stringResource(R.string.setup_privacy),
    )
    DockPermissionId.Phone -> PermissionCopy(
        stringResource(R.string.setup_phone),
        stringResource(R.string.setup_phone_reason),
        stringResource(R.string.setup_privacy),
    )
    DockPermissionId.Contacts -> PermissionCopy(
        stringResource(R.string.setup_contacts),
        stringResource(R.string.setup_contacts_reason),
        stringResource(R.string.setup_privacy),
    )
    DockPermissionId.NearbyDevices -> PermissionCopy(
        stringResource(R.string.setup_nearby),
        stringResource(R.string.setup_nearby_reason),
        stringResource(R.string.setup_privacy),
    )
    DockPermissionId.Notifications -> PermissionCopy(
        stringResource(R.string.setup_notifications),
        stringResource(R.string.setup_notifications_reason),
        stringResource(R.string.setup_privacy),
    )
}

private fun Modifier.consumeTouches(): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown()
        down.consume()
        do {
            val event = awaitPointerEvent()
            event.changes.forEach { it.consume() }
        } while (event.changes.any { it.pressed })
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

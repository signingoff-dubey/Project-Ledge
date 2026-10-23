package com.cedarworks.ledge.demo

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cedarworks.ledge.LedgeArrangement
import com.cedarworks.ledge.LedgeHost
import com.cedarworks.ledge.demo.onboarding.PermissionCoordinator
import com.cedarworks.ledge.demo.onboarding.PermissionOnboardingScreen
import com.cedarworks.ledge.demo.onboarding.SetupStore
import com.cedarworks.ledge.demo.overlay.DockOverlayService
import com.cedarworks.ledge.demo.overlay.DockSession
import com.cedarworks.ledge.rememberLedgeState
import com.cedarworks.ledge.ui.theme.LedgeTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var pocketedThisIntent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pocketedThisIntent = savedInstanceState?.getBoolean(KEY_POCKETED) == true
        enableEdgeToEdge()
        if (acceptShare(intent)) {
            Toast.makeText(this, getString(R.string.pocket_saved), Toast.LENGTH_SHORT).show()
        }
        setContent {
            LedgeTheme {
                LedgeRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pocketedThisIntent = false
        if (acceptShare(intent)) {
            Toast.makeText(this, getString(R.string.pocket_saved), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_POCKETED, pocketedThisIntent)
        super.onSaveInstanceState(outState)
    }

    private fun acceptShare(intent: Intent?): Boolean {
        if (pocketedThisIntent) return false
        val saved = DockSession.get(this).captureShare(intent, getString(R.string.pocket_image))
        if (saved) pocketedThisIntent = true
        return saved
    }

    private companion object {
        const val KEY_POCKETED = "pocketed_share"
    }
}

@Composable
private fun LedgeRoot() {
    val context = LocalContext.current
    val store = remember { SetupStore(context) }
    val coordinator = remember { PermissionCoordinator(context, store) }
    var finished by remember { mutableStateOf(store.isFinished()) }
    var explained by remember { mutableStateOf(store.explainedOutsideAccess()) }
    var reviewing by rememberSaveable { mutableStateOf(false) }
    OverlayBinding(active = finished && explained && !reviewing)
    Box(Modifier.fillMaxSize()) {
        LedgeApp(
            onReviewSetup = { reviewing = true },
        )
        if (!explained || reviewing) {
            PermissionOnboardingScreen(
                coordinator = coordinator,
                onFinished = {
                    store.finish()
                    store.markOutsideAccessExplained()
                    finished = true
                    explained = true
                    reviewing = false
                },
            )
        }
    }
}

@Composable
private fun OverlayBinding(active: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, active) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                syncOverlay(context, active)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        syncOverlay(context, active)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

private fun syncOverlay(context: android.content.Context, active: Boolean) {
    val allowed = active && Settings.canDrawOverlays(context)
    val intent = Intent(context, DockOverlayService::class.java)
    if (allowed) {
        ContextCompat.startForegroundService(context, intent)
    } else {
        context.stopService(intent)
    }
}

@Composable
fun LedgeApp(
    onReviewSetup: () -> Unit = {},
) {
    val context = LocalContext.current
    val session = remember { DockSession.get(context) }
    val ledgeState = rememberLedgeState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val onMessage: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    val overlayRunning = session.overlayRunning
    val needsCamera = stringResource(R.string.torch_needs_camera)
    val torchUnavailable = stringResource(R.string.torch_unavailable)
    val torchLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            session.demo.flashlight = false
            onMessage(needsCamera)
            return@rememberLauncherForActivityResult
        }
        when (DockDevice.applyTorch(context, true)) {
            TorchOutcome.On -> session.demo.flashlight = true
            else -> {
                session.demo.flashlight = false
                onMessage(torchUnavailable)
            }
        }
    }
    val onTorch: (Boolean) -> Unit = { enable ->
        when (DockDevice.applyTorch(context, enable)) {
            TorchOutcome.On -> session.demo.flashlight = true
            TorchOutcome.Off -> session.demo.flashlight = false
            TorchOutcome.NeedsPermission -> {
                session.demo.flashlight = false
                torchLauncher.launch(android.Manifest.permission.CAMERA)
            }
            TorchOutcome.Unavailable -> {
                session.demo.flashlight = false
                onMessage(torchUnavailable)
            }
        }
    }
    val view = LocalView.current
    LaunchedEffect(session.demo.caffeine, session.demo.brightness, session.modules) {
        val activity = view.context.findActivity()
        if (activity != null) {
            DockDevice.applyActivityWindow(activity.window, session.demo, session.modules)
        }
    }
    if (!overlayRunning) {
        LaunchedEffect(session.demo.inCall) {
            session.syncCall { ledgeState.open() }
        }
        var skipEdgeClose by remember { mutableStateOf(true) }
        LaunchedEffect(session.edge) {
            if (skipEdgeClose) {
                skipEdgeClose = false
                return@LaunchedEffect
            }
            ledgeState.close()
        }
    }
    val home = @Composable {
        HomeScreen(
            demo = session.demo,
            edge = session.edge,
            onEdgeChange = session::updateEdge,
            onOpenLedge = {
                if (session.overlayRunning) session.updateExpanded(true) else ledgeState.open()
            },
            modules = session.modules,
            onAddModule = session::addModule,
            onRemoveModule = session::removeModule,
            onChanged = session::persistExtras,
            onTorch = onTorch,
            anchorFraction = session.anchorFraction,
            onAnchorFractionChange = session::updateAnchor,
            onMessage = onMessage,
            onReviewSetup = onReviewSetup,
            snackbarHostState = snackbarHostState,
        )
    }
    if (overlayRunning) {
        home()
    } else {
        LedgeHost(
            state = ledgeState,
            edge = session.edge,
            arrangement = LedgeArrangement.Workflow,
            anchorFraction = session.anchorFraction,
            onAnchorFractionChange = session::updateAnchor,
            openDescription = stringResource(R.string.open_ledge),
            closeDescription = stringResource(R.string.close_ledge),
            workflowDescription = stringResource(R.string.dock_tray),
            panels = {
                samplePanels(
                    demo = session.demo,
                    onMessage = onMessage,
                    modules = session.modules,
                    onChanged = session::persistExtras,
                    onTorch = onTorch,
                )
            },
        ) {
            home()
        }
    }
}

@Preview(name = "Home", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun LedgePhonePreview() {
    LedgeTheme(dynamicColor = false) {
        LedgeApp()
    }
}

@Preview(name = "Home light", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun LedgePhoneLightPreview() {
    LedgeTheme(darkTheme = false, dynamicColor = false) {
        LedgeApp()
    }
}

@Preview(name = "Ledge open", widthDp = 980, heightDp = 560, showBackground = true)
@Composable
private fun LedgeOpenPreview() {
    LedgeTheme(darkTheme = true, dynamicColor = false) {
        val demo = remember { DemoState() }
        val ledgeState = rememberLedgeState(initiallyOpen = true)
        LedgeHost(
            state = ledgeState,
            arrangement = LedgeArrangement.Workflow,
            openDescription = "Open side actions",
            closeDescription = "Close side actions",
            panels = { samplePanels(demo, onMessage = {}) },
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF242428)),
            )
        }
    }
}

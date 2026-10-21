package com.cedarworks.ledge.demo

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LedgeTheme {
                LedgeRoot()
            }
        }
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
            selectedKey = session.selectedKey,
            onSelectedKeyChange = session::updateSelectedKey,
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
            selectedKey = session.selectedKey,
            anchorFraction = session.anchorFraction,
            onAnchorFractionChange = session::updateAnchor,
            openDescription = stringResource(R.string.open_ledge),
            closeDescription = stringResource(R.string.close_ledge),
            panels = { samplePanels(session.demo, onMessage) },
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
            openDescription = "Open side actions",
            closeDescription = "Close side actions",
            panels = { samplePanels(demo) {} },
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF242428)),
            )
        }
    }
}

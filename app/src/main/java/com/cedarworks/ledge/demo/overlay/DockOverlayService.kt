package com.cedarworks.ledge.demo.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.setViewTreeOnBackPressedDispatcherOwner
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.cedarworks.ledge.LedgeDefaults
import com.cedarworks.ledge.LedgeEdge
import com.cedarworks.ledge.LedgeHost
import com.cedarworks.ledge.LedgeSurface
import com.cedarworks.ledge.LedgeValue
import com.cedarworks.ledge.LiquidLedgeShape
import com.cedarworks.ledge.LocalLedgeColors
import com.cedarworks.ledge.LocalLedgeMetrics
import com.cedarworks.ledge.LocalLedgeShape
import com.cedarworks.ledge.demo.MainActivity
import com.cedarworks.ledge.demo.R
import com.cedarworks.ledge.demo.samplePanels
import com.cedarworks.ledge.rememberLedgeState
import com.cedarworks.ledge.ui.theme.LedgeTheme
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Draws the dock over other apps. The parked handle is only as wide as the
 * edge grab, so touches everywhere else reach the screen behind it. Opening
 * uses a full-screen window for the slide and the scrim, then removes it.
 */
class DockOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var lifecycleOwner: OverlayLifecycle
    private lateinit var session: DockSession
    private var handleView: View? = null
    private var handleParams: WindowManager.LayoutParams? = null
    private var panelView: View? = null
    private var syncing = false

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        try {
            startAsForeground()
        } catch (_: RuntimeException) {
            stopSelf()
            return
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        lifecycleOwner = OverlayLifecycle().also { it.start() }
        session = DockSession.get(this)
        session.setOverlayListener { syncWindows() }
        session.overlayRunning = true
        syncWindows()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (Settings.canDrawOverlays(this)) {
            try {
                ContextCompat.startForegroundService(this, Intent(this, DockOverlayService::class.java))
            } catch (_: RuntimeException) {
                // The process is already leaving. The next launch starts the dock again.
            }
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        if (::session.isInitialized) {
            session.setOverlayListener(null)
            session.overlayRunning = false
            session.updateExpanded(false)
        }
        removePanel()
        removeHandle()
        if (::lifecycleOwner.isInitialized) lifecycleOwner.destroy()
        super.onDestroy()
    }

    private fun syncWindows() {
        if (syncing || !::session.isInitialized) return
        syncing = true
        try {
            if (session.expanded) {
                removeHandle()
                ensurePanel()
            } else {
                removePanel()
                ensureHandle()
                layoutHandle()
            }
        } finally {
            syncing = false
        }
    }

    private fun ensureHandle() {
        if (handleView != null) return
        val view = dockView {
            DockHandle(session)
        }
        val params = edgeParams(width = dp(28), height = panelHeightPx())
        handleParams = params
        if (!addOverlay(view, params)) return
        handleView = view
    }

    private fun layoutHandle() {
        val view = handleView ?: return
        val params = handleParams ?: return
        val frame = usableFrame()
        val height = panelHeightPx()
        val travel = (frame.height - height).coerceAtLeast(1)
        session.travelPx = travel.toFloat()
        params.width = dp(28)
        params.height = height
        params.gravity = Gravity.TOP or if (session.edge == LedgeEdge.End) Gravity.END else Gravity.START
        params.x = 0
        params.y = frame.top + (travel * session.anchorFraction.coerceIn(0f, 1f)).toInt()
        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: IllegalArgumentException) {
            handleView = null
        }
    }

    private fun ensurePanel() {
        if (panelView != null) return
        val view = dockView {
            OverlayPanel(session) { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        val params = edgeParams(width = WindowManager.LayoutParams.MATCH_PARENT, height = WindowManager.LayoutParams.MATCH_PARENT)
        params.gravity = Gravity.TOP or Gravity.START
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        if (!addOverlay(view, params)) return
        panelView = view
        view.requestFocus()
    }

    private fun removeHandle() {
        handleView?.let { removeOverlay(it) }
        handleView = null
        handleParams = null
    }

    private fun removePanel() {
        panelView?.let { removeOverlay(it) }
        panelView = null
    }

    private fun addOverlay(view: View, params: WindowManager.LayoutParams): Boolean {
        return try {
            windowManager.addView(view, params)
            true
        } catch (_: WindowManager.BadTokenException) {
            stopSelf()
            false
        } catch (_: SecurityException) {
            stopSelf()
            false
        }
    }

    private fun removeOverlay(view: View) {
        try {
            windowManager.removeView(view)
        } catch (_: IllegalArgumentException) {
            // Already removed.
        }
    }

    private fun dockView(content: @Composable () -> Unit): View {
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeOnBackPressedDispatcherOwner(lifecycleOwner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                LedgeTheme {
                    content()
                }
            }
        }
        return BackFrame(this, lifecycleOwner.onBackPressedDispatcher).apply {
            addView(
                composeView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
        }
    }

    private fun edgeParams(width: Int, height: Int): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        return WindowManager.LayoutParams(
            width,
            height,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).also { params ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                params.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun panelHeightPx(): Int {
        val frame = usableFrame()
        val desired = dp(220)
        val floor = dp(176)
        val budget = frame.height - dp(16)
        return when {
            budget >= desired -> desired
            budget < floor -> floor
            else -> budget
        }
    }

    private fun usableFrame(): Frame {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsets(android.view.WindowInsets.Type.systemBars())
            val bounds = metrics.bounds
            return Frame(
                top = insets.top,
                height = (bounds.height() - insets.top - insets.bottom).coerceAtLeast(1),
            )
        }
        val size = android.graphics.Point()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealSize(size)
        val status = dimension("status_bar_height")
        val nav = dimension("navigation_bar_height")
        return Frame(top = status, height = (size.y - status - nav).coerceAtLeast(1))
    }

    private fun dimension(name: String): Int {
        val id = resources.getIdentifier(name, "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else 0
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt().coerceAtLeast(1)

    private fun startAsForeground() {
        val channelId = "ledge_dock"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.overlay_channel),
                NotificationManager.IMPORTANCE_LOW,
            )
            channel.setShowBadge(false)
            channel.setSound(null, null)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_ledge)
            .setContentTitle(getString(R.string.overlay_notice_title))
            .setContentText(getString(R.string.overlay_notice_body))
            .setOngoing(true)
            .setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private data class Frame(val top: Int, val height: Int)

    private class OverlayLifecycle : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner, OnBackPressedDispatcherOwner {
        private val registry = LifecycleRegistry(this)
        private val store = ViewModelStore()
        private val savedState = SavedStateRegistryController.create(this)
        override val onBackPressedDispatcher = OnBackPressedDispatcher()

        override val lifecycle: Lifecycle
            get() = registry

        override val viewModelStore: ViewModelStore
            get() = store

        override val savedStateRegistry: SavedStateRegistry
            get() = savedState.savedStateRegistry

        fun start() {
            savedState.performAttach()
            savedState.performRestore(null)
            registry.currentState = Lifecycle.State.RESUMED
        }

        fun destroy() {
            registry.currentState = Lifecycle.State.DESTROYED
            store.clear()
        }
    }

    private companion object {
        const val NOTIFICATION_ID = 7
    }
}

private class BackFrame(
    context: android.content.Context,
    private val backDispatcher: OnBackPressedDispatcher,
) : FrameLayout(context) {
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (
            event.keyCode == KeyEvent.KEYCODE_BACK &&
            event.action == KeyEvent.ACTION_UP &&
            backDispatcher.hasEnabledCallbacks()
        ) {
            backDispatcher.onBackPressed()
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}

@Composable
private fun DockHandle(session: DockSession) {
    val description = stringResource(R.string.open_ledge)
    CallHandoff(session) { session.updateExpanded(true) }
    val onEnd = session.edge == LedgeEdge.End
    CompositionLocalProvider(
        LocalLedgeColors provides LedgeDefaults.colors(),
        LocalLedgeMetrics provides LedgeDefaults.metricsFor(640.dp),
        LocalLedgeShape provides LiquidLedgeShape(emergeFromEnd = onEnd),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = description }
                .pointerInput(onEnd) {
                    val slop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        var totalX = 0f
                        var totalY = 0f
                        var axis = 0
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                val inward = if (onEnd) totalX < 0f else totalX > 0f
                                if (axis == 0 || (axis == 1 && inward)) {
                                    session.updateExpanded(true)
                                }
                                break
                            }
                            val dx = change.position.x - change.previousPosition.x
                            val dy = change.position.y - change.previousPosition.y
                            totalX += dx
                            totalY += dy
                            if (axis == 0 && hypot(totalX, totalY) > slop) {
                                axis = if (abs(totalX) > abs(totalY)) 1 else 2
                            }
                            if (axis == 2) {
                                change.consume()
                                val travel = session.travelPx.coerceAtLeast(1f)
                                session.updateAnchor(session.anchorFraction + dy / travel)
                            } else if (axis == 1) {
                                change.consume()
                            }
                        }
                    }
                },
            contentAlignment = if (onEnd) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            LedgeSurface { }
        }
    }
}

@Composable
private fun OverlayPanel(session: DockSession, onMessage: (String) -> Unit) {
    val ledgeState = rememberLedgeState()
    var seenOpen by remember { mutableStateOf(false) }
    CallHandoff(session) { session.updateExpanded(true) }
    LaunchedEffect(Unit) { ledgeState.open() }
    LaunchedEffect(ledgeState.progress) {
        if (ledgeState.progress > 0.9f) seenOpen = true
        if (seenOpen && ledgeState.settled == LedgeValue.Closed && ledgeState.progress <= 0.001f) {
            session.updateExpanded(false)
        }
    }
    var skipEdgeClose by remember { mutableStateOf(true) }
    LaunchedEffect(session.edge) {
        if (skipEdgeClose) {
            skipEdgeClose = false
            return@LaunchedEffect
        }
        ledgeState.close()
    }
    LedgeHost(
        state = ledgeState,
        edge = session.edge,
        selectedKey = session.selectedKey,
        anchorFraction = session.anchorFraction,
        onAnchorFractionChange = session::updateAnchor,
        openDescription = stringResource(R.string.open_ledge),
        closeDescription = stringResource(R.string.close_ledge),
        panels = { samplePanels(session.demo, onMessage) },
    ) {}
}

@Composable
private fun CallHandoff(session: DockSession, onOpen: () -> Unit) {
    LaunchedEffect(session.demo.inCall) {
        session.syncCall(onOpen)
    }
}

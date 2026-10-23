package com.cedarworks.ledge.demo

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.StickyNote2
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.cedarworks.ledge.LedgeControl
import com.cedarworks.ledge.LedgeControlShape
import com.cedarworks.ledge.LedgeDefaults
import com.cedarworks.ledge.LedgeSurface
import com.cedarworks.ledge.LedgeToggle
import com.cedarworks.ledge.LocalLedgeColors
import com.cedarworks.ledge.LocalLedgeMetrics
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private val dockSwatches = listOf(
    Color(0xFFE86A60),
    Color(0xFFC9D7F8),
    Color(0xFF5FD6A0),
    Color(0xFFE7C36A),
    Color(0xFFD7B4F3),
)

@Composable
internal fun DockModulePanel(
    module: DockModule,
    demo: DemoState,
    onMessage: (String) -> Unit,
    onChanged: () -> Unit,
    onTorch: (Boolean) -> Unit,
) {
    when (module) {
        DockModule.Clipboard -> ClipboardColumn()
        DockModule.Compass -> CompassColumn()
        DockModule.Counter -> CounterColumn(demo, onChanged)
        DockModule.Torch -> TorchColumn(demo, onTorch)
        DockModule.AppShortcut -> ShortcutColumn(demo, onMessage)
        DockModule.Bluetooth -> BluetoothColumn(demo)
        DockModule.Pocket -> PocketColumn(demo)
        DockModule.LocalModel -> LocalModelColumn(demo, onMessage, onChanged)
        DockModule.Progress -> ProgressColumn(demo, onChanged)
        DockModule.Motes -> MotesColumn(demo)
        DockModule.Scroll -> ScrollColumn()
        DockModule.CircleCut -> CircleCutColumn(demo, onChanged)
        DockModule.AudioFlow -> AudioColumn(demo, onMessage, onChanged)
        DockModule.ColorPicker -> ColorColumn(demo, onChanged)
        DockModule.Caffeine -> CaffeineColumn(demo, onMessage, onChanged)
        DockModule.Media -> SampleMediaPanel(demo)
        DockModule.Brightness -> BrightnessColumn(demo, onChanged)
        DockModule.Timer -> TimerColumn(demo, onChanged)
        DockModule.AppLibrary -> LibraryColumn(onMessage)
    }
}

@Composable
internal fun EmptyDockColumn(onMessage: (String) -> Unit) {
    val hint = stringResource(R.string.dock_empty)
    DockColumn {
        DockLabel(hint)
        LedgeControl(
            onClick = { onMessage(hint) },
            contentDescription = hint,
            shape = LedgeControlShape.Circle,
        ) {
            PanelIcon(Icons.Rounded.Add)
        }
    }
}

@Composable
private fun ClipboardColumn() {
    val context = LocalContext.current
    val empty = stringResource(R.string.clipboard_empty)
    val copied = stringResource(R.string.copied)
    var clip by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { clip = DockDevice.clipboardText(context) }
    DockColumn {
        DockLabel(stringResource(R.string.module_clipboard))
        DockLabel(clip.ifBlank { empty }.replace('\n', ' '))
        LedgeControl(
            onClick = {
                val current = DockDevice.clipboardText(context)
                if (current.isBlank()) {
                    DockDevice.copyText(context, "Ledge")
                    clip = DockDevice.clipboardText(context)
                } else {
                    clip = current
                }
            },
            contentDescription = if (clip.isBlank()) copied else stringResource(R.string.module_clipboard),
        ) {
            PanelIcon(Icons.Rounded.ContentPaste)
        }
    }
}

@Composable
private fun CompassColumn() {
    val context = LocalContext.current
    var degrees by remember { mutableFloatStateOf(Float.NaN) }
    DisposableEffect(context) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.values.size < 3) return
                try {
                    val rotation = FloatArray(9)
                    val orientation = FloatArray(3)
                    SensorManager.getRotationMatrixFromVector(rotation, event.values)
                    SensorManager.getOrientation(rotation, orientation)
                    val heading = Math.toDegrees(orientation[0].toDouble())
                    degrees = ((heading + 360.0) % 360.0).toFloat()
                } catch (_: IllegalArgumentException) {
                    degrees = Float.NaN
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) {
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { manager.unregisterListener(listener) }
    }
    val label = if (degrees.isNaN()) {
        stringResource(R.string.compass_unavailable)
    } else {
        "${degrees.roundToInt()}° ${cardinal(degrees)}"
    }
    DockColumn {
        DockLabel(stringResource(R.string.module_compass))
        DockLabel(label)
        LedgeControl(
            onClick = {},
            contentDescription = label,
            enabled = false,
            shape = LedgeControlShape.Circle,
        ) {
            PanelIcon(Icons.Rounded.Explore)
        }
    }
}

@Composable
private fun CounterColumn(demo: DemoState, onChanged: () -> Unit) {
    DockColumn {
        DockLabel(demo.counter.toString())
        LedgeControl(
            onClick = {
                demo.counter += 1
                onChanged()
            },
            contentDescription = stringResource(R.string.counter_add),
            shape = LedgeControlShape.Circle,
        ) {
            PanelIcon(Icons.Rounded.Add)
        }
        LedgeControl(
            onClick = {
                demo.counter = (demo.counter - 1).coerceAtLeast(0)
                onChanged()
            },
            contentDescription = stringResource(R.string.counter_remove),
            shape = LedgeControlShape.Circle,
        ) {
            PanelIcon(Icons.Rounded.Remove)
        }
    }
}

@Composable
private fun TorchColumn(demo: DemoState, onTorch: (Boolean) -> Unit) {
    DockColumn {
        DockLabel(stringResource(R.string.module_torch))
        LedgeToggle(
            checked = demo.flashlight,
            onCheckedChange = onTorch,
            contentDescription = stringResource(R.string.flashlight),
            shape = LedgeControlShape.TallOval,
        ) {
            PanelIcon(if (demo.flashlight) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff)
        }
    }
}

@Composable
private fun ShortcutColumn(demo: DemoState, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val missing = stringResource(R.string.shortcut_pick)
    val failed = stringResource(R.string.shortcut_failed)
    val label = remember(demo.shortcutPackage) {
        if (demo.shortcutPackage.isBlank()) {
            ""
        } else {
            DockDevice.launchableApps(context)
                .firstOrNull { it.packageName == demo.shortcutPackage }
                ?.label
                .orEmpty()
                .ifEmpty { demo.shortcutPackage }
        }
    }
    DockColumn {
        DockLabel(label.ifBlank { stringResource(R.string.module_shortcut) })
        LedgeControl(
            onClick = {
                if (demo.shortcutPackage.isBlank()) {
                    onMessage(missing)
                } else if (!DockDevice.launch(context, demo.shortcutPackage)) {
                    onMessage(failed)
                }
            },
            contentDescription = label.ifBlank { missing },
            shape = LedgeControlShape.TallOval,
        ) {
            if (demo.shortcutPackage.isBlank()) {
                PanelIcon(Icons.Rounded.Apps)
            } else {
                AppGlyph(demo.shortcutPackage)
            }
        }
    }
}

@Composable
private fun BluetoothColumn(demo: DemoState) {
    DockColumn {
        DockLabel(stringResource(R.string.bluetooth))
        LedgeToggle(
            checked = demo.bluetooth,
            onCheckedChange = demo::updateBluetooth,
            contentDescription = stringResource(R.string.bluetooth),
            shape = LedgeControlShape.TallOval,
        ) {
            PanelIcon(Icons.Rounded.Bluetooth)
        }
    }
}

@Composable
private fun PocketColumn(demo: DemoState) {
    var index by remember { mutableIntStateOf(0) }
    val line = demo.pocket.getOrNull(index.coerceIn(0, (demo.pocket.size - 1).coerceAtLeast(0)))
    DockColumn {
        DockLabel(line ?: stringResource(R.string.pocket_empty))
        LedgeControl(
            onClick = {
                if (demo.pocket.isNotEmpty()) index = (index + 1) % demo.pocket.size
            },
            contentDescription = stringResource(R.string.module_pocket),
            shape = LedgeControlShape.Circle,
        ) {
            PanelIcon(Icons.Rounded.Inbox)
        }
    }
}

@Composable
private fun LocalModelColumn(
    demo: DemoState,
    onMessage: (String) -> Unit,
    onChanged: () -> Unit,
) {
    val empty = stringResource(R.string.local_model_empty)
    DockColumn {
        DockLabel(demo.localAnswer.ifBlank { stringResource(R.string.module_local_model) })
        LedgeControl(
            onClick = {
                val draft = localDraft(demo.localPrompt)
                if (draft.isEmpty()) {
                    onMessage(empty)
                } else {
                    demo.localAnswer = draft
                    onChanged()
                }
            },
            contentDescription = stringResource(R.string.local_model_run),
            shape = LedgeControlShape.Circle,
        ) {
            PanelIcon(Icons.Rounded.AutoAwesome)
        }
    }
}

@Composable
private fun ProgressColumn(demo: DemoState, onChanged: () -> Unit) {
    DockColumn {
        DockLabel("${demo.progressMark}")
        LedgeControl(
            onClick = {
                demo.progressMark = if (demo.progressMark >= 100) 0 else demo.progressMark + 10
                onChanged()
            },
            contentDescription = stringResource(R.string.module_progress),
        ) {
            PanelIcon(Icons.Rounded.Timer)
        }
    }
}

@Composable
private fun MotesColumn(demo: DemoState) {
    var index by remember { mutableIntStateOf(0) }
    val line = demo.motes.getOrNull(index.coerceIn(0, (demo.motes.size - 1).coerceAtLeast(0)))
    DockColumn {
        DockLabel(line ?: stringResource(R.string.mote_empty))
        LedgeControl(
            onClick = {
                if (demo.motes.isNotEmpty()) index = (index + 1) % demo.motes.size
            },
            contentDescription = stringResource(R.string.module_motes),
            shape = LedgeControlShape.Circle,
        ) {
            PanelIcon(Icons.AutoMirrored.Rounded.StickyNote2)
        }
    }
}

@Composable
private fun ScrollColumn() {
    DockColumn(arrangement = Arrangement.Top) {
        DockLabel(stringResource(R.string.module_scroll))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LocalLedgeMetrics.current.gap),
        ) {
            for (line in 1..24) {
                DockLabel(stringResource(R.string.scroll_line, line))
            }
        }
    }
}

@Composable
private fun CircleCutColumn(demo: DemoState, onChanged: () -> Unit) {
    DockColumn {
        DockLabel(demo.circleCuts.toString())
        LedgeControl(
            onClick = {
                demo.circleCuts += 1
                onChanged()
            },
            contentDescription = stringResource(R.string.circle_cut_action),
            shape = LedgeControlShape.Circle,
        ) {
            PanelIcon(Icons.Rounded.ContentCut)
        }
    }
}

@Composable
private fun AudioColumn(
    demo: DemoState,
    onMessage: (String) -> Unit,
    onChanged: () -> Unit,
) {
    val room = stringResource(R.string.audio_room)
    val ear = stringResource(R.string.audio_ear)
    val quiet = stringResource(R.string.audio_quiet)
    val route = when (demo.audioRoute) {
        0 -> room
        1 -> ear
        else -> quiet
    }
    DockColumn {
        DockLabel(route)
        LedgeControl(
            onClick = {
                demo.audioRoute = (demo.audioRoute + 1) % 3
                if (demo.audioRoute == 2) demo.playing = false
                onChanged()
                onMessage(
                    when (demo.audioRoute) {
                        0 -> room
                        1 -> ear
                        else -> quiet
                    },
                )
            },
            contentDescription = stringResource(R.string.module_audio_flow),
            shape = LedgeControlShape.TallOval,
        ) {
            PanelIcon(Icons.Rounded.GraphicEq)
        }
    }
}

@Composable
private fun ColorColumn(demo: DemoState, onChanged: () -> Unit) {
    val swatch = dockSwatches[demo.colorIndex.coerceIn(0, dockSwatches.lastIndex)]
    val colors = LocalLedgeColors.current
    DockColumn {
        DockLabel(stringResource(R.string.module_color))
        LedgeControl(
            onClick = {
                demo.colorIndex = (demo.colorIndex + 1) % dockSwatches.size
                onChanged()
            },
            contentDescription = stringResource(R.string.module_color),
            shape = LedgeControlShape.Circle,
            containerColor = swatch,
            contentColor = colors.onControlActive,
        ) {
            PanelIcon(Icons.Rounded.Palette)
        }
    }
}

@Composable
private fun CaffeineColumn(
    demo: DemoState,
    onMessage: (String) -> Unit,
    onChanged: () -> Unit,
) {
    val on = stringResource(R.string.caffeine_on)
    val off = stringResource(R.string.caffeine_off)
    DockColumn {
        DockLabel(stringResource(R.string.module_caffeine))
        LedgeToggle(
            checked = demo.caffeine,
            onCheckedChange = { enabled ->
                demo.caffeine = enabled
                onChanged()
                onMessage(if (enabled) on else off)
            },
            contentDescription = stringResource(R.string.module_caffeine),
            shape = LedgeControlShape.TallOval,
        ) {
            PanelIcon(Icons.Rounded.Coffee)
        }
    }
}

@Composable
private fun BrightnessColumn(demo: DemoState, onChanged: () -> Unit) {
    val percent = (demo.brightness.coerceIn(0.2f, 1f) * 100).roundToInt()
    DockColumn {
        DockLabel("$percent%")
        LedgeControl(
            onClick = {
                demo.brightness = if (demo.brightness >= 0.99f) 0.25f else (demo.brightness + 0.15f).coerceAtMost(1f)
                onChanged()
            },
            contentDescription = stringResource(R.string.module_brightness),
            shape = LedgeControlShape.Circle,
        ) {
            PanelIcon(Icons.Rounded.BrightnessMedium)
        }
    }
}

@Composable
private fun TimerColumn(demo: DemoState, onChanged: () -> Unit) {
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(demo.timerEndsAt) {
        while (demo.timerEndsAt - SystemClock.elapsedRealtime() > 0L) {
            delay(250)
            tick++
        }
    }
    val left = (demo.timerEndsAt - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
    val label = remember(left, tick) { clockLabel(left) }
    DockColumn {
        DockLabel(label)
        LedgeControl(
            onClick = {
                demo.extendTimer(SystemClock.elapsedRealtime(), 30_000L)
                onChanged()
            },
            contentDescription = stringResource(R.string.timer_add_30),
        ) {
            DockLabel(stringResource(R.string.timer_30))
        }
        LedgeControl(
            onClick = {
                demo.extendTimer(SystemClock.elapsedRealtime(), 60_000L)
                onChanged()
            },
            contentDescription = stringResource(R.string.timer_add_minute),
        ) {
            DockLabel(stringResource(R.string.timer_1m))
        }
    }
}

@Composable
private fun LibraryColumn(onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val apps = remember { DockDevice.launchableApps(context) }
    val failed = stringResource(R.string.shortcut_failed)
    DockColumn(arrangement = Arrangement.Top) {
        DockLabel(stringResource(R.string.module_library))
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(apps, key = { it.packageName }) { app ->
                LedgeControl(
                    onClick = {
                        if (!DockDevice.launch(context, app.packageName)) onMessage(failed)
                    },
                    contentDescription = app.label,
                    shape = LedgeControlShape.Circle,
                ) {
                    AppGlyph(app.packageName)
                }
            }
        }
    }
}

@Composable
private fun AppGlyph(packageName: String) {
    val context = LocalContext.current
    val bitmap = remember(packageName) { DockDevice.appIcon(context, packageName) }
    val metrics = LocalLedgeMetrics.current
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier
                .size(metrics.iconSize)
                .clip(CircleShape),
        )
    } else {
        PanelIcon(Icons.Rounded.Apps)
    }
}

@Composable
private fun DockColumn(
    arrangement: Arrangement.Vertical = Arrangement.SpaceEvenly,
    content: @Composable ColumnScope.() -> Unit,
) {
    val metrics = LocalLedgeMetrics.current
    LedgeSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = metrics.verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = arrangement,
            content = content,
        )
    }
}

@Composable
private fun DockLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.widthIn(max = LocalLedgeMetrics.current.titleWidth),
        color = LocalLedgeColors.current.label,
        style = LedgeDefaults.labelStyle,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun cardinal(degrees: Float): String {
    val names = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val index = (((degrees % 360f) + 360f) % 360f / 45f).roundToInt() % 8
    return names[index]
}

private fun clockLabel(ms: Long): String {
    val total = (ms / 1000L).coerceAtLeast(0L)
    val minutes = total / 60L
    val seconds = total % 60L
    return "%d:%02d".format(minutes, seconds)
}

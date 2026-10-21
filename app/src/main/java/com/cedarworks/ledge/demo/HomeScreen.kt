package com.cedarworks.ledge.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuOpen
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.cedarworks.ledge.LedgeEdge
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    demo: DemoState,
    edge: LedgeEdge,
    onEdgeChange: (LedgeEdge) -> Unit,
    onOpenLedge: () -> Unit,
    selectedKey: String,
    onSelectedKeyChange: (String) -> Unit,
    anchorFraction: Float,
    onAnchorFractionChange: (Float) -> Unit,
    onMessage: (String) -> Unit,
    onReviewSetup: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val name = stringResource(R.string.contact_name)
    val calling = stringResource(R.string.calling, name)
    val ended = stringResource(R.string.call_ended)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                actions = {
                    IconButton(onClick = onOpenLedge) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.MenuOpen,
                            contentDescription = stringResource(R.string.open_ledge),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Clock()
            Text(
                text = swipeHint(edge),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = edge == LedgeEdge.End,
                    onClick = { onEdgeChange(LedgeEdge.End) },
                    label = { Text(stringResource(R.string.edge_trailing)) },
                )
                FilterChip(
                    selected = edge == LedgeEdge.Start,
                    onClick = { onEdgeChange(LedgeEdge.Start) },
                    label = { Text(stringResource(R.string.edge_leading)) },
                )
            }
            SectionTitle(stringResource(R.string.which_actions))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (demo.inCall) {
                    ActionChip("call", stringResource(R.string.call), selectedKey, onSelectedKeyChange)
                    ActionChip("contact", stringResource(R.string.contact_name), selectedKey, onSelectedKeyChange)
                }
                ActionChip("shortcuts", stringResource(R.string.shortcuts), selectedKey, onSelectedKeyChange)
                ActionChip("media", stringResource(R.string.track_title), selectedKey, onSelectedKeyChange)
                ActionChip("assist", stringResource(R.string.assist), selectedKey, onSelectedKeyChange)
                ActionChip("toggles", stringResource(R.string.quick_settings), selectedKey, onSelectedKeyChange)
            }
            SectionTitle(stringResource(R.string.place_on_edge))
            Slider(
                value = anchorFraction.coerceIn(0f, 1f),
                onValueChange = onAnchorFractionChange,
            )
            Button(onClick = onOpenLedge) {
                Text(stringResource(R.string.show_actions))
            }
            SectionTitle(stringResource(R.string.setup))
            Card(colors = containerCardColors()) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.setup_status),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = onReviewSetup) {
                        Text(stringResource(R.string.review_setup))
                    }
                }
            }
            if (demo.inCall) {
                CallCard(
                    name = name,
                    muted = demo.muted,
                    onMute = { demo.muted = !demo.muted },
                    onEnd = {
                        demo.inCall = false
                        demo.muted = false
                        onMessage(ended)
                    },
                )
            }
            if (demo.focus) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.focus_on),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            if (demo.listening) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.listening),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            SectionTitle(stringResource(R.string.quick_settings))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SettingChip(stringResource(R.string.wifi), demo.wifi, Icons.Rounded.Wifi) {
                    demo.updateWifi(!demo.wifi)
                }
                SettingChip(stringResource(R.string.bluetooth), demo.bluetooth, Icons.Rounded.Bluetooth) {
                    demo.updateBluetooth(!demo.bluetooth)
                }
                SettingChip(stringResource(R.string.airplane), demo.airplane, Icons.Rounded.AirplanemodeActive) {
                    demo.updateAirplane(!demo.airplane)
                }
                SettingChip(stringResource(R.string.flashlight), demo.flashlight, Icons.Rounded.FlashlightOn) {
                    demo.flashlight = !demo.flashlight
                }
            }
            SectionTitle(stringResource(R.string.now_playing))
            Card(colors = containerCardColors()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.track_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.track_artist),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { demo.playing = !demo.playing }) {
                        Icon(
                            imageVector = if (demo.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(
                                if (demo.playing) R.string.pause else R.string.play,
                            ),
                        )
                    }
                }
            }
            SectionTitle(stringResource(R.string.reach))
            Card(colors = containerCardColors()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.call_contact, name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = {
                            demo.inCall = true
                            demo.muted = false
                            onMessage(calling)
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Icon(Icons.Rounded.Call, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun Clock() {
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            tick++
        }
    }
    val now = remember(tick) { Date() }
    val time = remember(now) { DateFormat.getTimeInstance(DateFormat.SHORT).format(now) }
    val date = remember(now) { DateFormat.getDateInstance(DateFormat.FULL).format(now) }
    Column {
        Text(
            text = date,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = time,
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.semantics { heading() },
        )
    }
}

@Composable
private fun ActionChip(
    key: String,
    label: String,
    selectedKey: String,
    onSelected: (String) -> Unit,
) {
    FilterChip(
        selected = selectedKey == key,
        onClick = { onSelected(key) },
        label = { Text(label) },
    )
}

@Composable
private fun swipeHint(edge: LedgeEdge): String {
    val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val physicalRight = if (ltr) edge == LedgeEdge.End else edge == LedgeEdge.Start
    val side = stringResource(if (physicalRight) R.string.edge_right else R.string.edge_left)
    return stringResource(R.string.hint_swipe, side)
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun containerCardColors() = CardDefaults.cardColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
)

@Composable
private fun SettingChip(
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(icon, contentDescription = null)
        },
    )
}

@Composable
private fun CallCard(
    name: String,
    muted: Boolean,
    onMute: () -> Unit,
    onEnd: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.in_call_with, name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (muted) {
                    Text(
                        text = stringResource(R.string.mute),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            IconButton(onClick = onMute) {
                Icon(
                    imageVector = if (muted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                    contentDescription = stringResource(if (muted) R.string.unmute else R.string.mute),
                )
            }
            IconButton(onClick = onEnd) {
                Icon(
                    imageVector = Icons.Rounded.CallEnd,
                    contentDescription = stringResource(R.string.end_call),
                )
            }
        }
    }
}

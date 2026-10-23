package com.cedarworks.ledge.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DockEditors(
    demo: DemoState,
    modules: List<DockModule>,
    onChanged: () -> Unit,
    onAddMote: (String) -> Unit,
    onClearPocket: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (DockModule.Brightness in modules) {
            Text(stringResource(R.string.module_brightness))
            Slider(
                value = demo.brightness.coerceIn(0.2f, 1f),
                onValueChange = { value ->
                    demo.brightness = value
                    onChanged()
                },
                valueRange = 0.2f..1f,
            )
        }
        if (DockModule.LocalModel in modules) {
            Text(stringResource(R.string.local_model_note))
            OutlinedTextField(
                value = demo.localPrompt,
                onValueChange = { value ->
                    demo.localPrompt = value.take(240)
                    onChanged()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.local_model_hint)) },
                singleLine = true,
            )
        }
        if (DockModule.Motes in modules) {
            var draft by remember { mutableStateOf("") }
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.take(80) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.mote_hint)) },
                singleLine = true,
            )
            TextButton(
                onClick = {
                    onAddMote(draft)
                    draft = ""
                },
                enabled = draft.isNotBlank(),
            ) {
                Text(stringResource(R.string.mote_add))
            }
        }
        if (DockModule.Pocket in modules) {
            if (demo.pocket.isEmpty()) {
                Text(stringResource(R.string.pocket_empty))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    demo.pocket.forEach { line ->
                        Text(line)
                    }
                }
                TextButton(onClick = onClearPocket) {
                    Text(stringResource(R.string.pocket_clear))
                }
            }
        }
        if (DockModule.AppShortcut in modules) {
            ShortcutPicker(demo, onChanged)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShortcutPicker(demo: DemoState, onChanged: () -> Unit) {
    val context = LocalContext.current
    val apps = remember { DockDevice.launchableApps(context).take(18) }
    Text(stringResource(R.string.shortcut_pick))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        apps.forEach { app ->
            FilterChip(
                selected = demo.shortcutPackage == app.packageName,
                onClick = {
                    demo.shortcutPackage = app.packageName
                    onChanged()
                },
                label = { Text(app.label) },
            )
        }
    }
}

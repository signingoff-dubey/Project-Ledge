# Ledge

Ledge is an Android side-swipe shelf. A droplet rests on the screen edge. Drag it up or down to place it, then swipe inward and it slides out from that spot. The shelf is a Jetpack Compose library. The `app` module is a Material 3 sample.

The panel art, names, and icons in the sample are original. Avatar photos and third-party product names are left to the app that embeds the library.

## Modules

| Module | Role |
| --- | --- |
| `:ledge` | Reusable Compose library. Gesture, shape, controls, and panel recipes. |
| `:app` | Sample application. Material 3 home screen and the six reference panels. |

Minimum SDK 24. Compile SDK 37. The sample targets SDK 36. UI is Jetpack Compose with Material 3.

## Build

Open the project in Android Studio, or from the repository root:

```bash
./gradlew :app:assembleDebug :ledge:testDebugUnitTest
```

On Windows:

```bat
gradlew.bat :app:assembleDebug :ledge:testDebugUnitTest
```

## Use the library

Include the module:

```kotlin
// settings.gradle.kts
include(":ledge")

// app/build.gradle.kts
dependencies {
    implementation(project(":ledge"))
}
```

Wrap your screen and declare panels. Pass translated strings for anything a screen reader speaks.

```kotlin
val ledgeState = rememberLedgeState()
LedgeHost(
    state = ledgeState,
    edge = LedgeEdge.End,
    openDescription = "Open side actions",
    closeDescription = "Close side actions",
    panels = {
        panel(key = "call", contentDescription = "Call") {
            CallLedge(
                title = "Call",
                muted = muted,
                onMute = { muted = !muted },
                onEnd = onHangUp,
                muteDescription = if (muted) "Unmute" else "Mute",
                endDescription = "End call",
                muteIcon = { Icon(Icons.Rounded.MicOff, contentDescription = null) },
                endIcon = { Icon(Icons.Rounded.CallEnd, contentDescription = null) },
            )
        }
    },
) {
    YourScreen(onOpenLedge = { ledgeState.open() })
}
```

Recipes in `com.cedarworks.ledge`:

- `CallLedge` for mute and end
- `ContactLedge` for a person and a call action
- `ShortcutLedge` for a stack of icon buttons
- `MediaLedge` for play/pause and repeat
- `AssistantLedge` for a listening glow
- `QuickSettingsLedge` for on/off controls

`LedgeSurface`, `LedgeControl`, and `LedgeToggle` are there when a recipe does not fit. Icons are slots, so the library does not pull in an icon font.

## Gestures

- Drag the handle up or down the edge to choose where the panel sits. The sample also has a position slider.
- Swipe the handle inward, or tap it, and the panel expands from that spot. The sample also has a toolbar action and a **Show actions** button.
- Choose which single panel is parked there: shortcuts, playback, assistant, or quick settings. The call and contact panels appear only while a call is active.
- Drag the edge back outward, tap the dimmed page, or press system back to close.
- The grab zone is excluded from the system back gesture, only around the handle and up to the platform limit of 200dp. The rest of that edge still goes back.
- `LedgeEdge.Start` docks the panel on the leading side. In a left-to-right layout that is the left edge, which also handles back, so the sample defaults to the trailing edge.

`LedgeState` remembers whether the shelf was open across rotation.

## Theme

Panels default to the dark droplet: a straight screen edge, pointed tips, black shell, gray controls, blue active state, red end-call, green answer. Override the palette with `LedgeDefaults.colors(darkPanels = false)` or your own `LedgeColors`, and pass it to `LedgeHost`.

```kotlin
LedgeHost(
    colors = LedgeDefaults.colors(darkPanels = false),
    panels = { /* ... */ },
) { /* content */ }
```

The sample home screen uses a fixed Material 3 scheme so it stays consistent with the shelf. Pass `dynamicColor = true` to `LedgeTheme` if you want the home screen to follow the device wallpaper on Android 12 and newer. The shelf colors stay independent unless you map `LedgeColors` from `MaterialTheme`.

Panel height tracks the available space. On a short window the panels scale down together so the silhouette stays the same.

## Sample behavior

The home screen and the shelf share one `DemoState`:

- Wi-Fi, Bluetooth, airplane mode, and the flashlight stay in sync. Turning airplane mode on clears Wi-Fi and Bluetooth.
- Calling Alex Rivera opens the call popup. Mute and end update the banner on the home screen. Ending the call hides those panels.
- The first launch explains the dock, then asks to draw over other apps and to turn on the Ledge accessibility service so the edge handle stays up on other screens. That service does not read windows or keystrokes. Notifications are optional and only used for the ongoing notice. The sample does not request microphone, phone, contacts, or Bluetooth access. Review setup on the home screen opens that explanation again.

The dock slides in and out on a long ease. While it is moving, the window asks for the display's highest refresh rate and releases that request once the dock is resting. If the system animator scale is off, the slide snaps open and closed instead.
- Playback, repeat, focus, favorites, and the assistant listening state update the cards behind the shelf.

See [docs/CONTINUING.md](docs/CONTINUING.md) for the layout of the code and the current overlay behavior.

These actions are in-app state. The library does not request radio, camera, or microphone permissions. Connect them to real device APIs in your own panels.

## License

Copyright 2026 The Ledge Authors. Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE).

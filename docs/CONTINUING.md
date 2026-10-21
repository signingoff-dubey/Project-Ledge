# Continuing Ledge

This is the handoff for the next person working on the project. The sample is a single parked droplet, not a row of six panels. Call and contact panels appear only while a call is active.

## Layout

| Path | What it owns |
| --- | --- |
| `ledge/src/main/java/com/cedarworks/ledge/` | Library. Host, gestures, motion, controls, presets. |
| `ledge/.../geometry/LedgeGeometry.kt` | Droplet outline. One edge is straight. The other edge is the pointed contour. |
| `ledge/.../LedgeState.kt` | Open and close progress. Tap uses a 480ms ease. A fling uses a spring with no bounce. |
| `ledge/.../LedgeFrameRate.kt` | While the dock is moving, ask for the fastest refresh rate at the current resolution, then release it. |
| `ledge/.../LedgeDefaults.kt` | Resting size is about 220dp by 52dp. Controls scale with that height. |
| `app/.../demo/MainActivity.kt` | Sample shell, onboarding gate, and the choice between the in-app dock and the overlay. |
| `app/.../demo/SamplePanels.kt` | The six sample panels. |
| `app/.../demo/DemoState.kt` | On-screen sample state. It does not change radios, place real calls, or use the microphone. |
| `app/.../onboarding/` | First-launch explanation and the permission coordinator. |
| `app/.../overlay/DockSession.kt` | Anchor, edge, selected panel, and sample state shared by the home screen and the overlay. |
| `app/.../overlay/DockOverlayService.kt` | Foreground service that draws the dock over other apps. |

Package names: library `com.cedarworks.ledge`, sample `com.cedarworks.ledge.demo`, application id `com.cedarworks.ledge`.

## Build

JDK 11. Android Gradle Plugin 9.3.3, Gradle 9.5.0, Kotlin 2.2.10, Compose BOM 2026.02.01. Compile SDK 37 (minor 1), target SDK 36, min SDK 24.

```bat
gradlew.bat :app:assembleDebug :ledge:testDebugUnitTest :app:testDebugUnitTest
```

`local.properties` points at the SDK and is not part of the repo.

## How the dock behaves

One panel is parked on the chosen edge. `anchorFraction` is `0` at the top of the safe area and `1` at the bottom. The home-screen slider and a vertical drag on the handle write the same value.

The straight side of the droplet faces the screen edge. Tips are points. The body is full width through the middle, so controls use `contentWidth` and the vertical padding keeps them out of the tips.

Opening and closing is a horizontal slide. The outline does not squash while it moves. Sub-pixel remainder is applied with `translationX` so the slide does not step by whole pixels. Reduced motion (`Settings.Global.ANIMATOR_DURATION_SCALE` at `0`) snaps instead of easing.

## Outside the app

After setup, if **Draw over other apps** is allowed, `DockOverlayService` keeps a narrow handle on the screen edge. Dragging that handle updates `DockSession`, so the slider in Ledge matches it. Swipe inward to open the full dock. Tap the dimmed page, or press back, to close it. The handle window is only the grab width, so touches outside it reach the app underneath.

The service is a `specialUse` foreground service. Its notification is the optional ongoing notice. Swiping the app away tries to start the service again. If overlay permission is missing, the dock stays inside the activity only.

The sample does not install an accessibility service and does not read other apps.

## Permissions

`DockPermissionPolicy.sample` requests:

- Overlay, required, so the dock can leave the activity.
- Notifications, optional, for that ongoing notice.

Onboarding is three steps: welcome, permissions, done. Continue on the permission step stays disabled until overlay is granted. **Not now** finishes setup without it. The coordinator records a grant only after Android reports it. Review setup on the home screen opens the same flow again.

Do not add microphone, phone, contacts, Bluetooth, or accessibility unless a real feature needs that one permission, and show it in onboarding first.

## Sample content

Original sample copy only. The contact is Alex Rivera. The track is Northline by Harbor & Pine. The assistant is titled Assist. Do not add celebrity photos, Gemini branding, or the name Wemb.

## Checks already in the tree

- `LedgeGeometryTest` checks the point, the full body, the straight edge, and that stretch does not move that edge.
- `LedgeMotionTest` checks drag direction and fling settling.
- `LiquidDockAnimatorTest` checks the stretch value used by the shape API.
- `LedgeFrameRateTest` checks that a faster mode at another resolution is ignored.
- `DockPermissionPolicyTest` checks that the sample asks only for overlay and an optional notice.

## License

Apache License 2.0. Copyright 2026 The Ledge Authors. See [LICENSE](../LICENSE).

# iOS View-Wrapping API Design

## Summary

Replace the current swizzling-based `PathSense.configure()` approach on iOS with a view-wrapping API. The client provides their content view, the SDK wraps it in a container with a transparent drawing overlay on top, and returns the container.

## Architecture

### Component Hierarchy

```
PathSenseContainerView (UIView)
+-- clientView (UIView, provided by client, fills container via autolayout)
+-- TouchOverlayView (isUserInteractionEnabled = false, draws gradient paths)
|   +-- UILabel (coordinate HUD)
+-- PathSenseTouchRecognizer (custom UIGestureRecognizer attached to container)
```

### Touch Interception

`PathSenseTouchRecognizer` is a `UIGestureRecognizer` subclass that:

- Overrides `touchesBegan/Moved/Ended/Cancelled` to read touch coordinates
- Feeds them to `PathTracker.onDown/onMove/onUp` and `TouchOverlayView.notifyTouchStart/Move/End`
- Set `cancelsTouchesInView = false`, `delaysTouchesBegan = false`, `delaysTouchesEnded = false` so client content receives all touches normally
- Transitions to `.began` on first touch, `.changed` on moves, `.ended` on touch up — never `.failed`
- Handles `.cancelled` state (e.g. incoming call) by calling `PathTracker.onCancel()` + `TouchOverlayView.notifyTouchCancel()`
- Tracks first touch only (matching current single-touch behavior)

## Public API

### UIKit

```swift
// Wrap a view and get back a container
let container = PathSense.wrap(view: myView, config: PathSenseConfig())
// Add container to your view hierarchy
parentView.addSubview(container)

// Clear drawn paths
container.clearCanvas()
```

`PathSense.wrap(view:config:)` is a static factory that returns a `PathSenseContainerView`.

### SwiftUI

```swift
@State private var pathSenseAction = PathSenseAction()

var body: some View {
    PathSenseOverlay(config: .init(), action: $pathSenseAction) {
        YourContentView()
    }

    Button("Clear") { pathSenseAction.clearCanvas() }
}
```

`PathSenseOverlay` is a SwiftUI view that uses `UIViewRepresentable` internally, hosting a `PathSenseContainerView` with the SwiftUI content rendered via a `UIHostingController`. The hosting controller is added as a child view controller of the nearest parent VC to ensure proper environment propagation.

`PathSenseAction` is a lightweight class (ObservableObject) that bridges imperative calls (like `clearCanvas()`) from SwiftUI to the underlying container. It is passed via `@Binding` so the overlay's coordinator can connect it to the container instance.

### Enable/Disable

Per-instance control via a property on `PathSenseContainerView`:

```swift
// UIKit
container.isCaptureEnabled = false  // pauses tracking + drawing

// SwiftUI — via action binding
pathSenseAction.isCaptureEnabled = false
```

This replaces the old global `PathSense.enable()`/`disable()`.

### Configuration

`PathSenseConfig`, `PathOverlayConfig`, and `PathStyle` remain unchanged. Config is per-container instance (not global), enabling different configs for different parts of the UI.

## New Files

- `PathSenseContainerView.swift` — UIView subclass: creates container, adds client view + overlay, attaches gesture recognizer, exposes `clearCanvas()`
- `PathSenseTouchRecognizer.swift` — UIGestureRecognizer subclass: observes touches, feeds PathTracker + overlay
- `PathSenseOverlay.swift` — SwiftUI wrapper using UIViewRepresentable
- `TouchOverlayView.swift` — extracted from PathCaptureView.swift (no logic changes)

## Modified Files

- `PathSense.swift` — gut the file: remove `configure()`/`enable()`/`disable()`/`isEnabled`/`clearCanvas()`, all swizzling (`swizzleSendEvent`, UIWindow extension), window lifecycle observers (`didBecomeVisible/Key/Hidden`), `NSMapTable<UIWindow, Attachment>` tracking, `Attachment` inner class, `gestureWindow`/`activeWindow` statics, and all `PathTrackingWindow` type checks. Replace with a single `wrap(view:config:)` static factory method.

## Deleted Files

- `PathTrackingWindow.swift` — UIWindow subclass (replaced by container approach)
- `PathTrackingWindowRepresentable.swift` — SwiftUI wrapper for PathTrackingWindow
- `PathCaptureView.swift` — UIView subclass (replaced; TouchOverlayView extracted first)
- `PathCaptureRepresentable.swift` — SwiftUI wrapper for PathCaptureView
- `PathSenseUI.swift` — deprecated wrapper that calls `PathSense.enable()`/`disable()` (no longer valid)

## Sample App Updates

- `ios-swiftui` sample: replace `PathSense.configure()` in App init with `PathSenseOverlay { ... }` in body
- `ios-uikit` sample: replace `PathSense.configure()` in AppDelegate with `PathSense.wrap(view:config:)` in view controller

## Lifecycle

- `PathSenseContainerView` owns its `PathTracker` instance. Creates it in `init`, calls `tracker.destroy()` in `deinit`.
- `PathSenseOverlay`'s coordinator manages the `UIHostingController` lifecycle — adds it as a child VC of the nearest parent to ensure SwiftUI environment propagation.
- Layout uses `layoutSubviews()` with frame-based sizing (matching current `TouchOverlayView` pattern), not Auto Layout.

## Key Decisions

- **Per-instance config** instead of global state — more flexible, no singleton
- **Gesture recognizer** for touch observation — idiomatic iOS, no swizzling
- **TouchOverlayView unchanged** — proven drawing engine, just extracted to own file
- **PathTracker unchanged** — KMM core unaffected
- `clearCanvas()` is an instance method on the container (bridged to SwiftUI via `PathSenseAction`)
- Minimum deployment target remains iOS 13 (no iOS 14+ APIs like `@StateObject` needed)

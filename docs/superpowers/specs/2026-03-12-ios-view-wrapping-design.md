# iOS Tracking Window Design (Superseded Spec)

## Status

This spec file now tracks the current iOS direction after the breaking migration:

- iOS integration uses `PathSenseTrackingWindow`
- Touch interception happens via `UIWindow.sendEvent(_:)`
- The iOS rendering path is compiled only in `#if DEBUG`
- `PathTracker` remains the source of events/metrics/recognition on iOS

## Current Bootstrap Pattern

```swift
#if DEBUG
let window = PathSenseTrackingWindow(windowScene: windowScene, config: config)
#else
let window = UIWindow(windowScene: windowScene)
#endif
```

## Runtime Controls

- `window.isCaptureEnabled`
- `window.clearCanvas()`
- `window.tracker`

## Notes

- This file was originally created for the intermediate view-wrapping design.
- The codebase now uses the tracking-window model for iOS parity.

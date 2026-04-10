# Bubble Popover Menu Design

## Problem

Tapping the floating bubble in IDLE state immediately starts the recording permission flow. Users need a way to access options (start recording, toggle audio, get info) before committing to a recording.

## Solution

Replace the direct tap-to-record behavior with a popover menu that appears when tapping the bubble in IDLE state. Recording-state tap behavior (immediate stop) remains unchanged.

## Popover UI

### Visual Style

Dark glassmorphic card:
- Background: `#1A1A1A` at ~90% opacity with subtle blur
- Corner radius: 16dp/pt
- Soft shadow
- Small triangular arrow nib pointing toward the bubble

### Menu Items

| Item | Left Icon | Right Element |
|---|---|---|
| **Start Recording** | Filled red circle (gentle pulse) | Chevron `›` |
| **Get More Info** | Rounded info `ⓘ` in accent color | Chevron `›` |
| **Audio** | Waveform/mic icon in accent color | Pill-shaped toggle (green ON / gray OFF) |

### Dimensions & Typography

- Card width: ~200dp/pt
- Row height: ~52dp/pt
- Icons: 20dp/pt, filled style (SF Symbols on iOS, Material Icons on Android)
- Text: 15sp/pt, semi-bold, white
- Dividers: thin `#FFFFFF` at 10% opacity between rows

### Animations

- **Entry:** scale 0.8 → 1.0 + fade in, ~200ms ease-out
- **Exit:** scale → 0.9 + fade out, ~150ms

### Positioning

Appears on the opposite side of the bubble's screen edge (bubble snapped right → popover to the left, and vice versa). Vertically centered on the bubble.

## Platform Implementation

### Android — `PopoverMenuView.kt`

- New custom `FrameLayout` in `com.screenrecorder.bubble` package
- Added to `WindowManager` as a separate overlay (`TYPE_APPLICATION_OVERLAY`), same approach as `BubbleView`
- `BubbleManager` owns the popover lifecycle (show/hide/remove)
- Canvas-drawn background: rounded rect + arrow nib + shadow (consistent with `BubbleView` drawing style)
- Audio toggle state stored in `BubbleManager`, passed to `RecordingOrchestrator` when recording starts

### iOS — `PopoverMenuView.swift`

- New `UIView` subclass in `ScreenRecorderUI` target
- Added as a subview of `BubbleWindow` (already handles pass-through hit testing)
- `BubbleViewController` owns the popover lifecycle
- `UIVisualEffectView` with dark blur material for glassmorphic background
- Audio toggle state stored in `BubbleViewController`, passed to `ScreenRecorder` when recording starts

## Interaction Flow

### IDLE State (changed)

```
Tap bubble → show popover
  ├─ Tap "Start Recording" → dismiss popover → onBubbleTapRecord() → permission → recording
  ├─ Tap "Get More Info" → no-op (placeholder for future)
  ├─ Tap "Audio" toggle → flip audio enabled state
  ├─ Tap outside popover → dismiss
  └─ Drag bubble → dismiss
```

### RECORDING State (unchanged)

```
Tap bubble → onBubbleTapStop() → stop recording → share sheet (Android) / idle (iOS)
```

## Dismiss Triggers

- Tap outside the popover
- Tap "Start Recording"
- Bubble starts dragging
- Recording starts (safety fallback)

## Scope Boundaries

### In Scope
- New `PopoverMenuView` on both Android and iOS
- Modified bubble tap handler for IDLE state
- Audio toggle state management in bubble layer

### Out of Scope
- No changes to `RecordingStateMachine`, `RecordingOrchestrator`, `RecordingController`, or any shared KMM code
- "Get More Info" action — placeholder for future
- Popover during RECORDING state — not needed, tap stops immediately

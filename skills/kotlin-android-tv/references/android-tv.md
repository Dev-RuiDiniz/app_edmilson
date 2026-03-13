# Android TV

## Use This Reference For

- Apps launched from `LEANBACK_LAUNCHER`.
- Screens navigated with D-pad or media remote instead of touch.
- TV playback experiences using Media3, ExoPlayer, WebView, image rotation, or timed content playlists.
- Layout, focus, key-event, and visibility problems that appear only on television devices.

## 10-Foot UI Rules

- Design for distance: larger tap targets, larger typography, fewer competing actions, and stronger contrast.
- Keep the primary action visually obvious on first focus.
- Avoid dense forms and deep nested menus when a remote is the main input device.
- Prefer deterministic focus movement over automatic guesswork in complex grids or overlays.

## Focus and Input

- Set initial focus intentionally when the screen opens.
- Verify directional navigation with `nextFocusUp`, `nextFocusDown`, `nextFocusLeft`, and `nextFocusRight` when default traversal is unreliable.
- Preserve a visible focused state for buttons, cards, and list items.
- Handle back, play/pause, and center/enter keys explicitly when playback or full-screen content is active.
- Avoid focus traps caused by hidden or disabled views that remain focusable.

## Launcher and Manifest Checks

- Confirm the launcher activity includes `android.intent.category.LEANBACK_LAUNCHER` when the app should appear on Android TV home screens.
- Keep `android.software.leanback` and touchscreen requirements aligned with the target devices.
- Provide a TV banner when the product should appear in the TV launcher.

## Playback and Media Surfaces

- Use Media3 or the existing player abstraction already present in the project.
- Release player resources predictably in lifecycle callbacks.
- Keep playback error states recoverable with explicit retry or reload actions.
- When mixing WebView, images, and video, make visibility transitions explicit so only one primary surface is active at a time.
- Track playlist advancement and timers carefully so content rotation does not survive after lifecycle stop or screen exit.

## Layout and Performance

- Prefer simpler hierarchies on TV screens that redraw large surfaces.
- Use resource qualifiers and dimensions tuned for television where the mobile layout becomes cramped or visually weak.
- Avoid surprise soft-keyboard flows; if text input is required, support enter/go actions and restore focus after completion.

## Testing Focus

- Verify first focus, error overlays, retry actions, and back behavior.
- Test with both valid and invalid remote input flows.
- Check behavior after playback errors, network loss, and activity recreation.

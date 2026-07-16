# Android App

Landscape fullscreen keyboard that sends HID key events to Flipper over BLE.

## Architecture

```
KeyboardActivity (fullscreen landscape)
  ├─ BLE button (top-left) → connect / disconnect
  ├─ Settings → Flipper MAC + enabled layouts
  └─ JsonKeyboardView ← assets/layouts/*.json
         │
         ▼
  BridgeSession → FlipperBleClient → BLE Serial TX
```

There is **no** Android IME / system keyboard registration. The app is a normal Activity: keys go to the PC via Flipper, not into other phone apps.

## First-time setup

1. Pair Flipper in system Bluetooth settings (once).
2. Install the APK (`make apk-install`).
3. Open **Flipper KB Bridge**.
4. Tap **⚙ Settings**, select the paired Flipper, enable layouts, **Save**.
5. On Flipper, launch **Android KB Bridge** (USB to PC).
6. Tap the **top-left BLE button** until it turns green (**Connected**).
7. Type on the on-screen keyboard; text appears on the PC.
8. Swipe left/right on the **space bar** to switch layouts.

## BLE button states

| Color | Meaning | Tap action |
|-------|---------|------------|
| Grey | Disconnected | Connect to saved MAC |
| Orange | Connecting / GATT up | Disconnect |
| Green | Ready (can type) | Disconnect |
| Red | Error | Retry connect |

If MAC is not set, Connect opens Settings.

## Screen / orientation

- Locked to **landscape** (does not rotate).
- Immersive fullscreen (status and navigation bars hidden; swipe edge to peek).
- Keep-screen-on while the keyboard activity is open.
- Current layout is always shown in the toolbar as `Layout: …`.
- After a space-bar swipe, a centered banner shows the new layout name for ~1.2 s.

## Keyboard layouts

Layouts live as separate JSON files under `android/app/src/main/assets/layouts/`.

Catalog: `layouts/catalog.json`.

### Important: labels only — no OS integration

On-screen layouts (EN / RU / UA, macOS vs MX Mini, etc.) are **for typing convenience on the phone**. They only change key **labels** and which HID usages are sent for each button.

They do **not** talk to the target Mac/PC:

- Switching layout in this app does **not** change the host input language / keyboard source.
- The PC still interprets physical HID key codes with **its own** active layout.
- Example: the “macOS UA” screen shows `і` / `ї` / `є`, but the host prints those characters only if Ukrainian (or matching) input is selected on the Mac/PC. With a US layout active, the same keys produce Latin letters.

RU/UA JSON files therefore reuse the same HID codes as EN (physical key positions). Pick the matching host input source yourself when you need those glyphs.

### Bundled layouts

| Id | Title |
|----|-------|
| `macos_en` | macOS EN |
| `macos_ru` | macOS RU |
| `macos_ua` | macOS UA |
| `number` | Number |
| `mx_mini_en` | Logitech MX Keys Mini EN |
| `mx_mini_ru` | Logitech MX Keys Mini RU |
| `mx_mini_ua` | Logitech MX Keys Mini UA |

### Switching

- **Settings** → check which layouts are enabled (order follows catalog / save order).
- On the keyboard: **swipe left/right on the space bar** to cycle enabled layouts.
  The active layout name appears as a centered banner and stays in the toolbar (`Layout: …`).
- A short tap on space still inserts a space.

### Schema

```json
{
  "id": "macos_en",
  "name": "macOS EN",
  "rows": [
    [
      {
        "label": "q",
        "hid": "0x14",
        "mods": "0x00",
        "span": 1,
        "sticky_mod": false,
        "role": null
      }
    ]
  ]
}
```

| Field | Required | Description |
|-------|----------|-------------|
| `id` | yes (or catalog) | Stable layout id |
| `name` | no | Shown in the toolbar |
| `rows` | yes | Array of rows; each row is an array of keys |
| `label` | yes | Text drawn on the key |
| `hid` | yes | USB HID usage (hex string, e.g. `"0x04"` for `a`) |
| `mods` | no | Modifier bitmask (hex). Default `0x00` |
| `span` | no | Relative width (supports halves, e.g. `1.5`). Default `1` |
| `sticky_mod` | no | If `true`, tap toggles sticky mods instead of sending a key |
| `role` | no | `"space"` enables swipe-to-switch on that key |

### Sticky modifiers

Works for Shift, Ctrl, Option/Alt, Cmd/GUI — any key with `"sticky_mod": true`:

```json
{"label": "⇧", "hid": "0x00", "mods": "0x02", "span": 1.5, "sticky_mod": true}
{"label": "⌘", "hid": "0x00", "mods": "0x08", "sticky_mod": true}
```

- Tap latches the modifier (key highlights green).
- Several sticky mods can be combined (XOR toggle per modifier bit).
- Next normal key is sent with those modifiers, then sticky state clears.

### Modifier bitmask

Same as protocol `mods` byte (see `docs/PROTOCOL.md`):

| Bit | Mask | Key |
|-----|------|-----|
| 0 | `0x01` | Left Ctrl |
| 1 | `0x02` | Left Shift |
| 2 | `0x04` | Left Alt / Option |
| 3 | `0x08` | Left GUI / Cmd |

### HID examples

| Key | `hid` |
|-----|-------|
| `a`–`z` | `0x04`–`0x1D` |
| `1`–`0` | `0x1E`–`0x27` |
| Enter | `0x28` |
| Escape | `0x29` |
| Backspace | `0x2A` |
| Tab | `0x2B` |
| Space | `0x2C` |

## Settings storage

SharedPreferences file `akb_prefs`:

| Key | Meaning |
|-----|---------|
| `flipper_mac` | Bluetooth address |
| `enabled_layouts` | Comma-separated layout ids |
| `current_layout` | Last active layout id |

## Source map

| Path | Role |
|------|------|
| `KeyboardActivity.kt` | Main UI, fullscreen, BLE, layout cycling |
| `SettingsActivity.kt` | Flipper MAC + enabled layouts |
| `keyboard/KeyboardLayoutLoader.kt` | Catalog + JSON parse |
| `keyboard/JsonKeyboardView.kt` | Draw keys, sticky mods, space swipe |
| `ble/FlipperBleClient.kt` | GATT client + write queue |
| `ble/BridgeProtocol.kt` | Frame encode (key down/up) |
| `prefs/AppPreferences.kt` | MAC + layout prefs |
| `assets/layouts/` | Layout JSON files + catalog |

## Build notes

- JDK **17** required for Gradle (`make apk` auto-detects JDK 17).
- System `gradle` is not required; use `android/gradlew`.
- Release APK: `android/app/build/outputs/apk/release/FlipperZeroKbd.apk`

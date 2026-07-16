# Android App

Landscape fullscreen keyboard that sends HID key events to Flipper over BLE.

## Architecture

```
KeyboardActivity (fullscreen landscape)
  ├─ BLE button (top-left) → connect / disconnect
  ├─ Settings → Flipper MAC
  └─ JsonKeyboardView ← assets/keyboard.json
         │
         ▼
  BridgeSession → FlipperBleClient → BLE Serial TX
```

There is **no** Android IME / system keyboard registration. The app is a normal Activity: keys go to the PC via Flipper, not into other phone apps.

## First-time setup

1. Pair Flipper in system Bluetooth settings (once).
2. Install the APK (`make apk-install`).
3. Open **Flipper KB Bridge**.
4. Tap **⚙ Settings**, select the paired Flipper, **Save**.
5. On Flipper, launch **Android KB Bridge** (USB to PC).
6. Tap the **top-left BLE button** until it turns green (**Connected**).
7. Type on the on-screen keyboard; text appears on the PC.

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

## Keyboard layout JSON

Default file: `android/app/src/main/assets/keyboard.json`.

After editing, rebuild/reinstall the APK. The app loads this asset at startup.

### Schema

```json
{
  "name": "default-qwerty",
  "rows": [
    [
      {
        "label": "q",
        "hid": "0x14",
        "mods": "0x00",
        "span": 1,
        "sticky_mod": false
      }
    ]
  ]
}
```

| Field | Required | Description |
|-------|----------|-------------|
| `name` | no | Layout name (informational) |
| `rows` | yes | Array of rows; each row is an array of keys |
| `label` | yes | Text drawn on the key |
| `hid` | yes | USB HID usage (hex string, e.g. `"0x04"` for `a`) |
| `mods` | no | Modifier bitmask (hex). Default `0x00` |
| `span` | no | Relative width (supports halves, e.g. `1.5`). Default `1` |
| `sticky_mod` | no | If `true`, tap toggles sticky mods instead of sending a key |

### Sticky modifiers

Example Shift key:

```json
{"label": "⇧", "hid": "0x00", "mods": "0x02", "span": 1.5, "sticky_mod": true}
```

- First tap latches Left Shift (key highlights).
- Next normal key is sent with that modifier, then the latch clears.

### Modifier bitmask

Same as protocol `mods` byte (see `docs/PROTOCOL.md`):

| Bit | Mask | Key |
|-----|------|-----|
| 0 | `0x01` | Left Ctrl |
| 1 | `0x02` | Left Shift |
| 2 | `0x04` | Left Alt |
| 3 | `0x08` | Left GUI |

### HID examples

| Key | `hid` |
|-----|-------|
| `a`–`z` | `0x04`–`0x1D` |
| `1`–`0` | `0x1E`–`0x27` |
| Enter | `0x28` |
| Backspace | `0x2A` |
| Tab | `0x2B` |
| Space | `0x2C` |

## Settings storage

Flipper MAC is stored in SharedPreferences:

- file: `akb_prefs`
- key: `flipper_mac`

Value is the Bluetooth address string (e.g. `AA:BB:CC:DD:EE:FF`).

## Source map

| Path | Role |
|------|------|
| `KeyboardActivity.kt` | Main UI, fullscreen, BLE button |
| `SettingsActivity.kt` | Pick paired Flipper MAC |
| `keyboard/KeyboardLayoutLoader.kt` | Parse JSON |
| `keyboard/JsonKeyboardView.kt` | Draw keys from layout |
| `ble/FlipperBleClient.kt` | GATT client + write queue |
| `ble/BridgeProtocol.kt` | Variant B frame encode |
| `prefs/AppPreferences.kt` | MAC persistence |
| `assets/keyboard.json` | Default layout |

## Build notes

- JDK **17** required for Gradle (`make apk` auto-detects JDK 17).
- System `gradle` is not required; use `android/gradlew`.
- Debug APK: `android/app/build/outputs/apk/debug/app-debug.apk`

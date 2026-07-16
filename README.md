# Android Keyboard → Flipper Zero → USB HID

Bluetooth bridge: a landscape Android keyboard app sends HID key events to Flipper Zero over BLE; Flipper injects them into a PC as a USB keyboard.

```
Android app  ──BLE Serial──►  Flipper FAP  ──USB HID──►  PC
```

## Components

| Path | Description |
|------|-------------|
| `docs/BUILD.md` | Build, flash, install, troubleshooting |
| `docs/ANDROID.md` | Android app UI, Settings, multi-layout JSON keyboards |
| `docs/FLIPPER.md` | FAP behavior, USB identity, BLE/RPC notes |
| `docs/PROTOCOL.md` | BLE UUIDs and frame format |
| `flipper/android_keyboard_bridge/` | Flipper FAP sources |
| `android/` | Android app (`assets/layouts/`) |

## Requirements

- Flipper Zero
- Android 8.0+ (API 26), BLE
- USB data cable (Flipper ↔ PC)
- Phone paired with Flipper in system Bluetooth settings
- Local `flipperzero-firmware` checkout to build the FAP
- JDK 17 for command-line Android builds

## Quick start

```bash
# Flipper
make flipper-link
make flipper-launch

# Android
make apk-install
```

Then on the phone:

1. **Settings** → select paired Flipper, enable layouts → Save  
2. Tap the **top-left BLE button** (green = ready)  
3. Type; characters appear on the PC  
4. Swipe on the **space bar** to switch layouts (banner + toolbar show the active name)  

Details: `docs/ANDROID.md`, `docs/FLIPPER.md`.

Bundled layouts: macOS EN/RU/UA, Number, Logitech MX Keys Mini EN/RU/UA.

## Build shortcuts

```bash
make help
make apk
make apk-install
make apk-release          # → FlipperZeroKbd.apk
make apk-release-install
make flipper-link
make flipper-build
make flipper-flash
make flipper-launch
make flipper-cli
```

Full instructions: `docs/BUILD.md`.

## Protocol

Each tap is `key_down` then `key_up` (6-byte frames):

```text
FB 4B 03 [01|02] [mods] [keycode]
```

See `docs/PROTOCOL.md`.

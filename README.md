# Android Keyboard → Flipper Zero → USB HID

Bluetooth bridge / direct HID: a landscape Android app can send keyboard and mouse events either through Flipper Zero (BLE Serial → USB HID) or **directly to a PC over Bluetooth HID** (no Flipper).

```
Android app  ──BLE Serial──►  Flipper FAP  ──USB HID──►  PC
```

## Motivation

I often work with devices that ship without a keyboard and needed something universal — a keyboard that can reach hosts over as many interfaces as practical (USB via Flipper today, direct Bluetooth HID from the phone, more later). Dedicated mini keyboards help, but their radio dongles kept going missing, which got old fast. A phone is almost always in reach, and a Flipper Zero is harder to lose than a tiny USB stick — so the phone becomes the keys, and Flipper (or Bluetooth) is the cable into the target.

## Components

| Path | Description |
|------|-------------|
| `docs/BUILD.md` | Build, flash, install, troubleshooting |
| `docs/ANDROID.md` | Android app UI, Settings, multi-layout JSON keyboards |
| `docs/FLIPPER.md` | FAP behavior, USB identity, BLE/RPC notes |
| `docs/PROTOCOL.md` | BLE UUIDs and frame format |
| `flipper/android_keyboard_bridge/` | Flipper FAP sources (**C** — production / default) |
| `flipper/android_keyboard_bridge_rust/` | Same FAP in **Rust**, for educational purposes only |
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

1. **Settings** → choose **Via Flipper** or **Direct Bluetooth to PC** (Flipper MAC required; PC optional for Direct), enable layouts → Save  
2. Tap the **top-left** connection button (green = ready; blue = waiting for PC to pair in Direct mode)  
3. For Direct BT without a saved PC: accept discoverable, then pair/connect from the PC — host MAC is saved automatically  
4. Type on the keyboard, or use the top-center **Keyboard | Touchpad** switch  
5. Swipe on the **space bar** to switch layouts (banner + toolbar show the active name)  

Details: `docs/ANDROID.md`, `docs/FLIPPER.md`.

Bundled layouts: macOS EN/RU/UA, Number, Logitech MX Keys Mini EN/RU/UA.

Layout screens are for convenience only — they do not switch the Mac/PC input language; match the host keyboard source yourself (see `docs/ANDROID.md`).

## Build shortcuts

```bash
make help
make apk
make apk-install
make apk-release          # → FlipperZeroKbd-<version>.apk
make apk-release-install
make flipper-link
make flipper-build
make flipper-flash
make flipper-launch
make flipper-rust-build   # optional educational Rust FAP (.fap)
make flipper-cli
```

Full instructions: `docs/BUILD.md`.

## Protocol

Keyboard taps use `key_down` / `key_up`; touchpad uses mouse move / button / scroll frames (same 6-byte header). See `docs/PROTOCOL.md`.

## Credits

Built with help from [Cursor](https://cursor.com).

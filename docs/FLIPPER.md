# Flipper FAP — Android KB Bridge

External app (`android_keyboard_bridge.fap`) that receives BLE Serial frames from the phone and injects USB HID keyboard reports into the host PC.

## Runtime behavior

On start:

1. Switches Flipper USB mode to standard HID keyboard (`usb_hid`).
2. Starts the BLE **Serial** profile and advertises.
3. Reclaims the Serial RX callback from the system RPC service (otherwise phone writes go to RPC, not this FAP).
4. Shows status UI; **Back** exits and **Up** toggles forced backlight — both via `RECORD_INPUT_EVENTS` pubsub (independent of the main loop / HID delays).

On stop / Back:

1. Panic-releases all USB keys.
2. Restores the default BLE profile.
3. Restores the previous USB configuration when possible.
4. Restores normal backlight auto-off if Up had locked it on.

## On-screen status

| Line | Meaning |
|------|---------|
| Status text | e.g. `Waiting for phone…`, `Last RX: N bytes`, `HID down key=XX` |
| `USB: connected` / `waiting` | Host accepted the HID interface (`furi_hal_hid_is_connected`) |
| `Phone: connected` / `waiting` | BLE GAP connected (or first RX seen) |
| `Frames:N HID:M` | Parsed protocol frames vs HID commands applied on the app thread |
| Hint line | `Up=light  Back=exit` — `light*` means backlight is forced on |

Useful checks while typing:

- `Frames` should increase (usually +2 per tap: down + up).
- `HID` should increase in step with frames.
- Status should alternate `HID down` / `HID up`.

## USB identity on the host

Flipper’s stock HID stack advertises as **Logitech-like**:

| Field | Value |
|-------|-------|
| Vendor ID | `0x046D` |
| Product ID | `0xC529` |

On macOS it often appears as a nameless “Composite Device” / keyboard — **not** as “Flipper”:

```bash
hidutil list | grep -i '0xc529\|0x46d'
system_profiler SPUSBDataType | grep -A8 '0xc529'
```

While this FAP owns USB HID, the Flipper CDC serial port (`/dev/cu.usbmodem…`) may disappear. That is expected.

## BLE / RPC reclaim

The firmware BT service opens an RPC session whenever the Serial profile connects and steals the Serial event callback. This FAP:

- registers `bt_set_status_changed_callback`
- on `BtStatusConnected`, reclaims RX with `ble_profile_serial_set_event_callback`
- repeats reclaim for a few main-loop ticks after connect

Do **not** call `ble_profile_serial_notify_buffer_is_empty` from inside the Serial RX callback: the service already holds `buff_size_mtx`, and re-entering it deadlocks after the first packet. Notify is deferred to the main loop.

## HID application path

BLE RX only **enqueues** commands. The main loop applies them with a short gap (~25 ms) so the host sees press before release:

```
BLE callback → protocol parse → hid_queue
main loop    → usb_hid_key_down / key_up → USB
```

Key down/up events map to `furi_hal_hid_kb_press` / `release` with `(mods << 8) | keycode`.

## Hardware buttons (input pubsub)

Both actions use a subscription to `RECORD_INPUT_EVENTS` (hardware buttons after debounce), not the ViewPort input path — so they still work while the main loop is busy applying HID.

| Button | Action |
|--------|--------|
| **Back** | Exit FAP (`exit_requested`). Accepts Short / Long / Press. |
| **Up** | Toggle forced backlight on/off. Short only (avoids double-toggle). |

Forced on uses `sequence_display_backlight_enforce_on`; off restores `sequence_display_backlight_enforce_auto`. Leaving the app always restores auto.

## Source map

| File | Role |
|------|------|
| `android_keyboard_bridge.c` | App UI, BLE lifecycle, HID queue pump, Back / Up |
| `protocol.c` / `protocol.h` | Frame sync + key down/up parse |
| `usb_hid_bridge.c` / `.h` | USB HID mode + key down/up |
| `application.fam` | FAP metadata (`stack_size` 4 KiB) |

## Build / deploy

See `docs/BUILD.md`. Shortcuts from project root:

```bash
make flipper-link
make flipper-build
make flipper-launch
```

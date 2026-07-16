#pragma once

#include <stdint.h>
#include <stdbool.h>

#define AKB_HID_REPORT_LEN 8

void usb_hid_bridge_start(void);
void usb_hid_bridge_stop(void);
bool usb_hid_bridge_is_usb_connected(void);

/** Variant A: replace entire keyboard state with boot report. */
void usb_hid_apply_report(const uint8_t report[AKB_HID_REPORT_LEN]);

/** Variant B: press/release one key (+ optional modifiers bitmask). */
void usb_hid_key_down(uint8_t mods, uint8_t keycode);
void usb_hid_key_up(uint8_t mods, uint8_t keycode);

void usb_hid_panic_release(void);

#pragma once

#include <stdint.h>
#include <stdbool.h>

void usb_hid_bridge_start(void);
void usb_hid_bridge_stop(void);
bool usb_hid_bridge_is_usb_connected(void);

void usb_hid_key_down(uint8_t mods, uint8_t keycode);
void usb_hid_key_up(uint8_t mods, uint8_t keycode);
void usb_hid_panic_release(void);

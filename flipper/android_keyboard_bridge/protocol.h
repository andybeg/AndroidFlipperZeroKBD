#pragma once

#include <stdint.h>
#include <stddef.h>
#include <furi.h>

#define AKB_MAGIC_0 0xFB
#define AKB_MAGIC_1 0x4B

/** Key down/up event frame: FB 4B 03 [event][mods][keycode] */
#define AKB_FRAME_PAYLOAD_LEN 3
#define AKB_FRAME_TOTAL_LEN   6
#define AKB_EVENT_DOWN        0x01
#define AKB_EVENT_UP          0x02

typedef enum {
    AkbHidCmdKeyDown,
    AkbHidCmdKeyUp,
    AkbHidCmdPanic,
} AkbHidCmdType;

typedef struct {
    AkbHidCmdType type;
    uint8_t mods;
    uint8_t keycode;
} AkbHidCmd;

typedef struct {
    uint8_t buffer[256];
    size_t length;
    FuriMessageQueue* hid_queue;
} AkbProtocolParser;

void akb_protocol_init(AkbProtocolParser* parser, FuriMessageQueue* hid_queue);

/** Feed BLE bytes; returns number of accepted frames enqueued for USB. */
size_t akb_protocol_feed(AkbProtocolParser* parser, const uint8_t* data, size_t size);

#include "pebble_os.h"
#include "pebble_app.h"
#include "pebble_fonts.h"
#include "loadingWindow.h"
#include "messaging.h"
#include "korean_text_layer.h"

#define MY_UUID { 0x98, 0x24, 0x92, 0xE8, 0x50, 0xDB, 0x4A, 0xE3, 0xA6, 0x9A, 0x1B, 0x43, 0xE0, 0x12, 0xC9, 0x3F }
PBL_APP_INFO(MY_UUID,
             "Korean Pebble", "Your Company",
             1, 0, /* App version */
             RESOURCE_ID_ICON,
             APP_INFO_STANDARD_APP);

static AppMessageCallbacksNode app_callbacks;

AppMessageCallbacksNode callback;

void handle_deinit(AppContextRef ctx) {
  korean_text_layer_system_deinit();
}

void handle_init(AppContextRef ctx) {
  (void)ctx;
  openLoadingWindow();
  resource_init_current_app(&APP_RESOURCES);

  // init font
  korean_text_layer_system_init();

  registerMessageCallbacks(&app_callbacks);

  notifyToPhone();
}

void pbl_main(void *params) {
  PebbleAppHandlers handlers = {
    .init_handler = &handle_init,
    .deinit_handler = &handle_deinit,
    .messaging_info = {
      .buffer_sizes = {
        .inbound = 512, // inbound buffer size in bytes
        .outbound = 32, // outbound buffer size in bytes
      },
    },
  };
  app_event_loop(params, &handlers);
}
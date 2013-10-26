#include "pebble_os.h"
#include "pebble_app.h"
#include "pebble_fonts.h"
#include "korean_text_layer.h"

#define MY_UUID { 0x98, 0x24, 0x92, 0xE8, 0x50, 0xDB, 0x4A, 0xE3, 0xA6, 0x9A, 0x1B, 0x43, 0xE0, 0x12, 0xC9, 0x3F }
PBL_APP_INFO(MY_UUID,
             "Template App", "Your Company",
             1, 0, /* App version */
             DEFAULT_MENU_ICON,
             APP_INFO_STANDARD_APP);

Window window;
KoreanTextLayer titleLayer, bodyLayer;
TextLayer statusLayer;
static AppMessageCallbacksNode app_callbacks;
uint8_t retryCount;

AppMessageCallbacksNode callback;

void notifyToPhone() {
  text_layer_set_text(&statusLayer, "Notify");
  DictionaryIterator *dict;
  app_message_out_get(&dict);
  if (dict == NULL)
    return;
  
  dict_write_uint8(dict, 0, 0);
  dict_write_end(dict);
  
  app_message_out_send();
  app_message_out_release();
}

char buf[18] = {0};
void in_received(DictionaryIterator *received, void *context) {
  Tuple *value = dict_find(received, 0);
  if (!value) {
    return;
  }
  switch(value->value->uint8) {
  case 0: // ping
    notifyToPhone();
    break;
  case 1: // notification
    value = dict_find(received, 2);
    korean_text_layer_set_text(&titleLayer, value->value->data, value->length);
    value = dict_find(received, 3);
    korean_text_layer_set_text(&bodyLayer, value->value->data, value->length);
    snprintf(buf, 18, "Received%d", *value->value->data);
    text_layer_set_text(&statusLayer, buf);
    break;
  }
  value = dict_find(received, 1);
  if (value) {
    switch (value->value->uint8) {
    case 1:
      vibes_short_pulse(); break;
    case 2:
      vibes_double_pulse(); break;
    case 3:
      vibes_long_pulse(); break;
    default:
      break;
    }
  }
}

void printReason(AppMessageResult reason) {
  if (reason == APP_MSG_CALLBACK_NOT_REGISTERED) {
    text_layer_set_text(&statusLayer, "Hello not registered!");
  } else if (reason == APP_MSG_CALLBACK_ALREADY_REGISTERED) {
    text_layer_set_text(&statusLayer, "Hello already registered!");
  } else if (reason == APP_MSG_ALREADY_RELEASED) {
    text_layer_set_text(&statusLayer, "Hello already released!");
  }else if (reason == APP_MSG_BUFFER_OVERFLOW) {
    text_layer_set_text(&statusLayer, "Hello buff over!");
  }else if (reason == APP_MSG_BUSY) {
    text_layer_set_text(&statusLayer, "Hello busy!");
  }else if (reason == APP_MSG_INVALID_ARGS) {
    text_layer_set_text(&statusLayer, "Hello invalid args!");
  }else if (reason == APP_MSG_APP_NOT_RUNNING) {
    text_layer_set_text(&statusLayer, "Hello not running!");
  }else if (reason == APP_MSG_OK) {
    text_layer_set_text(&statusLayer, "Hello ok!");
  }else if (reason == APP_MSG_NOT_CONNECTED) {
    text_layer_set_text(&statusLayer, "Hello not connected!");
  }else if (reason == APP_MSG_SEND_REJECTED) {
    text_layer_set_text(&statusLayer, "Hello send rejected!");
  }else if (reason == APP_MSG_SEND_TIMEOUT) {
    text_layer_set_text(&statusLayer, "Hello send timeout!");
  }else {
    text_layer_set_text(&statusLayer, "Hello Dropped!");
  }
}

void in_dropped(void *context, AppMessageResult reason) {
  printReason(reason);
}

void out_sent(DictionaryIterator *sent, void *context) {
  retryCount = 0;
  text_layer_set_text(&statusLayer, "Sent");
}

void out_failed(DictionaryIterator *failed, AppMessageResult reason, void *context) {
  printReason(reason);
}

void handle_deinit(AppContextRef ctx) {
  korean_text_layer_system_deinit();
}

void handle_init(AppContextRef ctx) {
  (void)ctx;
  window_init(&window, "Window Name");
  window_stack_push(&window, true /* Animated */);
  resource_init_current_app(&APP_RESOURCES);

  // init font
  korean_text_layer_system_init();

  korean_text_layer_init(&titleLayer, GRect(0, 0, 144, 20));
  layer_add_child(&window.layer, &titleLayer.layer);
  korean_text_layer_init(&bodyLayer, GRect(0, 20, 144, 100));
  layer_add_child(&window.layer, &bodyLayer.layer);

  text_layer_init(&statusLayer, GRect(0, 120, 144, 18));
  text_layer_set_font(&statusLayer, fonts_get_system_font(FONT_KEY_GOTHIC_18_BOLD));
  text_layer_set_text(&statusLayer, "Ready");
  layer_add_child(&window.layer, &statusLayer.layer);

  app_callbacks = (AppMessageCallbacksNode){
    .callbacks = {
      .in_received = in_received,
      .in_dropped = in_dropped,
      .out_sent = out_sent,
      .out_failed = out_failed,
    }
  };
  app_message_register_callbacks(&app_callbacks);

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
#include "stdint.h"
#include "pebble_os.h"
#include "messaging.h"
#include "loadingWindow.h"
#include "notificationWindow.h"
#include "mainWindow.h"

static uint8_t retryCount;

void notifyToPhone() {
  text_layer_set_text(loadingMessageLayer, "Notify");
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
void printReason(AppMessageResult reason) {
  if (reason == APP_MSG_CALLBACK_NOT_REGISTERED) {
    text_layer_set_text(loadingMessageLayer, "not registered!");
  } else if (reason == APP_MSG_CALLBACK_ALREADY_REGISTERED) {
    text_layer_set_text(loadingMessageLayer, "already registered!");
  } else if (reason == APP_MSG_ALREADY_RELEASED) {
    text_layer_set_text(loadingMessageLayer, "already released!");
  }else if (reason == APP_MSG_BUFFER_OVERFLOW) {
    text_layer_set_text(loadingMessageLayer, "buff over!");
  }else if (reason == APP_MSG_BUSY) {
    text_layer_set_text(loadingMessageLayer, "busy!");
  }else if (reason == APP_MSG_INVALID_ARGS) {
    text_layer_set_text(loadingMessageLayer, "invalid args!");
  }else if (reason == APP_MSG_APP_NOT_RUNNING) {
    text_layer_set_text(loadingMessageLayer, "not running!");
  }else if (reason == APP_MSG_OK) {
    text_layer_set_text(loadingMessageLayer, "ok!");
  }else if (reason == APP_MSG_NOT_CONNECTED) {
    text_layer_set_text(loadingMessageLayer, "not connected!");
  }else if (reason == APP_MSG_SEND_REJECTED) {
    text_layer_set_text(loadingMessageLayer, "send rejected!");
  }else if (reason == APP_MSG_SEND_TIMEOUT) {
    text_layer_set_text(loadingMessageLayer, "send timeout!");
  }else {
    text_layer_set_text(loadingMessageLayer, "Dropped!");
  }
}

void in_received(DictionaryIterator *received, void *context) {
  text_layer_set_text(loadingMessageLayer, "Received");
  Tuple *value = dict_find(received, 0);
  if (!value) {
    return;
  }
  switch(value->value->uint8) {
  case 0: // ping
    notifyToPhone();
    break;
  case 1: // notification
    openNotificationWindow();
    value = dict_find(received, 2);
    setNotificationText(nlt_title, value->value->cstring);
    value = dict_find(received, 3);
    setNotificationText(nlt_body, value->value->cstring);
    break;
  case 2: // notification Append
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

void in_dropped(void *context, AppMessageResult reason) {
  if (reason == APP_MSG_SEND_REJECTED) {
    openMainWindow();
    return;
  }

  printReason(reason);
}

void out_sent(DictionaryIterator *sent, void *context) {
  text_layer_set_text(loadingMessageLayer, "Sent");
  retryCount = 0;
}

void out_failed(DictionaryIterator *failed, AppMessageResult reason, void *context) {
  if (reason == APP_MSG_SEND_REJECTED ||
      (window_stack_get_top_window() == loadingWindow && reason == APP_MSG_SEND_TIMEOUT)) {
    openMainWindow();
    return;
  }

  printReason(reason);
}

void registerMessageCallbacks(AppMessageCallbacksNode *app_callbacks) {
  *app_callbacks = (AppMessageCallbacksNode){
    .callbacks = {
      .in_received = in_received,
      .in_dropped = in_dropped,
      .out_sent = out_sent,
      .out_failed = out_failed,
    }
  };
	
  app_message_register_callbacks(app_callbacks);
}
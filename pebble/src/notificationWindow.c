#include "pebble_os.h"
#include "notificationWindow.h"
#include "loadingWindow.h"
#include "korean_text_layer.h"

static Window window;
Window *notificationWindow = &window;
static KoreanTextLayer titleLayer, bodyLayer;
static bool isInitialized = false;

void initNotificationWindow() {
  if (isInitialized) {
    return;
  }

  window_init(&window, "Notification Window");
  korean_text_layer_init(&titleLayer, GRect(0, 0, 144, 20));
  layer_add_child(&window.layer, (Layer *)&titleLayer.layer);
  korean_text_layer_init(&bodyLayer, GRect(0, 20, 144, 100));
  layer_add_child(&window.layer, (Layer *)&bodyLayer.layer);

  isInitialized = true;
}

void openNotificationWindow() {
  initNotificationWindow();

  window_stack_push(&window, false);

  closeLoadingWindow();
}

void setNotificationText(notificationLabelType_e type, const char *text) {
  KoreanTextLayer *textLayer = NULL;

  switch(type) {
    case nlt_title:
      textLayer = &titleLayer;
      break;
    case nlt_body:
      textLayer = &bodyLayer;
      break;
  }

  korean_text_layer_set_text(textLayer, text);
}

void appendNotificationText(notificationLabelType_e type, const char *text) {
  KoreanTextLayer *textLayer = NULL;

  switch(type) {
    case nlt_title:
      textLayer = &titleLayer;
      break;
    case nlt_body:
      textLayer = &bodyLayer;
      break;
  }

  korean_text_layer_set_text(textLayer, text);
}
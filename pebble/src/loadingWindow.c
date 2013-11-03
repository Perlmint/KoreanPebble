#include "pebble_os.h"
#include "loadingWindow.h"
#include "pebble_fonts.h"

static Window window;
Window *loadingWindow = &window;
static TextLayer messageLayer;
TextLayer *loadingMessageLayer = &messageLayer;
static bool isInitialized = false;

void initLoadingWindow() {
  if (isInitialized) {
    return;
  }

  window_init(&window, "Loading Window");
  window_set_background_color(&window, GColorWhite);
  text_layer_init(&messageLayer, window.layer.frame);
  text_layer_set_font(&messageLayer, fonts_get_system_font(FONT_KEY_GOTHIC_18_BOLD));
  text_layer_set_text_alignment(&messageLayer, GTextAlignmentCenter);
  layer_add_child(&window.layer, (Layer *)&messageLayer);
  text_layer_set_text(&messageLayer, "Loading...");
  isInitialized = true;
}

void openLoadingWindow() {
  initLoadingWindow();

  window_stack_push(&window, false);
}

void closeLoadingWindow() {
  if (!window_stack_contains_window(&window) && window_stack_get_top_window() == loadingWindow) {
    return;
  }

  window_stack_remove(&window, false);
  layer_remove_child_layers(&window.layer);
  window_deinit(&window);
}

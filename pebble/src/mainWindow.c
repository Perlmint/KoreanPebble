#include "pebble_os.h"
#include "mainWindow.h"
#include "loadingWindow.h"

static Window window;
Window *mainWindow = &window;
static bool isInitialized = false;
static SimpleMenuLayer mainMenuLayer;
static const SimpleMenuItem mainItems[1] = {
  {
     .title = "Find Phone"
  }
};
SimpleMenuSection mainSection = 
  {
    .num_items = sizeof(mainItems) / sizeof(SimpleMenuItem),
    .items = (const SimpleMenuItem *)&mainItems
  };

void initMainWindow() {
  if (isInitialized) {
    return;
  }

  window_init(&window, "Main Window");
  simple_menu_layer_init(&mainMenuLayer, window.layer.frame, &window, &mainSection, 1, NULL);
  layer_add_child(&window.layer, simple_menu_layer_get_layer(&mainMenuLayer));
  isInitialized = true;
}

void openMainWindow() {
  initMainWindow();

  window_stack_push(&window, false);

  closeLoadingWindow();
}
#pragma once
#include "pebble_os.h"

typedef struct KoreanTextLayer_ {
  TextLayer layer;
} KoreanTextLayer;

void korean_text_layer_system_init();
void korean_text_layer_system_deinit();
void korean_text_layer_init(KoreanTextLayer *layer, const GRect rect);
void korean_text_layer_set_text(KoreanTextLayer *layer, const char *buf);

uint32_t extractOneCharacterFromUTF8Str(const char **str);
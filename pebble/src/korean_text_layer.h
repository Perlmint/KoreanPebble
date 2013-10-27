#pragma once
#include "pebble_os.h"

typedef struct KoreanTextLayer_ {
  TextLayer layer;
} KoreanTextLayer;

/**
 * Application wide init for korean_text_layer
 * call this function on app init sequence
 */
void korean_text_layer_system_init();

/**
 * Application wide deinit for korean_text_layer
 * call this function on app deinit sequence
 */
void korean_text_layer_system_deinit();

/**
 * Initialize korean_text_layer
 * default values :
 * * Alignment : left
 * @param layer target korean_text_layer
 * @param rect position and size of layer
 */
void korean_text_layer_init(KoreanTextLayer *layer, const GRect rect);

/**
 * Set text for korean_text_layer
 * @param layer target korean_text_layer
 * @param buf text
 */
void korean_text_layer_set_text(KoreanTextLayer *layer, const char *buf);

/**
 * Extract only one character code from utf-8 string
 * @param str original utf-8 string
 * @param ret unicode of extracted character
 * @return just one character wide forwarded string position
 */
const char *extractOneCharacterFromUTF8Str(const char *str, uint32_t *ret);

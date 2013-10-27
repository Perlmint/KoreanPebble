#pragma once
#include "pebble_os.h"

/**
 * Render korean character
 */
void renderKoreanCharacter(GContext *ctx, GBitmap *fontCursor, GPoint *point, const uint32_t ch);
/**
 * Calculate width of given korean character
 */
uint8_t getKoreanCharacterWidth(uint32_t c);

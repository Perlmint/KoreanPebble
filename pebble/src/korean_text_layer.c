#include "korean_text_layer.h"
#include "korean_text_layer_internal.h"
#include "koreanProcessor.h"
#include "pebble_app.h"
#include "pebble_fonts.h"

HeapBitmap font;
GBitmap fontCursor;

void korean_text_layer_update(struct Layer *layer, GContext *ctx);

char_family_t getCharacterFamily(uint32_t c) {
  if (c >= ' ' && c <= '~') {
    return ascii;
  }
  // 가 ~ 힣
  if (c >= 44032 && c <= 55203) {
    return korean;
  }
  // ㄱ ~ ㅎ ㅏ ~ ㅣ
  if (c >= 12593 && c <= 12643) {
    return korean;
  }
  return unknown;
}

uint8_t getCharacterWidth(uint32_t c) {
  char_family_t family = getCharacterFamily(c);
  switch (family) {
    case korean:
      return getKoreanCharacterWidth(c);
    default:
      return 9;
  }
}

void korean_text_layer_system_init() {
  heap_bitmap_init(&font, RESOURCE_ID_KOREAN_FONT_18);
  gbitmap_init_as_sub_bitmap(&fontCursor, &font.bmp, GRect(0, 0, 9, 18));
}

void korean_text_layer_system_deinit() {
  heap_bitmap_deinit(&font);
}

void korean_text_layer_init(KoreanTextLayer *layer, const GRect rect) {
  text_layer_init(&layer->layer, rect);
  layer_set_update_proc((Layer *)&layer->layer, &korean_text_layer_update);
  ((KoreanTextLayer *)layer)->layer.text_alignment = GTextAlignmentLeft;
}

void korean_text_layer_set_text(KoreanTextLayer *layer, const char *buf) {
  text_layer_set_text(&layer->layer, buf);
  layer_mark_dirty((Layer *)&layer->layer);
}

void renderAscii(GContext *ctx, GBitmap *fontCursor, GPoint *point, const uint32_t ch) {
  uint32_t code = ch - ' ';
  fontCursor->bounds.size.w = 9;
  fontCursor->bounds.origin.x = (code & 0x0F) * 9;
  fontCursor->bounds.origin.y = (code >> 4) * 18;
  graphics_draw_bitmap_in_rect(ctx, fontCursor, GRect(point->x, point->y, 9, 18));
  point->x += 9;
}

void renderUnknown(GContext *ctx, GBitmap *fontCursor, GPoint *point, const uint32_t ch) {
  fontCursor->bounds.size.w = 9;
  fontCursor->bounds.origin.x = 15 * 9;
  fontCursor->bounds.origin.y = 5 * 18;
  graphics_draw_bitmap_in_rect(ctx, fontCursor, GRect(point->x, point->y, 9, 18));
  point->x += 9;
}

void korean_text_layer_render_line(GContext *ctx, GPoint *point, const char *bufStart, const char *bufEnd) {
  for(uint32_t ch; bufStart != bufEnd;) {
    if (*bufStart == '\n') { // newline character should be at end of string
      break;
    }
    bufStart = extractOneCharacterFromUTF8Str(bufStart, &ch);
    switch(getCharacterFamily(ch)) {
      case korean:
        renderKoreanCharacter(ctx, &fontCursor, point, ch);
        break;
      case ascii:
        renderAscii(ctx, &fontCursor, point, ch);
        break;
      default:
        renderUnknown(ctx, &fontCursor, point, 0);
        break;
    }
  }
  point->y += 18;
}

uint16_t calcRenderStartPosition(const GTextAlignment alignment, const uint16_t width, const uint16_t lineWidth) {
  switch (alignment) {
    case GTextAlignmentLeft:
      return 0;
      break;
    case GTextAlignmentCenter:
      return (width - lineWidth) / 2;
      break;
    case GTextAlignmentRight:
      return width - lineWidth;
      break;
  }
  return 0;
}

void korean_text_layer_update(struct Layer *layer, GContext *ctx) {
  uint16_t lineWidth = 0,
    width = layer_get_frame(layer).size.w,
    height = layer_get_frame(layer).size.h;
  GPoint renderPoint = {
    .x = 0,
    .y = 0
    };

  // Setup layer for drawing
  graphics_context_set_compositing_mode(ctx, GCompOpAssign);
  graphics_context_set_fill_color(ctx, GColorClear);
  graphics_fill_rect(ctx, layer_get_frame(layer), 0, GCornerNone);
  graphics_context_set_compositing_mode(ctx, GCompOpAnd);

  const char *bufStart = text_layer_get_text(&(((KoreanTextLayer *)layer)->layer)),
             *bufEnd = 0;
  bufEnd = bufStart;

  for(uint32_t ch; renderPoint.y < height;) {
    if (*bufEnd == 0) { // string is end... render remain string and quit
      // Set Line render start Position
      renderPoint.x = calcRenderStartPosition(((KoreanTextLayer *)layer)->layer.text_alignment, width, lineWidth);
      korean_text_layer_render_line(ctx, &renderPoint, bufStart, bufEnd);
      break;
    }

    const char *newEnd = extractOneCharacterFromUTF8Str(bufEnd, &ch);

    if (ch == '\n') { // new line
      bufEnd = newEnd;
    } else {
      uint8_t chWidth = getCharacterWidth(ch);

      if (chWidth + lineWidth < width) {
        bufEnd = newEnd;
        lineWidth += chWidth;
        continue;
      }
    }

    // Set Line render start Position
    renderPoint.x = calcRenderStartPosition(((KoreanTextLayer *)layer)->layer.text_alignment, width, lineWidth);
    korean_text_layer_render_line(ctx, &renderPoint, bufStart, bufEnd);
    
    lineWidth = 0;
    bufStart = bufEnd;
  }
}

/*
  7	U+0000	U+007F	1	0xxxxxxx
11	U+0080	U+07FF	2	110xxxxx	10xxxxxx
16	U+0800	U+FFFF	3	1110xxxx	10xxxxxx	10xxxxxx
21	U+10000	U+1FFFFF	4	11110xxx	10xxxxxx	10xxxxxx	10xxxxxx
26	U+200000	U+3FFFFFF	5	111110xx	10xxxxxx	10xxxxxx	10xxxxxx	10xxxxxx
31	U+4000000	U+7FFFFFFF	6	1111110x	10xxxxxx	10xxxxxx	10xxxxxx	10xxxxxx	10xxxxxx
*/

const char *extractOneCharacterFromUTF8Str(const char *str, uint32_t *ret) {
  int8_t count = 0;
  *ret = 0;
  for(; count < 8; ++count) {
    uint8_t tmp = 0x80 >> count;
    if ((*str & tmp) != tmp) {
      break;
    }
  }
  if (count == 0) {
    *ret = *str;
    return ++str;
  }
  count--;
  *ret = *str & (0x3F >> (count));
  for(++str;count > 0; --count, ++str) {
    *ret <<= 6;
    *ret |= *str & 0x3F;
  }
  return str;
}

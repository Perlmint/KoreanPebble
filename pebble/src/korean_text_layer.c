#include "korean_text_layer.h"
#include "pebble_app.h"

HeapBitmap font;
GBitmap fontCursor;
GPoint *fontCursorOrigin;

void korean_text_layer_update(struct Layer *layer, GContext *ctx);

typedef enum {
    first = 1, middle_u = 2, middle_rl = 3, middle_rs = 4, last = 5, none = 0
  } glyphType_t;

uint8_t getGlyphWidth(uint8_t c, glyphType_t *current) {
  if (c <= '~') {
    *current = none;
    return 9;
  }
  c -= '~';
  switch(c) {
    case 'y': case 'u': case 'i': case 'o':
    case 'p': case '0': case 'h': case 'j':
    case 'k': case 'l': case ';': case '\'':
    case 'n': case 'm': 
      *current = first;
      return 9;
    case '4': case '5': case '9': case 'g':
    case 'v': case 'b': case '/': case '8':
      *current = middle_u;
      return 2;
    case '7': case 'e': case 'r': case 'c':
    case 'G': 
      *current = middle_rl;
      return 6;
    case '6': case 't': case 'd': case 'f':
      *current = middle_rs;
      return 4;
    default:
      *current = last;
      return 0;
  }
  return 9;
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
  fontCursorOrigin = &fontCursor.bounds.origin;
  layer_set_update_proc(&layer->layer, &korean_text_layer_update);
}

void korean_text_layer_set_text(KoreanTextLayer *layer, const char *buf) {
  text_layer_set_text(&layer->layer, buf);
  layer_mark_dirty(&layer->layer);
}

void korean_text_layer_render(struct Layer *layer, GContext *ctx, const uint8_t *str, uint8_t len, GPoint startPoint) {
  glyphType_t current = none;
  uint8_t code;
  int16_t xPos = startPoint.x, yPos = startPoint.y;
  for(uint16_t strPos = 0; strPos < len; ++strPos) {
    code = str[strPos];
    if (code <= '~') {
      code -= ' ';
      fontCursorOrigin->x = (code & 0x0F) * 9;
      fontCursorOrigin->y = (code >> 4) * 18 + 108;
      graphics_draw_bitmap_in_rect(ctx, &fontCursor, GRect(xPos, yPos, 9, 18));
      xPos += 9;
      continue;
    }
    uint8_t size = getGlyphWidth(code, &current);
    if (current == last) {
      xPos -= 9;
    } else if (current == middle_u) {
      xPos -= 7;
    }
    code -= '~' + '!';
    fontCursorOrigin->x = (code & 0x0F) * 9;
    fontCursorOrigin->y = (code >> 4) * 18;
    graphics_draw_bitmap_in_rect(ctx, &fontCursor, GRect(xPos, yPos, 9, 18));
    if (current == middle_u) {
      xPos += 7;
    } else if (current == last) {
      xPos += 9;
    } else {
      xPos += size;
    }
  }  
}

void korean_text_layer_update(struct Layer *layer, GContext *ctx) {
  int16_t xPos = 0, yPos = 0,
    width = layer_get_frame(layer).size.w,
    height = layer_get_frame(layer).size.h - 18, tmpPos;
  uint8_t tmp = 0;
  uint16_t strPos = 0, startPos = 0, searchPos;
  graphics_context_set_compositing_mode(ctx, GCompOpAssign);
  graphics_context_set_fill_color(ctx, GColorClear);
  graphics_fill_rect(ctx, layer_get_frame(layer), 0, GCornerNone);
  glyphType_t cur;
  graphics_context_set_compositing_mode(ctx, GCompOpAnd);
  const char *textBuf = text_layer_get_text((KoreanTextLayer *)layer)->layer);
  for(; yPos < height; ++strPos) {
    bool render = false;
    if (strPos > 256 || textBuf[strPos] == 0) {
      break;
    } else if (textBuf[strPos] == '\n') {
      render = true;
    } else {
      tmp += getGlyphWidth(textBuf[strPos], &cur);
    }
    if (xPos + tmp > width) {
      render = true;
    }
    if (render) {
      korean_text_layer_render(layer, ctx, textBuf + startPos, strPos - startPos, GPoint(xPos, yPos));
      startPos = strPos;
      if (textBuf[strPos] == '\n') {
        ++startPos;
      }
      tmp = 0;
      xPos = tmp;
      yPos += 18;
      continue;
    }
  }
  korean_text_layer_render(layer, ctx, textBuf + startPos, strPos - startPos, GPoint(xPos, yPos));
}

/*
  7	U+0000	U+007F	1	0xxxxxxx
11	U+0080	U+07FF	2	110xxxxx	10xxxxxx
16	U+0800	U+FFFF	3	1110xxxx	10xxxxxx	10xxxxxx
21	U+10000	U+1FFFFF	4	11110xxx	10xxxxxx	10xxxxxx	10xxxxxx
26	U+200000	U+3FFFFFF	5	111110xx	10xxxxxx	10xxxxxx	10xxxxxx	10xxxxxx
31	U+4000000	U+7FFFFFFF	6	1111110x	10xxxxxx	10xxxxxx	10xxxxxx	10xxxxxx	10xxxxxx
*/

uint32_t extractOneCharacterFromUTF8Str(const char **str) {
  uint32_t ch = 0;
  int8_t count = 0;
  for(; count < 8; ++count) {
    uint8_t tmp = 0x80 >> count;
    if ((**str & tmp) != tmp) {
      break;
    }
  }
  if (count == 1) {
    return 0;
  }
  ch = **str & (0x40 >> count);
  for(++*str;count > 0; --count, ++*str) {
    ch <<= 6;
    ch |= **str & 0x3F;    
  }
  return ch;
}

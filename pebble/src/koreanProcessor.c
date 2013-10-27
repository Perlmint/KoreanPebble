#include "koreanProcessor.h"

void getSplittedKoreanCharacter(uint32_t ch, char ret[5]) {
  char last = 0, middle = 0;
  uint8_t count = 0;
  if (ch >= 44032 && ch <= 55203) { // Korean Area
    ch -= 44032;
    last = ch % 28;
    if (last != 0) {
      last += 33;
    }
    ch = (ch / 28);
    middle = ch % 21 + 20;
    ret[count++] = ch / 21 + 1;
    switch(middle) {
      case 29: // ㅘ
        ret[count++] = 28;
        ret[count++] = 20;
        break;
      case 30: // ㅙ
        ret[count++] = 28;
        ret[count++] = 21;
        break;
      case 31: // ㅚ
        ret[count++] = 28;
        ret[count++] = 33;
        break;
      case 32: // ㅛ
        ret[count++] = 29;
        break;
      case 33: // ㅜ
        ret[count++] = 30;
        break;
      case 34: // ㅝ
        ret[count++] = 30;
        ret[count++] = 24;
        break;
      case 35: // ㅞ
        ret[count++] = 30;
        ret[count++] = 25;
        break;
      case 36: // ㅟ
        ret[count++] = 30;
        ret[count++] = 33;
        break;
      case 37: // ㅛ
        ret[count++] = 31;
        break;
      case 38: // ㅡ
        ret[count++] = 32;
        break;
      case 39: // ㅢ
        ret[count++] = 32;
        ret[count++] = 33;
        break;
      case 40: // ㅣ
        ret[count++] = 33;
        break;
      default:
        ret[count++] = middle;
        break;
    }
    if (count == 2) {
       ret[count++] = 0;
    }
    ret[count++] = last;
    ret[count] = 0;
  }
}

uint8_t getKoreanCharacterWidth(uint32_t ch) {
  char splited[5] = {0};
  uint8_t size = 9;
  getSplittedKoreanCharacter(ch, splited);
  for(uint8_t pos = 1; pos < 3; pos++) {
    if ((splited[pos] >= 28 && splited[pos] <= 32) || splited[pos] == 0) { // ㅗㅛㅜㅠㅡ
      size += 0;
    } else if (splited[pos] == 33 || splited[pos] % 2 == 0) { // ㅏㅑㅓㅕㅣ
      size += 5;
    } else { // ㅐㅒㅔㅖㅖ
      size += 7;
    }
  }
  return size;
}

void renderGlyph(GContext *ctx, GBitmap *fontCursor, GPoint *point, uint8_t code) {
  GPoint *fontCursorOrigin = &fontCursor->bounds.origin;

  if (code == 0) {
    return;
  }

  --code;
  fontCursorOrigin->x = (code & 0x0F) * 9;
  fontCursorOrigin->y = ((code >> 4) + 6) * 18;
  graphics_draw_bitmap_in_rect(ctx, fontCursor, GRect(point->x, point->y, 9, 18));
}

void renderFirst(GContext *ctx, GBitmap *fontCursor, GPoint *point, const char ch[1]) {
  // render it
  renderGlyph(ctx, fontCursor, point, ch[0]);
  point->x += 9;
}

void renderMiddle(GContext *ctx, GBitmap *fontCursor, GPoint *point, const char ch[2]) {
  uint8_t pos = 0;
  for(; ch[pos] != 0 && pos < 2; pos++) {
    if (ch[pos] >= 28 && ch[pos] <= 32) { // ㅗㅛㅜㅠㅡ
      point->x -= 9;
    }
    renderGlyph(ctx, fontCursor, point, ch[pos]);
    if (ch[pos] >= 28 && ch[pos] <= 32) { // ㅗㅛㅜㅠㅡ
      point->x += 9;
    } else if (ch[pos] == 33 || ch[pos] % 2 == 0) { // ㅏㅑㅓㅕㅣ
      point->x += 5;
    } else { // ㅐㅒㅔㅖㅖ
      point->x += 7;
    }
  }
}

void renderEnd(GContext *ctx, GBitmap *fontCursor, GPoint *point, const char ch[1]) {
  if (ch[0] == 0) {
    return;
  }
  // rewind position
  point->x -= 9;
  // render
  renderGlyph(ctx, fontCursor, point, ch[0]);
  // move forward
  point->x += 9;
}

void renderKoreanCharacter(GContext *ctx, GBitmap *fontCursor, GPoint *point, const uint32_t ch) {
  char splited[5] = {0};
  getSplittedKoreanCharacter(ch, splited);
  fontCursor->bounds.size.w = 9;
  renderFirst(ctx, fontCursor, point, splited);
  renderMiddle(ctx, fontCursor, point, splited + 1);
  renderEnd(ctx, fontCursor, point, splited + 3);
}
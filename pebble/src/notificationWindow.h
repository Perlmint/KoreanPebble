#pragma once

struct Window;

extern Window *notificationWindow;

typedef enum {
  nlt_title, nlt_body
} notificationLabelType_e;

void openNotificationWindow();
void setNotificationText(notificationLabelType_e type, const char *text);
void appendNotificationText(notificationLabelType_e type, const char *text);
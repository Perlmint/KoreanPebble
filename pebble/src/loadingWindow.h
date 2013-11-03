#pragma once

/**
 * Loading Window
 */

// Forward declaration
struct Window;
struct TextLayer;

// Extern
extern Window *loadingWindow;
extern TextLayer *loadingMessageLayer;

/**
 * Open The LoadingWindow
 * automatically initialize loadingWindow and push to window_stack
 */
void openLoadingWindow();

/**
 * Close The LoadingWindow
 * remove loadingWindow from window_stack when it is safe
 */
void closeLoadingWindow();
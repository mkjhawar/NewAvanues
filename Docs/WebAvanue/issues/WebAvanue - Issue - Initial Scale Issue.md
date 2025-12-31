# WebAvanue - Issue - Initial Scale Issue 

## Issue Summary

On **fresh install** or **cold restart**, WebView content was loading at an
incorrect scale (appearing zoomed out / overview-like), instead of the expected
mobile layout.

This was especially visible on pages like **google.com**, where the content
looked much smaller with excessive white space.

---

## Root Cause

The issue was caused by the **default value of `mobilePortraitScale`** in
`BrowserSettings`.

### What was happening

- `BrowserSettings.mobilePortraitScale` had a default value of **`100`**
- During WebView creation, `setInitialScale()` was called using this value
- `setInitialScale(100)` forces WebView to render at **100% zoom immediately**
- On cold start, this happened **before user settings were loaded**
- As a result, WebView always rendered the first page at **forced 100% scale**

This bypassed WebView’s natural auto-scaling behavior and caused incorrect
initial layout rendering.

Relevant code before fix:
```kotlin
val mobilePortraitScale: Float = 100f



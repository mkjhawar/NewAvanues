---
name: webapp-testing
description: Web application testing using Playwright. Use for frontend verification, UI debugging, screenshots, and browser automation.
---

# Web App Testing (Playwright)

## Setup
```bash
pip install playwright
playwright install  # Downloads browsers
```

## Basic Test
```python
from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()

    page.goto("http://localhost:3000")
    page.wait_for_load_state("networkidle")

    # Take screenshot
    page.screenshot(path="screenshot.png")

    browser.close()
```

## Selection Framework

| Scenario | Approach |
|----------|----------|
| Static HTML | Read file, extract selectors |
| Dynamic app (no server) | Launch with helper script |
| Running server | Navigate directly |

## Critical Rule
**Always wait for network idle on dynamic apps:**
```python
page.wait_for_load_state("networkidle")
```

## Selectors (Priority Order)

| Type | Example | When |
|------|---------|------|
| Text | `page.get_by_text("Submit")` | Visible text |
| Role | `page.get_by_role("button")` | Accessibility |
| Test ID | `page.get_by_test_id("login-btn")` | data-testid |
| CSS | `page.locator(".submit-btn")` | Fallback |

## Common Actions
```python
# Click
page.click("button#submit")
page.get_by_role("button", name="Submit").click()

# Fill input
page.fill("input[name='email']", "test@example.com")
page.get_by_label("Email").fill("test@example.com")

# Select dropdown
page.select_option("select#country", "US")

# Check checkbox
page.check("input[type='checkbox']")

# Wait for element
page.wait_for_selector(".results", state="visible")
```

## Assertions
```python
from playwright.sync_api import expect

expect(page.get_by_text("Success")).to_be_visible()
expect(page.locator(".error")).not_to_be_visible()
expect(page).to_have_title("Dashboard")
expect(page).to_have_url("http://localhost/dashboard")
```

## Screenshots
```python
# Full page
page.screenshot(path="full.png", full_page=True)

# Element only
page.locator(".chart").screenshot(path="chart.png")

# Viewport
page.set_viewport_size({"width": 1280, "height": 720})
page.screenshot(path="viewport.png")
```

## Console Logs
```python
page.on("console", lambda msg: print(f"[{msg.type}] {msg.text}"))
page.on("pageerror", lambda err: print(f"Error: {err}"))
```

## Network Interception
```python
def handle_route(route):
    if "api/data" in route.request.url:
        route.fulfill(json={"mocked": True})
    else:
        route.continue_()

page.route("**/*", handle_route)
```

## Multiple Pages
```python
# New tab
new_page = context.new_page()
new_page.goto("http://example.com")

# Popup handling
with page.expect_popup() as popup_info:
    page.click("a[target='_blank']")
popup = popup_info.value
```

## Form Testing Pattern
```python
# Navigate
page.goto("http://localhost:3000/signup")
page.wait_for_load_state("networkidle")

# Fill form
page.get_by_label("Email").fill("user@test.com")
page.get_by_label("Password").fill("secure123")

# Submit
page.get_by_role("button", name="Sign Up").click()

# Verify
page.wait_for_url("**/dashboard")
expect(page.get_by_text("Welcome")).to_be_visible()
```

## Debug Mode
```python
# Headed mode with slow-mo
browser = p.chromium.launch(headless=False, slow_mo=500)

# Pause for debugging
page.pause()  # Opens inspector
```

## Best Practices

| Practice | Reason |
|----------|--------|
| Wait for networkidle | Ensures dynamic content loaded |
| Use semantic selectors | More resilient to UI changes |
| Screenshot on failure | Debug visibility |
| Isolate tests | Independent, no shared state |

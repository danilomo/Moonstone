# Task: Capture Screenshots for Documentation

**Priority:** HIGH
**Effort:** Medium
**Status:** Pending

## Description

Capture high-quality screenshots and GIFs of Moonstone applications for the README and documentation.

## Why

- Visual presentation is critical for Hacker News launch
- Users want to see what the framework looks like before reading code
- Screenshots demonstrate the quality and polish of the framework

## Screenshots to Capture

### Static Screenshots (PNG)

| Filename | Sample | Description | Size |
|----------|--------|-------------|------|
| `counter.png` | `samples/counter/` | Button + number display | 800x600 |
| `todo.png` | `samples/todo/` | Task list with checkboxes | 800x600 |
| `navigation.png` | `samples/navigation/` | Tab switching demo | 800x600 |
| `new-components.png` | `samples/new-components/` | Component gallery | 800x600 |
| `dialogs.png` | `samples/dialogs/` | Modal dialog examples | 800x600 |
| `showcase.png` | `samples/showcase/` | Polished showcase app | 800x600 |

### Animated GIFs

| Filename | Sample | Description | Duration |
|----------|--------|-------------|----------|
| `counter.gif` | `samples/counter/` | Clicking increment button | 3-5 sec |
| `todo.gif` | `samples/todo/` | Adding and completing tasks | 5-8 sec |
| `hot-reload.gif` | any | Code change → instant UI update | 5-10 sec |

## How to Capture

### Running Samples

```bash
# Run each sample
./gradlew :desktop:run --args="samples/counter/app.scm"
./gradlew :desktop:run --args="samples/todo/app.scm"
./gradlew :desktop:run --args="samples/showcase/app.scm"
# etc.
```

### Screenshot Tools

- **Linux:** `gnome-screenshot`, `flameshot`, or `scrot`
- **macOS:** Cmd+Shift+4 or Screenshot.app
- **Windows:** Snipping Tool or Win+Shift+S

### GIF Recording Tools

- **Linux:** `peek`, `gifski`, or `byzanz`
- **macOS:** `gifski`, `Kap`, or `LICEcap`
- **Windows:** `ScreenToGif` or `LICEcap`

### Image Specifications

- **Resolution:** 800x600 or 1200x900 (2x for retina)
- **Format:** PNG for static, GIF for animated
- **Max size:** 500KB per image (optimize with `optipng` or `pngquant`)
- **Location:** Save to `docs/images/`

## Update README

After capturing screenshots, update `README.md` to display them:

```markdown
## Screenshots

<p align="center">
  <img src="docs/images/counter.gif" width="300" alt="Counter"/>
  <img src="docs/images/todo.png" width="300" alt="Todo"/>
  <img src="docs/images/showcase.png" width="300" alt="Showcase"/>
</p>
```

Add this section after the "Features" section in README.md.

## Acceptance Criteria

- [ ] All 6 static screenshots captured
- [ ] At least 2 animated GIFs created
- [ ] All images under 500KB each
- [ ] Images saved to `docs/images/`
- [ ] README updated to display screenshots
- [ ] Screenshots render correctly on GitHub

## Tips

- Use a clean desktop background
- Ensure window is focused and not cut off
- Show the app in a good state (data populated, not empty)
- For hot-reload GIF, show VS Code or editor alongside the app window

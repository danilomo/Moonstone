# Moonstone - Claude Context

## Overview

Scheme-based declarative UI framework on Jetpack Compose Multiplatform. Write UI apps in Scheme (KleinLisp) that render to native Desktop and Android.

**Repository:** `Moonstone/` (GUI framework) + `../KleinLisp/` (Scheme interpreter, auto-recompiles via composite build)

## Architecture

```
Scheme Script  →  MoonstoneRuntime  →  UIElement Tree  →  UIRenderer (Compose)
                  (Lisp + State + Components)
```

## Quick Start

```scheme
(define (app)
  (column #:spacing 16 #:padding 16
    (text #:value "Hello, Moonstone!")))
```

Run: `./gradlew :desktop:run --args="path/to/app.scm"`

## Critical KleinLisp Notes

For a full language reference (booleans, comments, strings, available procedures, missing features), see **`docs/api-reference.md` → [KleinLisp Language Reference]**.

Quick reminders:

- **Booleans:** `#t` / `#f` only — `true`/`false` are identifiers, not literals
- **Comments:** `;` line comments only — `#| |#` block and `#; datum` comments are NOT supported
- **Strings:** `string=?` for equality, `string-contains?` for substrings; `=` is for numbers only
- **Debugging:** `(println "message")` / `(print value)` / `display` (alias for println)
- **TCO:** Tail-call optimization is supported — tail-recursive functions are safe
- **Macros:** `define-syntax` / `syntax-rules` work; no `defmacro`
- **No `call/cc`:** continuations are not implemented
- **No nested named-let:** KleinLisp crashes when an inner named-let references outer loop variables. Always extract inner loops to top-level functions (see `samples/tetris/app.scm` for the pattern)
- **Integers are 32-bit:** literal integers must fit in `[-2147483648, 2147483647]`; use smaller constants in math-heavy code

**State basics:**
```scheme
(define count (state 0))               ; create state
(state-ref count)                      ; read
(state-set! count 5)                   ; write
(state-update! count (lambda (n) (+ n 1)))  ; update
(derived (lambda () ...))              ; computed state (auto-updates)
```

**Entry points:** Define one of: `app`, `main`, `my-app`, `root`

**Game state pattern** (one recomposition per tick, not one per variable):
```scheme
(define pos-x 100)           ; mutable game vars — NOT state cells
(define pos-y 100)
(define render-tick (state 0))  ; single reactive trigger
(define (refresh!)              ; call once at the end of each logical update
  (state-set! render-tick (+ (state-ref render-tick) 1)))
```

**Gamepad input:**
```scheme
(on-key-down               ; register at top-level, not inside app
  (lambda (key)
    (cond
      ((string=? key "dpad-left")  ...)
      ((string=? key "a")          ...))))
```

## Documentation Map

**📚 New User?** → `docs/getting-started.md` (tutorial walkthrough)
**🎨 Building UI?** → `docs/component-reference.md` (all components with all props)
**⚡ State & APIs?** → `docs/api-reference.md` (state, derived, platform functions, input events)
**🔤 KleinLisp language?** → `docs/api-reference.md#kleinlisp-language-reference` (booleans, strings, TCO, what's missing)
**🗄️ Using Database?** → `docs/orm-reference.md` (complete ORM API)
**🏗️ Building Real Apps?** → `docs/real-apps-guide.md` (DB patterns, forms, navigation, error handling)
**🎮 Building Games?** → `docs/game-guide.md` (game-canvas, draw primitives, game loop, Tetris walkthrough)

## Key Locations

**Components:** `core/src/commonMain/kotlin/net/sourceforge/moonstone/components/impl/`
**Samples:** `samples/` (counter, todo, navigation, dialogs, database-crud, tetris, gamepad-test)
**Desktop entry:** `desktop/src/main/kotlin/.../Main.kt`
**Android entry:** `android/src/main/kotlin/.../AppActivity.kt`
**Android functions:** `android/src/main/kotlin/.../AndroidExtensions.kt` (toast, vibrate, etc.)
**Game components:** `GameCanvasComponent`, `RectDrawComponent`, `CircleDrawComponent`, `LineDrawComponent` in `components/impl/`
**Input events:** `MoonstoneRuntime.dispatchKeyDown` → `on-key-down` Scheme handler

## Configuration

Optional `app.conf` in app folder:
```ini
[app]
name = My App
window-width = 800
db-location = data.db
```
Keys become `*key-name*` variables in Lisp (e.g., `*window-width*`)

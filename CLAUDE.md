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

**State basics:**
```scheme
(define count (state 0))               ; create state
(state-ref count)                      ; read
(state-set! count 5)                   ; write
(state-update! count (lambda (n) (+ n 1)))  ; update
(derived (lambda () ...))              ; computed state (auto-updates)
```

**Entry points:** Define one of: `app`, `main`, `my-app`, `root`

## Documentation Map

**📚 New User?** → `docs/getting-started.md` (tutorial walkthrough)
**🎨 Building UI?** → `docs/component-reference.md` (35 components with all props)
**⚡ State & APIs?** → `docs/api-reference.md` (state, derived, platform functions)
**🔤 KleinLisp language?** → `docs/api-reference.md#kleinlisp-language-reference` (booleans, strings, TCO, what's missing)
**🗄️ Using Database?** → `docs/orm-reference.md` (complete ORM API)
**🏗️ Building Real Apps?** → `docs/real-apps-guide.md` (DB patterns, forms, navigation, error handling)

## Key Locations

**Components:** `core/src/commonMain/kotlin/net/sourceforge/moonstone/components/impl/`
**Samples:** `samples/` (counter, todo, navigation, dialogs, database-crud, new-components)
**Desktop entry:** `desktop/src/main/kotlin/.../Main.kt`
**Android entry:** `android/src/main/kotlin/.../AppActivity.kt`
**Android functions:** `android/src/main/kotlin/.../AndroidExtensions.kt` (toast, vibrate, etc.)

## Configuration

Optional `app.conf` in app folder:
```ini
[app]
name = My App
window-width = 800
db-location = data.db
```
Keys become `*key-name*` variables in Lisp (e.g., `*window-width*`)

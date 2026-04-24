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

**Booleans:** Use `#t` and `#f` (not `true`/`false`)

**No semicolon comments:** KleinLisp does not support `;` comments

**String comparison:** Use `string=?` (not `=`) and `string-contains?` for substrings

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
**🗄️ Using Database?** → `docs/orm-reference.md` (complete ORM API)

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

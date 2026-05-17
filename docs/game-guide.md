# 2D Game Guide

This guide covers everything you need to build 2D games in Moonstone. Moonstone's reactive model maps cleanly onto game development: draw primitives replace layout components, a game-loop callback replaces lifecycle hooks, and the same `set!`-plus-state-trigger pattern handles all game state.

## Table of Contents

- [Core Concepts](#core-concepts)
- [game-canvas](#game-canvas)
- [Draw Primitives](#draw-primitives)
- [Game Loop](#game-loop)
- [State Management for Games](#state-management-for-games)
- [Input Handling](#input-handling)
- [Worked Example: Bouncing Ball](#worked-example-bouncing-ball)
- [Tetris Sample](#tetris-sample)
- [Performance Notes](#performance-notes)

---

## Core Concepts

A Moonstone game follows this cycle every tick:

```
tick! called → mutate game state (set!) → refresh! → app re-evaluated → new draw list → canvas redraws
```

The key differences from a normal Moonstone app:

| Normal App | Game |
|------------|------|
| `state` cells for each piece of UI state | Mutable vars (`set!`) for game state + one `state` trigger |
| App re-evaluates on every `state-set!` | App re-evaluates once per logical update via one `refresh!` |
| Children are layout components | Children are draw primitives (`rect`, `circle`, `line`) |
| No time loop | `#:on-tick` drives a game loop at a fixed interval |

---

## game-canvas

`game-canvas` is a fixed-size drawing surface that optionally runs a game loop.

```scheme
(game-canvas
  #:width  300
  #:height 400
  #:background "#111111"
  #:on-tick       my-tick-fn   ; called every tick-interval ms
  #:tick-interval 50           ; ms between ticks (default 16)
  (draw-element-1)
  (draw-element-2)
  (list-of-draw-elements))     ; Scheme lists are flattened into children
```

**Props:**

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `#:width` | number | — | Canvas width in dp (required) |
| `#:height` | number | — | Canvas height in dp (required) |
| `#:background` | color | transparent | Background fill color |
| `#:on-tick` | function | — | Called every `tick-interval` ms |
| `#:tick-interval` | number | `16` | Milliseconds between ticks (~60 fps at 16) |

**Children** are draw primitives (`rect`, `circle`, `line`). They are drawn in order — last child is on top. You can also pass a Scheme list as a positional argument; all elements from the list are flattened into the canvas's draw list:

```scheme
(game-canvas #:width 240 #:height 480 #:background "#000"
  (board-cells)      ; returns a list of rect elements
  (piece-cells)      ; returns a list of rect elements
  (ghost-cells))     ; returns a list of rect elements
```

The game loop coroutine starts when the canvas is first composed and stops when it leaves the tree. The `#:on-tick` function reference is kept fresh on every recomposition without restarting the loop, so it always calls the current version of the function.

---

## Draw Primitives

Draw primitives are components that only render inside a `game-canvas`. Outside a canvas they are no-ops.

### rect

A filled rectangle.

```scheme
(rect #:x 10 #:y 20 #:width 30 #:height 30 #:color "#FF5722")
```

| Prop | Type | Description |
|------|------|-------------|
| `#:x` | number | Left edge in dp |
| `#:y` | number | Top edge in dp |
| `#:width` | number | Width in dp |
| `#:height` | number | Height in dp |
| `#:color` | color | Fill color |

### circle

A filled circle.

```scheme
(circle #:cx 50 #:cy 50 #:radius 20 #:color "#00BCD4")
```

| Prop | Type | Description |
|------|------|-------------|
| `#:cx` | number | Center x in dp |
| `#:cy` | number | Center y in dp |
| `#:radius` | number | Radius in dp |
| `#:color` | color | Fill color |

### line

A line segment.

```scheme
(line #:x1 0 #:y1 0 #:x2 100 #:y2 100 #:color "#FFFFFF" #:stroke-width 2)
```

| Prop | Type | Description |
|------|------|-------------|
| `#:x1` | number | Start x in dp |
| `#:y1` | number | Start y in dp |
| `#:x2` | number | End x in dp |
| `#:y2` | number | End y in dp |
| `#:color` | color | Line color |
| `#:stroke-width` | number | Line thickness in dp (default 1) |

---

## Game Loop

The `#:on-tick` callback is called every `#:tick-interval` milliseconds. It should:

1. Update game state via `set!`
2. Call `refresh!` once to trigger a single recomposition

```scheme
(define (tick!)
  ; --- update game state ---
  (set! ball-x (+ ball-x vx))
  (set! ball-y (+ ball-y vy))
  ; handle wall bounces, collisions, etc.
  ; --- trigger one redraw ---
  (refresh!))
```

Calling `refresh!` more than once per tick wastes recompositions. Group all mutations and call `refresh!` at the end.

**Choosing `tick-interval`:**
- `16` ms → ~60 fps (smooth animation)
- `33` ms → ~30 fps (lighter; fine for grid-based games)
- `50` ms → 20 fps (typical for turn-based or slow-paced games)

---

## State Management for Games

The standard Moonstone reactive pattern (one `state` cell per value) triggers a recomposition on every `state-set!`. For games updating 10–200 values per tick, that would be 10–200 recompositions per tick.

The game pattern uses **mutable variables** (`set!`) for everything, plus a **single render trigger** state cell that gets incremented once per tick:

```scheme
; --- game state: mutable, not reactive ---
(define pos-x 100)
(define pos-y 100)
(define vel-x  2)
(define vel-y  3)

; --- one reactive cell: incrementing it triggers exactly one recomposition ---
(define render-tick (state 0))

(define (refresh!)
  (state-set! render-tick (+ (state-ref render-tick) 1)))
```

The `app` function reads the mutable variables directly. Because `reEvaluateApp` runs synchronously when `state-set!` is called, all mutations done before `refresh!` are visible to `app`.

**Important:** Avoid nested named-let expressions in functions called from `app`. KleinLisp has a known limitation where the inner letrec environment can see outer loop variables as uninitialized cells, causing a crash. Extract inner loops to top-level functions instead:

```scheme
; BAD — inner named let sees outer variable 'rows' as uninitialized in some cases
(define (board-rects)
  (let row-loop ((rows board) ...)
    (let col-loop ((cells (car rows)) ...)  ; may crash
      ...)))

; GOOD — separate top-level function
(define (row-rects cells c r acc) ...)  ; top-level helper

(define (board-rects)
  (let loop ((rows board) ...)
    ... (row-rects (car rows) ...) ...))
```

---

## Input Handling

Use `on-key-down` to respond to gamepad, keyboard, and d-pad input. Register the handler once at the top level (outside `app`):

```scheme
(on-key-down
  (lambda (key)
    (cond
      ((string=? key "dpad-left")  (move-left!))
      ((string=? key "dpad-right") (move-right!))
      ((string=? key "a")          (jump!))
      ((string=? key "start")      (toggle-pause!)))))
```

Each key event is independent — there is no `on-key-up`. For "held button" behaviour (e.g. soft drop in Tetris), use an auto-expiring flag: the handler sets a flag and a timer; the tick function ages the timer and clears the flag when it expires. If the button is held, the gamepad's auto-repeat keeps refreshing the timer.

**Available key names** (tested with standard gamepads):

| Key | Description |
|-----|-------------|
| `dpad-up`, `dpad-down`, `dpad-left`, `dpad-right` | D-pad directions |
| `a`, `b`, `x`, `y` | Face buttons (SNES layout) |
| `l1`, `r1` | Shoulder buttons |
| `l2`, `r2` | Trigger buttons |
| `start`, `select` | Center buttons |

See `samples/gamepad-test/app.scm` for an interactive button tester that lights up every button as you press it.

---

## Worked Example: Bouncing Ball

A complete, minimal game showing the full pattern:

```scheme
; ===== STATE =====

(define W 300)
(define H 400)
(define R 12)      ; ball radius

(define bx 150)    ; position
(define by 100)
(define vx  3)     ; velocity
(define vy  2)

(define render-tick (state 0))

(define (refresh!)
  (state-set! render-tick (+ (state-ref render-tick) 1)))

; ===== GAME LOOP =====

(define (tick!)
  (set! bx (+ bx vx))
  (set! by (+ by vy))
  ; bounce off walls
  (when (or (< (- bx R) 0) (> (+ bx R) W))
    (set! vx (- vx)))
  (when (or (< (- by R) 0) (> (+ by R) H))
    (set! vy (- vy)))
  (refresh!))

; ===== INPUT =====

(on-key-down
  (lambda (key)
    (cond
      ((string=? key "a")     (set! vy (- vy 1)))   ; flick up
      ((string=? key "b")     (set! vy (+ vy 1)))   ; flick down
      ((string=? key "start") (set! vx 3) (set! vy 2) (set! bx 150) (set! by 100)))))

; ===== RENDERING =====

(define (app)
  (game-canvas
    #:width  W
    #:height H
    #:background "#1A1A2E"
    #:on-tick tick!
    #:tick-interval 16
    (list (circle #:cx bx #:cy by #:radius R #:color "#00BCD4"))))
```

Key observations:
- No layout wrapper needed — `game-canvas` is the entire `app`.
- `(list ...)` creates a single-element list; the canvas flattens it.
- Input changes velocity directly; `tick!` drives movement every 16 ms.
- Reset on `start`.

---

## Tetris Sample

A fully playable Tetris implementation is in `samples/tetris/`. It demonstrates:

- **Colored blocks** — 7 tetrominos (I/O/T/S/Z/J/L), each a distinct color
- **Ghost piece** — dim shadow showing where the piece lands
- **Score, level, and lines** — displayed beside the board
- **Next piece preview** — second `game-canvas` in the side panel
- **Soft drop** — `↓` increases falling speed (auto-expires, works with gamepad hold/autorepeat)
- **Hard drop** — `↑` instantly slams the piece to the bottom
- **Rotation** — `A` clockwise, `B` counter-clockwise
- **Pause / restart** — `START`

```bash
./gradlew :desktop:run --args="samples/tetris/app.scm"
```

The Tetris source is worth reading for real-world patterns:
- Functional board updates with `list-update` (immutable list, mutable binding)
- `filter` + `append` for line clearing
- `effective-gravity` for soft drop speed boost
- Game over detection in `spawn-piece!`

---

## Performance Notes

- **Draw call count**: Each `rect`/`circle`/`line` is a Kotlin object created per frame. A 10×20 Tetris board produces ~204 objects per tick (200 cells + 4 piece + ghost). This is fast on JVM but keep it in mind for very dense scenes.
- **Recomposition cost**: `refresh!` triggers `reEvaluateApp` synchronously before returning. For 50 ms tick intervals the budget is comfortable; at 16 ms keep the app function and drawing helpers lean.
- **`set!` over `state`**: Never put individual game values in `state` cells if they all change together each tick — each `state-set!` would trigger a separate recomposition. Use `set!` for everything and one `state` trigger.

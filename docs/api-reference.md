# API Reference

This document covers the core Scheme functions and conventions in Moonstone.

## Table of Contents

- [State Management](#state-management)
- [Platform Utilities](#platform-utilities)
- [Entry Points](#entry-points)
- [Type Conventions](#type-conventions)
- [Color Formats](#color-formats)
- [Typography Styles](#typography-styles)
- [Arrangement and Alignment](#arrangement-and-alignment)
- [Icon Names](#icon-names)

---

## State Management

Moonstone uses reactive state cells for managing application state. When state changes, any UI components using that state automatically update.

### state

Creates a new state cell with an initial value.

```scheme
(state initial-value) -> state-cell
```

**Parameters:**
- `initial-value` - The initial value (any type)

**Returns:** A state cell object

**Example:**

```scheme
(define counter (state 0))
(define name (state ""))
(define items (state '()))
```

---

### state-ref

Reads the current value from a state cell.

```scheme
(state-ref cell) -> value
```

**Parameters:**
- `cell` - A state cell created with `state`

**Returns:** The current value

**Example:**

```scheme
(define count (state 10))
(state-ref count)  ; Returns 10
```

**Note:** In most cases, you can pass the state cell directly to a component's `#:value` prop, and it will automatically read and react to changes.

---

### state-set!

Sets a new value in a state cell.

```scheme
(state-set! cell new-value) -> void
```

**Parameters:**
- `cell` - A state cell
- `new-value` - The new value to set

**Example:**

```scheme
(define name (state ""))
(state-set! name "Alice")
```

---

### state-update!

Updates a state cell by applying a function to its current value.

```scheme
(state-update! cell update-fn) -> void
```

**Parameters:**
- `cell` - A state cell
- `update-fn` - A function that takes the current value and returns a new value

**Example:**

```scheme
(define count (state 0))

; Increment
(state-update! count (lambda (x) (+ x 1)))

; Double
(state-update! count (lambda (x) (* x 2)))

; Toggle boolean
(define enabled (state #f))
(state-update! enabled (lambda (x) (not x)))
```

---

## Platform Utilities

### platform

Returns the current platform.

```scheme
(platform) -> platform-object
```

**Returns:** A platform object representing the current runtime environment.

---

### platform?

Checks if running on a specific platform.

```scheme
(platform? platform-name) -> boolean
```

**Parameters:**
- `platform-name` - A symbol or string: `'android`, `'desktop`, `'desktop-jvm`, `'jvm`

**Returns:** The platform name if matched, otherwise false

**Example:**

```scheme
(if (platform? 'android)
    (android-specific-ui)
    (desktop-specific-ui))
```

---

## Entry Points

Your script must define one of the following functions as the application entry point:

| Function Name | Priority |
|---------------|----------|
| `app` | 1 (recommended) |
| `main` | 2 |
| `my-app` | 3 |
| `root` | 4 |

The entry point function should take no arguments and return a UI element.

**Example:**

```scheme
(define (app)
  (box
   #:fill-max-size #t
   #:content-alignment 'center
   (text #:value "Hello, World!")))
```

---

## Type Conventions

### Booleans

KleinLisp supports `#t` and `#f` boolean literals:

| Boolean | Literal |
|---------|---------|
| true | `#t` |
| false | `#f` |

**Example:**

```scheme
(button #:enabled #t ...)
(checkbox #:checked #f ...)
```

### Numbers

Numbers can be integers or floating-point:

```scheme
#:padding 16
#:width 100.5
#:elevation 4
```

### Strings

Strings are enclosed in double quotes:

```scheme
#:value "Hello, World!"
#:label "Enter your name"
```

### Symbols

Symbols are prefixed with a single quote:

```scheme
#:style 'filled
#:color 'blue
#:alignment 'center
```

### Lists

Lists use Scheme syntax:

```scheme
'(1 2 3 4 5)
'("a" "b" "c")
```

---

## Color Formats

Colors can be specified in several ways:

### Named Colors

```scheme
#:color 'red
#:color 'blue
#:background 'white
```

**Available named colors:**
- `'red`
- `'green`
- `'blue`
- `'white`
- `'black`
- `'gray` / `'grey`
- `'cyan`
- `'magenta`
- `'yellow`
- `'transparent`

### Hex Colors

```scheme
#:color "#FF5722"        ; RGB
#:color "#80FF5722"      ; ARGB (with alpha)
```

**Format:**
- `#RRGGBB` - 6-digit hex (opaque)
- `#AARRGGBB` - 8-digit hex (with alpha, 00 = transparent, FF = opaque)

---

## Typography Styles

Text components support Material Design 3 typography styles:

### Display

For large, prominent text:

```scheme
#:style 'display-large   ; 57sp
#:style 'display-medium  ; 45sp
#:style 'display-small   ; 36sp
```

### Headline

For section headers:

```scheme
#:style 'headline-large  ; 32sp
#:style 'headline-medium ; 28sp
#:style 'headline-small  ; 24sp
```

### Title

For titles and important labels:

```scheme
#:style 'title-large     ; 22sp
#:style 'title-medium    ; 16sp
#:style 'title-small     ; 14sp
```

### Body

For body text and paragraphs:

```scheme
#:style 'body-large      ; 16sp
#:style 'body-medium     ; 14sp
#:style 'body-small      ; 12sp
```

### Label

For small labels and captions:

```scheme
#:style 'label-large     ; 14sp
#:style 'label-medium    ; 12sp
#:style 'label-small     ; 11sp
```

### Font Weights

```scheme
#:font-weight 'thin
#:font-weight 'light
#:font-weight 'normal
#:font-weight 'medium
#:font-weight 'semi-bold
#:font-weight 'bold
#:font-weight 'extra-bold
```

---

## Arrangement and Alignment

### Vertical Arrangement (Column)

```scheme
#:vertical-arrangement 'top           ; Align to top
#:vertical-arrangement 'center        ; Center vertically
#:vertical-arrangement 'bottom        ; Align to bottom
#:vertical-arrangement 'space-between ; Even space, first at top, last at bottom
#:vertical-arrangement 'space-around  ; Even space around each item
#:vertical-arrangement 'space-evenly  ; Completely even spacing
```

### Horizontal Arrangement (Row)

```scheme
#:horizontal-arrangement 'start         ; Align to start (left in LTR)
#:horizontal-arrangement 'center        ; Center horizontally
#:horizontal-arrangement 'end           ; Align to end (right in LTR)
#:horizontal-arrangement 'space-between
#:horizontal-arrangement 'space-around
#:horizontal-arrangement 'space-evenly
```

### Horizontal Alignment (Column)

```scheme
#:horizontal-alignment 'start   ; Align children to start
#:horizontal-alignment 'center  ; Center children
#:horizontal-alignment 'end     ; Align children to end
```

### Vertical Alignment (Row)

```scheme
#:vertical-alignment 'top      ; Align to top
#:vertical-alignment 'center   ; Center vertically
#:vertical-alignment 'bottom   ; Align to bottom
```

### Content Alignment (Box)

```scheme
#:content-alignment 'center
#:content-alignment 'top-start
#:content-alignment 'top-center
#:content-alignment 'top-end
#:content-alignment 'center-start
#:content-alignment 'center-end
#:content-alignment 'bottom-start
#:content-alignment 'bottom-center
#:content-alignment 'bottom-end
```

---

## Icon Names

Available Material Design icons:

### Navigation

```scheme
"home"
"menu"
"arrow-back"
"arrow-forward"
"close"
"more-vert"
```

### Actions

```scheme
"search"
"add"
"delete"
"edit"
"refresh"
"share"
"done"
"check"
```

### Social

```scheme
"person"
"favorite"
"star"
```

### Communication

```scheme
"email"
"phone"
"notifications"
```

### Content

```scheme
"settings"
"info"
"warning"
"lock"
"visibility"
"visibility-off"
```

**Example:**

```scheme
(icon #:name "favorite" #:size 24 #:tint 'red)
```

---

## Error Messages

Moonstone provides detailed error messages to help debug issues:

### Missing Required Property

```
Missing required property 'value' for component 'text'.
Available properties: value, style, color, font-size, font-weight, max-lines, padding, ...
```

### Unknown Component

```
Unknown component: 'buttn'.
Available components: alert-dialog, bottom-navigation, bottom-sheet, box, button, ...
```

### State Errors

```
State error in 'state-ref': Expected a state cell, got: StringObject
Hint: Create a state cell first with (define my-state (state "initial-value")), then use (state-ref my-state)
```

### Script Load Errors

```
Failed to load script 'app.scm': Unrecognizable token at line 5
```

---

## Debug Mode

When running with `--debug`, additional features are available:

### Reload Button
Manually reload the current script.

### Inspector Panel
View the component tree and inspect properties.

### Error Display
See detailed error messages in the debug panel.

### Hot Reload
When running with `--hot-reload` or `-w`, the script automatically reloads when saved.

**CLI Usage:**

```bash
# Debug mode only
./gradlew :desktop:run --args="--debug app.scm"

# Hot reload only
./gradlew :desktop:run --args="--hot-reload app.scm"

# Both
./gradlew :desktop:run --args="-d -w app.scm"
```

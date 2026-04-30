# API Reference

This document covers the core Scheme functions and conventions in Moonstone.

## Table of Contents

- [KleinLisp Language Reference](#kleinlisp-language-reference)
- [State Management](#state-management)
- [Lifecycle Hooks](#lifecycle-hooks)
- [Platform Utilities](#platform-utilities)
- [Entry Points](#entry-points)
- [Type Conventions](#type-conventions)
- [Color Formats](#color-formats)
- [Typography Styles](#typography-styles)
- [Arrangement and Alignment](#arrangement-and-alignment)
- [Icon Names](#icon-names)

---

## KleinLisp Language Reference

KleinLisp is a Scheme dialect with broad R7RS coverage but a few important differences. This section is a quick-scan reference for developers coming from Racket, Guile, MIT Scheme, or SICP examples.

### Boolean Literals

Only `#t` and `#f` are boolean literals. `true` and `false` parse as ordinary identifiers — they will not cause a syntax error, but they will throw an "unbound variable" error at runtime.

```scheme
#t   ; true
#f   ; false

; Workaround if you want the names:
(define true #t)
(define false #f)
```

---

### Comments

Single-line comments work with `;` (standard Scheme).

```scheme
; This is a comment
;; Double semicolons also work
```

**Not supported:** Block comments (`#| ... |#`) and datum comments (`#; expr`) are not recognized by the lexer — they will cause a syntax error.

---

### String Operations

```scheme
; Equality — use string=?, not =
(string=? "hello" "hello")   ; #t
(string<? "a" "b")           ; #t
(string>? "b" "a")           ; #t

; Substring check (KleinLisp extension)
(string-contains? "hello world" "world")  ; #t
(string-prefix? "hello" "he")             ; #t
(string-suffix? "hello" "lo")             ; #t

; Other string utilities
(string-upcase "hello")           ; "HELLO"
(string-downcase "HELLO")         ; "hello"
(string-length "hello")           ; 5
(string-append "hello" " " "world") ; "hello world"
(substring "hello" 1 3)           ; "el"
(string-split "a,b,c" ",")        ; ("a" "b" "c")
(string-join '("a" "b" "c") ",")  ; "a,b,c"
(string-trim "  hello  ")         ; "hello"
(string-replace "hello" "l" "r")  ; "herro"
```

---

### Null / Void Handling

```scheme
; Empty list (nil)
'()
(null? '())   ; #t

; DB null values use db-null?
(if (db-null? val)
    "(empty)"
    (number->string val))

; void is returned by state-set!, for-each, and other side-effect functions
; Do not use void values in conditional expressions
```

---

### Available Numeric Operations

All standard arithmetic is available:

```scheme
; Arithmetic
+  -  *  /

; Comparison
=  <  >  <=  >=

; Conversion
(number->string 42)        ; "42"
(string->number "3.14")    ; 3.14
(string->number "bad")     ; #f — returns #f on failure, not an error

; Math functions
floor  ceiling  round  truncate
abs  min  max
modulo  remainder  quotient
expt  sqrt  exp  log
sin  cos  tan
```

---

### Available String Operations

```scheme
; Comparison (variadic — R7RS compliant)
string=?  string<?  string>?  string<=?  string>=?
string-ci=?  string-ci<?  string-ci>?  string-ci<=?  string-ci>=?

; Manipulation
string-length  string-append  substring  string-ref
string-upcase  string-downcase  string-foldcase
string-split   string-join    string-trim
string-replace string-prefix? string-suffix?

; Conversion
string->number  number->string
string->list    list->string
string->symbol  symbol->string

; KleinLisp extensions
string-contains?   ; (string-contains? haystack needle)

; Higher-order
string-map         ; (string-map proc string ...)
string-for-each    ; (string-for-each proc string ...)
```

---

### Available List Operations

```scheme
; Construction and access
car  cdr  cons  list  list-ref  list-tail

; Predicates
null?  pair?  list?

; Utilities
length  append  reverse

; Higher-order
map  filter  for-each
```

---

### Control Flow

All standard control forms are supported, including the `=>` arrow clause in `cond`:

```scheme
; if, cond, case, when, unless, begin, and, or
(if condition then else)

; cond with => arrow clause (passes test result to procedure)
(cond
  ((assv x alist) => cdr)   ; calls (cdr result-of-assv)
  (else #f))

; do loop
(do ((i 0 (+ i 1)))
    ((= i 5))
  (println i))
```

---

### Let Forms

All standard let variants are available:

```scheme
let  let*  letrec  letrec*
let-values  let*-values   ; for multiple return values (values)
```

Named `let` for tail-recursive loops is also supported:

```scheme
(let loop ((i 0))
  (when (< i 5)
    (println i)
    (loop (+ i 1))))
```

---

### Tail-Call Optimization

TCO is fully implemented. Tail-recursive functions will not stack-overflow regardless of iteration depth:

```scheme
; This will run for any n without a stack overflow
(define (count-down n)
  (if (= n 0)
      'done
      (count-down (- n 1))))

(count-down 1000000)
```

---

### Macros

`define-syntax` with `syntax-rules` is supported:

```scheme
(define-syntax swap!
  (syntax-rules ()
    ((swap! a b)
     (let ((tmp a))
       (set! a b)
       (set! b tmp)))))
```

`let-syntax` and `letrec-syntax` are also available for locally scoped macros.

---

### Persistent Collections (KleinLisp Extension)

KleinLisp includes immutable persistent data structures (backed by PCollections):

```scheme
; Persistent vector (immutable, structural sharing)
#v[1 2 3]
#vec[1 2 3]

; Persistent map (immutable hash map)
#m{"key" "value" "other" 42}
#map{"key" "value"}

; Persistent set (immutable)
#s{1 2 3}
#set{1 2 3}
```

---

### I/O

```scheme
(println "message")   ; print with newline
(print value)         ; print without newline
(display "text")      ; alias for println
(newline)             ; print a blank line
```

---

### What Is NOT in KleinLisp

| Feature | Status |
|---------|--------|
| `call/cc` / `call-with-current-continuation` | Not implemented |
| Block comments `#\| ... \|#` | Not supported by lexer |
| Datum comments `#; expr` | Not supported by lexer |
| `defmacro` | Not supported (use `define-syntax`) |
| Module system / `import` | Partial — `define-library` and `import` parse but all definitions are global |

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

### derived

Creates a derived state cell that automatically computes its value from other state cells. The computation function is re-evaluated whenever any dependencies change.

```scheme
(derived computation-fn) -> derived-state-cell
```

**Parameters:**
- `computation-fn` - A function that computes the derived value (typically reads from other state cells)

**Returns:** A derived state cell that automatically updates when dependencies change

**Example:**

```scheme
(define first-name (state "John"))
(define last-name (state "Doe"))

; Derived state automatically updates when first-name or last-name changes
(define full-name
  (derived (lambda ()
    (string-append (state-ref first-name) " " (state-ref last-name)))))

(text #:value full-name)  ; Displays "John Doe"

(state-set! first-name "Jane")
; full-name automatically becomes "Jane Doe"
```

**Common Use Cases:**

```scheme
; Computed count
(define todos (state (list "Task 1" "Task 2")))
(define todo-count
  (derived (lambda ()
    (length (state-ref todos)))))

; Formatted display
(define slider-value (state 50))
(define slider-label
  (derived (lambda ()
    (string-append "Value: " (number->string (state-ref slider-value))))))

; Filtered data
(define items (state (list 1 2 3 4 5)))
(define even-items
  (derived (lambda ()
    (filter even? (state-ref items)))))

; Validation
(define email (state ""))
(define email-valid
  (derived (lambda ()
    (string-contains? (state-ref email) "@"))))
```

**Note:** Derived state cells are read-only. Use `state-ref` to read their value, but you cannot use `state-set!` or `state-update!` on them. To change a derived value, update the underlying state cells it depends on.

---

## Lifecycle Hooks

Moonstone is built on Jetpack Compose, which re-runs composable functions whenever state changes (recomposition). The `app` function is no exception — it runs on every recompose. This means **any side effect placed directly in `app` (DB queries, network calls, file I/O) will run repeatedly**, causing infinite loops or data corruption.

Use scaffold's lifecycle hooks to run code at the right time. See [App Initialization](getting-started.md#app-initialization) in the Getting Started guide for the full worked example.

### `#:on-start`

Runs **once**, when the scaffold first appears. Use for initialization: loading data, creating database tables, fetching remote config.

Implemented with `LaunchedEffect(Unit)` — guaranteed to fire exactly once per scaffold lifetime.

```scheme
(define items (state '()))

(define (init)
  (db-query my-query
    (lambda (rows err)
      (if (not err) (state-set! items rows)))))

(define (app)
  (scaffold #:on-start init
    (column ...)))
```

---

### `#:on-resume`

Runs on **every recompose**. Use to refresh data that may have changed while the app was backgrounded or after a navigation event.

Implemented with `LaunchedEffect(true)`.

```scheme
(define (refresh)
  (db-query my-query
    (lambda (rows err)
      (if (not err) (state-set! items rows)))))

(define (app)
  (scaffold #:on-resume refresh
    (column ...)))
```

---

### `#:on-close`

Runs when the scaffold is disposed (app window closes or composable leaves the tree). Use for cleanup: closing connections, flushing unsaved state.

Implemented with `DisposableEffect`.

```scheme
(define (cleanup)
  (db-close my-db))

(define (app)
  (scaffold #:on-close cleanup
    (column ...)))
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

### Android Platform Functions

The following functions are only available when running on Android. They provide access to Android-specific features and device information.

#### toast

Shows a brief notification message to the user.

```scheme
(toast message [duration]) -> void
```

**Parameters:**
- `message` - The message text to display (string)
- `duration` - Optional duration: `"long"` for long duration, or number > 2000 for long (default: short)

**Example:**

```scheme
(button #:on-click (lambda () (toast "Item saved!"))
  (text #:value "Save"))

(toast "Processing..." "long")
```

---

#### vibrate

Vibrates the device for a specified duration.

```scheme
(vibrate milliseconds) -> void
```

**Parameters:**
- `milliseconds` - Duration of vibration in milliseconds (number, default: 100)

**Example:**

```scheme
(button #:on-click (lambda () (vibrate 200))
  (text #:value "Vibrate"))
```

**Note:** Requires `VIBRATE` permission in AndroidManifest.xml.

---

#### dark-mode?

Checks if the system is in dark mode.

```scheme
(dark-mode?) -> #t/#f
```

**Returns:** `#t` if dark mode is enabled, `#f` otherwise

**Example:**

```scheme
(if (dark-mode?)
    (text #:value "Dark theme active" #:color 'white)
    (text #:value "Light theme active" #:color 'black))
```

---

#### screen-width

Returns the screen width in density-independent pixels (dp).

```scheme
(screen-width) -> number
```

**Returns:** Screen width in dp

**Example:**

```scheme
(define is-tablet (> (screen-width) 600))

(if is-tablet
    (row #:spacing 16 (sidebar) (main-content))
    (column (main-content)))
```

---

#### screen-height

Returns the screen height in density-independent pixels (dp).

```scheme
(screen-height) -> number
```

**Returns:** Screen height in dp

**Example:**

```scheme
(text #:value (string-append "Screen: "
                             (number->string (screen-width))
                             "x"
                             (number->string (screen-height))))
```

---

#### android-version

Returns the Android API level.

```scheme
(android-version) -> number
```

**Returns:** Android API level (e.g., 33 for Android 13)

**Example:**

```scheme
(if (>= (android-version) 31)
    (use-material-you-colors)
    (use-legacy-colors))
```

---

#### device-model

Returns the device model name.

```scheme
(device-model) -> string
```

**Returns:** Device model string (e.g., "Pixel 7", "SM-G998B")

**Example:**

```scheme
(text #:value (string-append "Device: " (device-model)))
```

---

#### app-folder

Returns the absolute path to the app's folder.

```scheme
(app-folder) -> string
```

**Returns:** Absolute path to the app's data folder

**Example:**

```scheme
(define app-path (app-folder))
(text #:value (string-append "App folder: " app-path))
```

---

#### read-app-file

Reads the contents of a file from the app's folder.

```scheme
(read-app-file filename) -> string
```

**Parameters:**
- `filename` - Name of the file to read (string)

**Returns:** File contents as a string

**Example:**

```scheme
(define settings (read-app-file "settings.json"))
(parse-json settings)
```

**Note:** Throws an exception if the file doesn't exist or can't be read.

---

#### write-app-file

Writes content to a file in the app's folder.

```scheme
(write-app-file filename content) -> void
```

**Parameters:**
- `filename` - Name of the file to write (string)
- `content` - Content to write (string)

**Example:**

```scheme
(write-app-file "settings.json" (json-stringify settings))
(toast "Settings saved")
```

---

#### app-file-exists?

Checks if a file exists in the app's folder.

```scheme
(app-file-exists? filename) -> #t/#f
```

**Parameters:**
- `filename` - Name of the file to check (string)

**Returns:** `#t` if file exists, `#f` otherwise

**Example:**

```scheme
(if (app-file-exists? "settings.json")
    (load-settings)
    (use-default-settings))
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

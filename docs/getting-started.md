# Getting Started with Moonstone

This guide will walk you through building your first Moonstone application.

## Prerequisites

Before you begin, ensure you have:

- **JDK 17 or higher** - [Download from Adoptium](https://adoptium.net/)
- **Git** - For cloning the repository

Verify your Java installation:

```bash
java -version
# Should output: openjdk version "17.x.x" or higher
```

## Installation

### 1. Clone the Repository

```bash
git clone https://github.com/your-username/Moonstone.git
cd Moonstone
```

### 2. Build the Project

```bash
./gradlew build
```

This will download dependencies and compile the framework.

### 3. Verify Installation

Run a sample to verify everything works:

```bash
./gradlew :desktop:run --args="samples/hello-world/app.scm"
```

You should see a window displaying "Hello, Moonstone!".

## Hello World Tutorial

Let's create a simple "Hello World" application from scratch.

### Step 1: Create Your Script

Create a new file `my-app.scm`:

```scheme
(define (app)
  (box
   #:fill-max-size #t
   #:content-alignment 'center
   (text #:value "Hello, World!"
         #:style 'headline-large
         #:color 'blue)))
```

### Step 2: Run Your App

```bash
./gradlew :desktop:run --args="my-app.scm"
```

You should see a centered "Hello, World!" text in blue.

### Understanding the Code

- `(define (app) ...)` - Defines the entry point function
- `(box ...)` - Creates a container that can center its content
- `#:fill-max-size #t` - Makes the box fill the entire window
- `#:content-alignment 'center` - Centers the content
- `(text ...)` - Displays text
- `#:value "Hello, World!"` - The text to display
- `#:style 'headline-large` - Uses a large headline typography style
- `#:color 'blue` - Sets the text color to blue

## Understanding State Management

Moonstone uses reactive state cells for managing application state.

### Creating State

```scheme
(define counter (state 0))
```

This creates a state cell initialized to `0`.

### Reading State

```scheme
(state-ref counter)  ; Returns the current value
```

Or use the state cell directly in a component:

```scheme
(text #:value counter)  ; Automatically updates when state changes
```

### Updating State

```scheme
; Set to a specific value
(state-set! counter 10)

; Update based on current value
(state-update! counter (lambda (x) (+ x 1)))
```

## App Initialization

### Why you can't put side effects in `app`

The `app` function is called on every recompose — which happens whenever any state changes. Putting a side effect like a database query directly in `app` creates an infinite loop: the query triggers a state update, the state update triggers a recompose, the recompose calls `app` again, which fires the query again.

```scheme
; WRONG — runs on every recompose, causing infinite loops
(define (app)
  (load-data-from-db)   ; runs hundreds of times!
  (column ...))
```

### The right pattern — use `#:on-start` on scaffold

Place initialization logic in a function and pass it to scaffold's `#:on-start` hook. It runs once, when the app first appears.

```scheme
(define data (state '()))

(define (load-data)
  (db-query my-query
    (lambda (rows err)
      (if (not err)
          (state-set! data rows)))))

(define (app)
  (scaffold
    #:on-start load-data
    (column
      (for-each (state-ref data)
        (lambda (row) (text #:value (p-map-get row #:name)))))))
```

### Lifecycle hook summary

| Hook | When it runs | Common use |
|------|-------------|------------|
| `#:on-start` | Once, on first render | Load initial data, create DB tables |
| `#:on-resume` | Every recompose | Refresh data when state changes |
| `#:on-close` | When app disposes | Cleanup, save unsaved state |

See the [API Reference](api-reference.md#lifecycle-hooks) for full semantics, and `samples/daily-metrics/app.scm` for a complete real-world example.

---

## Building a Counter App

Let's build an interactive counter application.

### Step 1: Create the Script

Create `counter.scm`:

```scheme
(define count (state 0))

(define (increment)
  (state-update! count (lambda (x) (+ x 1))))

(define (decrement)
  (state-update! count (lambda (x) (- x 1))))

(define (reset)
  (state-set! count 0))

(define (app)
  (column
   #:fill-max-size #t
   #:padding 32
   #:spacing 24
   #:vertical-arrangement 'center
   #:horizontal-alignment 'center

   (text #:value "Counter"
         #:style 'headline-large)

   (text #:value count
         #:style 'display-large)

   (row
    #:spacing 16

    (button #:on-click decrement
      (text #:value "-"))

    (button #:on-click reset
            #:style 'outlined
      (text #:value "Reset"))

    (button #:on-click increment
      (text #:value "+")))))
```

### Step 2: Run It

```bash
./gradlew :desktop:run --args="counter.scm"
```

### How It Works

1. **State Definition**: `(define count (state 0))` creates a reactive state cell
2. **Update Functions**: `increment`, `decrement`, and `reset` modify the state
3. **UI Layout**: `column` arranges children vertically, `row` arranges buttons horizontally
4. **Reactivity**: When state changes, the UI automatically updates

## Adding Interactivity

### Text Input

```scheme
(define name (state ""))

(define (app)
  (column
   #:padding 32
   #:spacing 16

   (text-field
    #:value name
    #:label "Enter your name"
    #:on-change (lambda (v) (state-set! name v)))

   (text #:value name)))
```

### Checkboxes and Switches

```scheme
(define enabled (state #f))

(checkbox
 #:checked enabled
 #:on-change (lambda (v) (state-set! enabled v))
 (text #:value "Enable feature"))

(switch
 #:checked enabled
 #:on-change (lambda (v) (state-set! enabled v))
 (text #:value "Dark mode"))
```

### Radio Buttons

```scheme
(define choice (state 1))

(column
 (radio-button
  #:selected choice
  #:value 1
  #:on-select (lambda () (state-set! choice 1))
  (text #:value "Option A"))

 (radio-button
  #:selected choice
  #:value 2
  #:on-select (lambda () (state-set! choice 2))
  (text #:value "Option B")))
```

## Development Workflow

### Fastest iteration: hot reload

Instead of killing and restarting the app, use the `--hot-reload` flag:

```bash
./gradlew :desktop:run --args="--hot-reload my-app.scm"
```

Save your `.scm` file and the UI updates automatically — no restart needed.

### Gradle daemon

Gradle keeps a daemon running between builds, so the second build is always faster than the first. After initial startup, rebuilds are typically under 2 seconds.

### Build caching

With `org.gradle.caching=true` in `gradle.properties`, Gradle reuses unchanged module outputs. This is particularly helpful when switching between branches.

## Using Hot Reload

During development, use hot reload to see changes instantly:

```bash
./gradlew :desktop:run --args="--hot-reload my-app.scm"
```

Now when you save changes to `my-app.scm`, the UI will automatically update.

## Using Debug Mode

Enable the debug panel for development:

```bash
./gradlew :desktop:run --args="--debug my-app.scm"
```

This shows:
- Reload count
- Error messages
- Component inspector (click "Inspector" button)

Combine with hot reload:

```bash
./gradlew :desktop:run --args="-d -w my-app.scm"
```

## Important Notes

> **Stuck?** See the [Troubleshooting Guide](troubleshooting.md) for common pitfalls and quick fixes.

### Comments

Use `;` for line comments — standard Scheme style works:

```scheme
; This is a comment
(define x 42)  ; inline comment
```

### Entry Point Function

Your script must define one of these functions as the entry point:
- `app` (recommended)
- `main`
- `my-app`
- `root`

## Next Steps

- **Ready to build something real?** → Read the [Building Real Apps Guide](real-apps-guide.md) for database patterns, form validation, navigation, and error handling
- **Hitting an error?** → Check the [Troubleshooting Guide](troubleshooting.md) for common pitfalls
- Explore the [Component Reference](component-reference.md) for all available components
- Read the [API Reference](api-reference.md) for state management and utilities
- Check out the sample applications in the `samples/` directory

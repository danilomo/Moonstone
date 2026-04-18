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

### No Comments

Standard Scheme comments (`;`) are not supported. Structure your code clearly without inline comments.

### Entry Point Function

Your script must define one of these functions as the entry point:
- `app` (recommended)
- `main`
- `my-app`
- `root`

## Next Steps

- Explore the [Component Reference](component-reference.md) for all available components
- Read the [API Reference](api-reference.md) for state management and utilities
- Check out the sample applications in the `samples/` directory

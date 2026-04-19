# Moonstone Architecture

## Overview

Moonstone is a declarative UI framework that allows developers to write native applications using Scheme (Lisp). It bridges KleinLisp (a Scheme interpreter) with Jetpack Compose Multiplatform for rendering, enabling rapid UI development with functional programming paradigms while leveraging modern native UI components.

## Architecture Diagram

```
┌─────────────────┐
│  Scheme Script  │
│    (app.scm)    │
└────────┬────────┘
         │ parse & evaluate
         ▼
┌─────────────────┐
│ MoonstoneRuntime│
│  + KleinLisp    │
│  + StateManager │
│  + ComponentReg │
└────────┬────────┘
         │ build tree
         ▼
┌─────────────────┐
│  UIElement Tree │
│  (components)   │
└────────┬────────┘
         │ render
         ▼
┌─────────────────┐
│   UIRenderer    │
│ (Compose impl)  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Native UI       │
│ (Desktop/Android)│
└─────────────────┘
```

## Module Structure

### core/
The shared multiplatform code containing the entire framework implementation.

**Key subdirectories:**
- `components/` - UI component implementations (35+ components)
  - `impl/` - Concrete component classes (TextComponent, ButtonComponent, etc.)
  - `ComponentFactory.kt` - Component creation interface
  - `ComponentRegistry.kt` - Component lookup and registration
  - `UIElement.kt` - UI tree node abstraction

- `runtime/` - Scheme runtime integration
  - `MoonstoneRuntime.kt` - Main runtime orchestrator
  - `StateCell.kt` - Reactive state container
  - `DerivedStateCell.kt` - Computed state
  - `StateManager.kt` - State coordination
  - `PropParser.kt` - Scheme property parsing

- `render/` - Compose rendering
  - `UIRenderer.kt` - Compose rendering engine
  - `ModifierBuilder.kt` - Modifier generation from props

- `db/` - SQLite ORM
  - Database abstraction layer
  - Schema definition
  - Query building
  - Transaction support

- `debug/` - Development tools
  - Hot reload support
  - Component inspector

- `error/` - Error handling
  - Custom exception types
  - Error context tracking

### desktop/
Desktop application entry point and packaging configuration.

**Key files:**
- `Main.kt` - Application entry point
- `ComponentRegistrar.kt` - Registers all components
- Packaging configuration for native distributions

### android/
Android application entry point and platform-specific extensions.

**Key files:**
- `AppActivity.kt` - Main activity
- `AndroidExtensions.kt` - Android-specific functions (toast, vibrate, etc.)
- `ComponentRegistrar.kt` - Registers all components
- Manifest and resources

## Key Abstractions

### 1. MoonstoneRuntime

The central runtime class that orchestrates the entire framework.

**Location:** `core/src/commonMain/kotlin/net/sourceforge/moonstone/runtime/MoonstoneRuntime.kt`

**Responsibilities:**
- Initialize KleinLisp interpreter
- Manage component registry
- Coordinate state management
- Load and evaluate Scheme scripts
- Find and execute entry point functions
- Register core Scheme functions (state, derived, platform, etc.)

**Key methods:**
- `loadScript(path: Path): UIElement` - Load from file
- `loadCode(code: String): UIElement` - Load from string
- `loadCodeForReactiveMode(code: String)` - Enable reactive updates
- `registerComponent(factory: ComponentFactory)` - Add custom component

### 2. StateCell

A reactive container that triggers UI updates when its value changes.

**Location:** `core/src/commonMain/kotlin/net/sourceforge/moonstone/runtime/StateCell.kt`

**Responsibilities:**
- Store mutable state
- Wrap Compose MutableState for integration
- Notify subscribers on changes
- Schedule UI recomposition

**Scheme API:**
- `(state initial-value)` - Create state cell
- `(state-ref cell)` - Read current value
- `(state-set! cell value)` - Update value
- `(state-update! cell fn)` - Update with function

### 3. DerivedStateCell

A computed state cell that automatically recalculates when dependencies change.

**Location:** `core/src/commonMain/kotlin/net/sourceforge/moonstone/runtime/DerivedStateCell.kt`

**Responsibilities:**
- Store computation function
- Re-evaluate on each access
- Enable Compose snapshot tracking

**Scheme API:**
- `(derived (lambda () <computation>))` - Create derived cell
- `(state-ref derived-cell)` - Get computed value

**Example:**
```scheme
(define count (state 0))
(define display (derived (lambda ()
  (string-append "Count: " (number->string (state-ref count))))))
```

### 4. UIElement

Abstract representation of UI tree nodes.

**Location:** `core/src/commonMain/kotlin/net/sourceforge/moonstone/components/UIElement.kt`

**Properties:**
- `type: String` - Component type identifier
- `props: Map<String, Any?>` - Component properties
- `children: List<UIElement>` - Child elements

**Implementations:**
- `ComponentElement` - Generic component
- `TextElement` - Specialized text element

### 5. UIRenderer

Compose rendering engine that traverses the UI tree and renders components.

**Location:** `core/src/commonMain/kotlin/net/sourceforge/moonstone/render/UIRenderer.kt`

**Responsibilities:**
- Traverse UIElement tree
- Look up component renderers
- Invoke Compose @Composable functions
- Handle render errors

### 6. ComponentRegistry

Registry mapping component names to their factory implementations.

**Location:** `core/src/commonMain/kotlin/net/sourceforge/moonstone/components/ComponentRegistry.kt`

**Responsibilities:**
- Store component factories
- Provide component lookup
- Bind components to Lisp environment

**Methods:**
- `register(name: String, factory: ComponentFactory)` - Add component
- `get(name: String): ComponentFactory?` - Lookup component
- `bindToEnvironment(env: LispEnvironment)` - Register as Scheme functions

## Data Flow

### 1. Script Evaluation

```
Scheme Code → Lisp Parser → AST → Evaluator → Define Functions
```

When a script is loaded:
1. KleinLisp parses the Scheme code into an Abstract Syntax Tree
2. The evaluator processes definitions and expressions
3. Functions like `app`, `main`, etc. are registered in the environment
4. State cells are created and stored

### 2. Component Tree Construction

```
Entry Point Call → Component Function Calls → UIElement Creation → Tree Assembly
```

When the entry point is invoked:
1. Runtime finds and calls the entry function (e.g., `app`)
2. Entry function calls component functions (e.g., `(column ...)`)
3. Each component factory's `create()` method is invoked
4. Props are parsed from keyword arguments
5. Children are recursively created
6. UIElement tree is assembled and returned

### 3. State Change Propagation

```
state-set! → StateCell.value = newValue → notifySubscribers → scheduleRecomposition → re-evaluate app → new UIElement tree
```

When state changes:
1. `state-set!` updates the StateCell value
2. Compose MutableState triggers snapshot invalidation
3. StateManager schedules recomposition
4. In reactive mode, the app function is re-evaluated
5. A new UIElement tree is generated
6. UIRenderer re-renders the UI

### 4. UI Recomposition

```
rootElement.value changes → Compose observes change → UIRenderer called → Component.Render() invoked → Native UI updates
```

During composition:
1. Compose observes `rootElement` state
2. When it changes, UIRenderer is recomposed
3. UIRenderer traverses the new UIElement tree
4. For each element, it looks up the component factory
5. Component's `Render()` composable is invoked
6. Jetpack Compose handles native UI updates

## Extension Points

### 1. Adding New Components

To add a custom component:

```kotlin
class MyComponent : ComponentFactory {
    override val name = "my-component"

    override fun create(params: Array<LispObject>): LispObject {
        val (props, children) = propParser.parse(params)
        val element = ComponentElement(name, props, children)
        return JavaObject(UIElementWrapper(element))
    }

    @Composable
    override fun Render(element: UIElement, renderChild: @Composable (UIElement) -> Unit) {
        // Your Compose UI code here
        Text("Custom component!")
    }
}

// Register it:
runtime.registerComponent(MyComponent())
```

**See:** `core/src/commonMain/kotlin/net/sourceforge/moonstone/components/impl/` for examples.

### 2. Adding Platform Functions

To add Scheme functions available to scripts:

```kotlin
runtime.environment().registerFunction("my-function") { params ->
    // Your implementation
    StringObject("Result")
}
```

**Examples:**
- `MoonstoneRuntime.registerStateFunctions()` - State management functions
- `AndroidExtensions.kt` - Android-specific functions (toast, vibrate, etc.)

### 3. Custom State Types

While StateCell handles most cases, you can create custom reactive types:

```kotlin
class CustomCell(initialValue: LispObject, stateManager: StateManager) {
    private val state = mutableStateOf(initialValue)

    var value: LispObject
        get() = state.value
        set(newValue) {
            // Custom logic here
            state.value = newValue
            stateManager.scheduleRecomposition()
        }
}
```

## Component Lifecycle

### Registration Phase (Startup)

```
1. Create MoonstoneRuntime
2. Call registerAllComponents()
3. Each component factory registered to ComponentRegistry
4. Each component registered as Scheme function in environment
```

### Creation Phase (Script Execution)

```
1. Scheme code calls component function: (button #:label "Click")
2. Lisp environment dispatches to ComponentFactory.create()
3. PropParser extracts keyword arguments into Map
4. Children are recursively created
5. ComponentElement/UIElement created and wrapped
6. Returned to caller (parent component or entry point)
```

### Rendering Phase (Composition)

```
1. UIRenderer receives UIElement tree
2. For each element, looks up ComponentFactory by type
3. Calls factory.Render(element, renderChild)
4. Component renders using Jetpack Compose
5. Children rendered via renderChild callback
6. Compose handles layout, drawing, and event handling
```

## Error Handling

### Script Loading Errors

`ScriptLoadException` wraps errors during script parsing and initial evaluation:
- Syntax errors
- Unbound variables
- Missing entry points

### Runtime Errors

`StateException` for state-related errors:
- Invalid state-ref argument
- state-set! on non-StateCell
- Missing required arguments

### Component Errors

Components can throw during creation or rendering:
- Missing required props
- Invalid prop types
- Too many/few children

**Error Context:** The `ErrorContext` class tracks the current script path for better error messages.

## Platform Abstraction

The `Platform` enum distinguishes between Android and Desktop:

```kotlin
enum class Platform {
    ANDROID,
    DESKTOP_JVM;

    companion object {
        fun detect(): Platform = // Detects based on environment
    }
}
```

Platform-specific features:
- **Desktop:** File system access, window management
- **Android:** Toast, vibration, sensors, permissions

Use `(platform?)` in Scheme to conditionally execute platform-specific code:

```scheme
(if (platform? "android")
    (show-toast "Android!")
    (text #:value "Desktop"))
```

## Performance Considerations

### State Updates

- StateCell uses Compose MutableState for efficient recomposition
- Only changed subtrees are recomposed
- Derived cells recompute on each access (no caching yet)

### Component Creation

- Components created during script evaluation
- Lightweight UIElement objects (data classes)
- No Compose overhead until rendering

### Rendering

- Jetpack Compose handles efficient rendering
- Only changed components re-rendered
- Native platform performance

## Testing Strategy

### Unit Tests

**Location:** `core/src/commonTest/kotlin/`

- `StateCellTest.kt` - State cell behavior
- `DerivedStateCellTest.kt` - Derived state computation
- `MoonstoneRuntimeTest.kt` - Runtime initialization, entry points
- `ComponentRegistryTest.kt` - Component registration and lookup
- `ModifierBuilderTest.kt` - Compose modifier generation

**Location:** `core/src/desktopTest/kotlin/`

- ORM/database tests (integration tests)

### Integration Tests

Test full app scenarios by loading Scheme scripts and verifying the resulting UI tree.

### Manual Testing

- Sample apps in `samples/` directory
- Hot reload for iterative development

## Build System

- **Gradle** with Kotlin Multiplatform
- **Composite build** - KleinLisp is built separately
- **Build flavors:** Debug (hot reload) and Release
- **CI/CD:** GitHub Actions for testing and releases

## Security Considerations

- Scripts have full access to registered functions
- No sandboxing by default
- Database queries use parameterized statements (SQL injection protection)
- File access limited to platform capabilities

## Future Architecture Improvements

See `docs/adr/` for architectural decision records and rationale for current design choices.

## Further Reading

- [Component Reference](component-reference.md) - All 35+ components documented
- [API Reference](api-reference.md) - Scheme API functions
- [ORM Reference](orm-reference.md) - Database abstraction
- [Getting Started](getting-started.md) - Tutorial walkthrough
- [ADRs](adr/) - Architecture Decision Records

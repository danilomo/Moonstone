# ADR-0003: Reactive State Management

## Status

Accepted

## Context

Modern UI frameworks require efficient state management to keep the UI in sync with application data. When state changes, only the affected parts of the UI should update.

Alternative approaches considered:
1. **Manual UI Updates** - Explicit calls to update UI after state changes
2. **Observer Pattern** - Register callbacks for state changes
3. **Reactive Streams** - RxJava/Flow-based reactive programming
4. **Immutable State Trees** - Redux/Elm-style state management
5. **Compose MutableState Integration** - Leverage Compose's built-in reactivity

## Decision

We chose to integrate with **Compose's MutableState** through wrapper classes (`StateCell` and `DerivedStateCell`) that expose a Scheme-friendly API.

### Architecture

```kotlin
class StateCell(initialValue: LispObject, stateManager: StateManager) {
    private val state = mutableStateOf(initialValue)  // Compose state
    private val subscribers = mutableListOf<() -> Unit>()

    var value: LispObject
        get() = state.value
        set(newValue) {
            if (state.value != newValue) {
                state.value = newValue          // Triggers Compose recomposition
                notifySubscribers()              // Explicit subscribers
            }
        }
}
```

### Key Reasons

**1. Zero-Cost Abstraction**
StateCell is a thin wrapper around `mutableStateOf()`. There's no performance overhead - Compose's snapshot system does the heavy lifting.

**2. Automatic Recomposition**
When a StateCell changes during Compose rendering, Compose automatically tracks the dependency and recomposes only affected composables:
```scheme
(define count (state 0))

(define (app)
  (text #:value (number->string (state-ref count))))  ; Reads count

; Later:
(state-set! count 1)  ; UI automatically updates
```

**3. Granular Updates**
Only composables that read the changed state recompose. If a UI has 100 components but only one reads `count`, only that one re-renders.

**4. Derived State**
Computed state is expressed functionally:
```scheme
(define price (state 10))
(define quantity (state 2))
(define total (derived (lambda () (* (state-ref price) (state-ref quantity)))))
```
When `price` or `quantity` changes, `total` automatically recomputes during rendering.

**5. Scheme-Friendly API**
The API follows Scheme conventions:
- `state` - constructor (like `list`, `vector`)
- `state-ref` - getter (like `car`, `vector-ref`)
- `state-set!` - setter (like `set!`, `vector-set!`)
- `state-update!` - functional update (like `map`, `filter`)

**6. Reactive Mode**
Apps can run in reactive mode where the entire app function re-evaluates on state changes:
```scheme
(define count (state 0))
(define (app)
  (column
    (text #:value (number->string (state-ref count)))
    (button #:label "+" #:on-click (lambda () (state-update! count inc)))))
```
Changes to `count` trigger full app re-evaluation, generating a new UI tree.

## Consequences

### What Becomes Easier

**✓ Simple Mental Model**
State is just a container. Read with `state-ref`, write with `state-set!`. The UI updates automatically.

**✓ Composable State**
Multiple derived states can depend on the same base state:
```scheme
(define count (state 0))
(define is-even (derived (lambda () (= (mod (state-ref count) 2) 0))))
(define is-positive (derived (lambda () (> (state-ref count) 0))))
```

**✓ Functional Transformations**
`state-update!` encourages functional thinking:
```scheme
(state-update! count (lambda (n) (+ n 1)))  ; Increment
(state-update! items (lambda (xs) (filter even? xs)))  ; Filter
```

**✓ No Boilerplate**
No need to define action types, reducers, or dispatch functions. Just create state and modify it.

**✓ Testable**
State cells can be tested in isolation without UI:
```kotlin
val cell = StateCell(IntObject(0), stateManager)
cell.value = IntObject(5)
assertEquals(5, cell.value.asInt().value)
```

### What Becomes Harder

**✗ No Time Travel**
Unlike Redux, there's no built-in action history or time-travel debugging. Every state change is immediate and irreversible.

**✗ No Centralized State**
State is distributed across multiple `StateCell` instances. There's no single source of truth to serialize or inspect.

**✗ Derived State Recomputation**
`DerivedStateCell` recomputes on every `state-ref` access. No memoization:
```kotlin
val value: LispObject
    get() = computation.evaluate(emptyArray())  // Always recomputes
```
This is fine for cheap computations but could be slow for expensive ones.

**✗ Manual Dependency Tracking**
Derived cells don't automatically track which states they depend on. The tracking happens through Compose's snapshot system during rendering.

**✗ Circular Dependencies**
Nothing prevents circular derived states:
```scheme
(define a (derived (lambda () (state-ref b))))
(define b (derived (lambda () (state-ref a))))
```
This causes stack overflow at runtime.

### Mitigations

**Time Travel:** Could be added later with a `HistoryStateCell` wrapper that records changes.

**Centralized State:** For apps that need it, create a single root state cell containing a map:
```scheme
(define app-state (state (p-map #:count 0 #:user "Alice")))
```

**Derived Recomputation:**
- Compose's snapshot system caches results during a single composition
- For expensive computations, use regular state and update it explicitly
- Future: Add memoization to DerivedStateCell

**Dependency Tracking:** Document best practices. Derived cells should only read state, not have side effects.

**Circular Dependencies:** Could add cycle detection, but GIGO (Garbage In, Garbage Out) - don't write circular dependencies.

## Alternatives Considered

### Manual UI Updates
```kotlin
val count = 0
fun increment() {
    count++
    updateUI()  // Manual call
}
```
**Rejected** because:
- Fragile - easy to forget `updateUI()`
- Error-prone - might update wrong parts
- Doesn't scale to large UIs

### Observer Pattern
```kotlin
class ObservableState<T>(private var value: T) {
    private val observers = mutableListOf<(T) -> Unit>()
    fun set(newValue: T) {
        value = newValue
        observers.forEach { it(newValue) }
    }
}
```
**Rejected** because:
- Manual subscription management
- Memory leaks if observers not removed
- Doesn't integrate with Compose recomposition

### Reactive Streams (RxJava/Flow)
```kotlin
val count = MutableStateFlow(0)
count.collect { newValue ->
    // Update UI
}
```
**Rejected** because:
- Steep learning curve (hot vs cold, backpressure, operators)
- Overkill for simple UI state
- Doesn't integrate natively with Compose
- Hard to expose to Scheme

### Redux/Elm Architecture
```kotlin
sealed class Action {
    object Increment : Action()
}

fun reducer(state: State, action: Action): State =
    when (action) {
        is Increment -> state.copy(count = state.count + 1)
    }
```
**Rejected** because:
- Too much boilerplate for Scheme scripts
- Requires defining action types (not dynamic)
- Overkill for small apps
- Time-travel debugging rarely needed in practice

## Real-World Usage

From `samples/todo/app.scm`:
```scheme
(define todos (state (list)))
(define input-text (state ""))

(define (add-todo!)
  (when (not (string=? (state-ref input-text) ""))
    (state-update! todos (lambda (ts)
      (append ts (list (p-map #:text (state-ref input-text) #:done #f)))))
    (state-set! input-text "")))

(define (app)
  (column #:spacing 16
    (row #:spacing 8
      (text-field #:value (state-ref input-text)
                  #:on-change (lambda (v) (state-set! input-text v)))
      (button #:label "Add" #:on-click add-todo!))
    (column . (map (lambda (todo)
                     (todo-item todo))
                   (state-ref todos)))))
```

This demonstrates:
- Multiple independent state cells (`todos`, `input-text`)
- Functional state updates (`state-update!` with `append`)
- State reset (`state-set! input-text ""`)
- State reading in UI (`state-ref`)

All with minimal code and no ceremony.

## Performance Characteristics

**StateCell.value setter:**
- O(1) - Direct assignment to MutableState
- Triggers Compose snapshot invalidation (fast)

**state-ref:**
- O(1) - Direct property access
- Registers Compose dependency if in composition

**state-update!:**
- O(f) where f is the update function cost
- Plus O(1) for the state write

**DerivedStateCell:**
- O(f) on every access where f is the computation cost
- No memoization

**Recomposition:**
- O(changed composables) - Only recomposes affected parts
- Compose's diffing is highly optimized

## Testing State

Tests verify state behavior without UI:

```kotlin
@Test
fun `state-set! updates value`() {
    val stateManager = StateManager()
    val cell = StateCell(IntObject(0), stateManager)

    cell.value = IntObject(10)

    assertEquals(10, cell.value.asInt()?.value)
}

@Test
fun `derived updates when dependency changes`() {
    val stateManager = StateManager()
    val source = stateManager.createCell(IntObject(5))

    val derived = DerivedStateCell(object : Function {
        override fun evaluate(args: Array<LispObject>): LispObject =
            IntObject(source.value.asInt()!!.value * 2)
    })

    assertEquals(10, derived.value.asInt()?.value)

    source.value = IntObject(10)
    assertEquals(20, derived.value.asInt()?.value)
}
```

## Related Decisions

- ADR-0001: Use Jetpack Compose (enables MutableState integration)
- ADR-0002: Scheme as DSL (functional API fits Scheme conventions)

## References

- [Compose State Documentation](https://developer.android.com/jetpack/compose/state)
- [Snapshot System](https://developer.android.com/jetpack/compose/lifecycle#state-snapshots)
- [Scheme R7RS (set! semantics)](https://small.r7rs.org/)

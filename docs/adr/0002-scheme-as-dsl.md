# ADR-0002: Scheme as DSL for UI Definition

## Status

Accepted

## Context

Moonstone needed a way for users to define UIs declaratively. The choice of language for this DSL significantly impacts usability, expressiveness, and implementation complexity.

Alternative options considered:
1. **Kotlin DSL** - Type-safe builders in Kotlin
2. **JSON/YAML** - Declarative data formats
3. **XML** - Traditional markup language
4. **Custom DSL** - Purpose-built syntax
5. **Scheme (Lisp)** - Functional programming language

## Decision

We chose **Scheme (specifically KleinLisp)** as the DSL for defining UIs in Moonstone.

### Key Reasons

**1. Code as Data (Homoiconicity)**
Scheme's syntax is its data structure. A UI definition:
```scheme
(column #:spacing 16
  (text #:value "Hello")
  (button #:label "Click"))
```
is literally a list that can be manipulated programmatically. This enables powerful metaprogramming and UI generation.

**2. Minimal Syntax**
Scheme has almost no syntax to learn - just parentheses and atoms. Compare:
```scheme
(button #:label "Click" #:on-click handler)
```
vs Kotlin DSL:
```kotlin
button {
    label = "Click"
    onClick = handler
}
```
The Scheme version is more concise and uniform.

**3. Functional Programming**
Scheme's functional nature encourages:
- Pure functions for UI components
- Immutable data structures
- Composable abstractions
- Reactive state transformations with `derived`

**4. Dynamic Typing for Rapid Prototyping**
While Kotlin's type system is powerful, it adds friction during UI exploration. Scheme allows:
```scheme
(define (make-ui count)
  (if (> count 5)
      (text #:value "Many")
      (text #:value "Few")))
```
Without type annotations or explicit returns.

**5. Existing Implementation**
KleinLisp (developed alongside Moonstone) provides a clean, embeddable Scheme interpreter written in Kotlin. No parser generator or language tooling needed.

**6. REPL-Driven Development**
Scheme's REPL tradition enables interactive development:
```scheme
> (define my-ui (column (text #:value "Test")))
> (update-app (lambda () my-ui))  ; Live update the running app
```

**7. Educational Value**
Scheme is widely used in CS education. Moonstone can serve as a practical Scheme project for students learning functional programming.

## Consequences

### What Becomes Easier

**✓ UI Generation**
Generating UIs programmatically is trivial with Lisp macros and functions:
```scheme
(define (make-items items)
  (map (lambda (item)
         (text #:value item))
       items))

(column . (make-items (list "A" "B" "C")))
```

**✓ Reactive Transformations**
Derived state naturally expresses computations:
```scheme
(define count (state 0))
(define label (derived (lambda ()
  (string-append "Count: " (number->string (state-ref count))))))
```

**✓ Abstraction**
Creating reusable UI components is just defining functions:
```scheme
(define (card title body)
  (surface #:padding 16
    (column
      (text #:value title #:style "headline")
      (text #:value body))))
```

**✓ Conditional UI**
Control flow uses familiar Scheme constructs:
```scheme
(if (platform? "android")
    (show-toast "Mobile")
    (text #:value "Desktop"))
```

**✓ Hot Reload**
Scheme's dynamic nature makes hot reload straightforward - re-evaluate changed functions and update the UI.

### What Becomes Harder

**✗ IDE Support**
No IntelliJ autocomplete, no type checking, no refactoring tools. Users rely on documentation and REPL exploration.

**✗ Error Messages**
Runtime errors in Scheme can be cryptic:
```
UnboundVariableException: textt
```
vs Kotlin compile-time:
```
Unresolved reference: textt. Did you mean 'text'?
```

**✗ Learning Curve**
Users unfamiliar with Lisp syntax need to learn:
- Prefix notation: `(+ 1 2)` not `1 + 2`
- Keyword arguments: `#:label "Click"`
- No semicolons for comments in KleinLisp

**✗ Performance**
Interpreted Scheme is slower than compiled Kotlin. While not an issue for UI definition, complex computations should be in Kotlin.

**✗ Type Safety**
No compile-time guarantees about prop types:
```scheme
(button #:label 42)  ; Runtime error, should be string
```

### Mitigations

**IDE Support:** Provide comprehensive documentation with examples. Syntax highlighting for `.scm` files works in most editors.

**Error Messages:** Custom exceptions (`StateException`, `ScriptLoadException`) provide helpful hints:
```
StateException: Expected a state cell, got: IntObject
Hint: Create a state cell first with (define my-state (state "initial-value"))
```

**Learning Curve:**
- Tutorial in `docs/getting-started.md` with step-by-step examples
- Sample apps demonstrating patterns
- Quick reference card

**Performance:**
- Hot paths (rendering) are in Kotlin
- UI definition happens once at load time
- State updates are Kotlin (StateCell)

**Type Safety:**
- Runtime validation in `ComponentFactory.create()`
- `propTypes` map documents expected types
- Helpful error messages when validation fails

## Alternatives Considered

### Kotlin DSL
**Rejected** because:
- Requires recompilation for UI changes (no hot reload)
- Type-safe builders add ceremony: `button { label = "Click" }` vs `(button #:label "Click")`
- No data-as-code metaprogramming
- Doesn't match the "scripting" vision of Moonstone

### JSON/YAML
**Rejected** because:
- No logic or control flow (pure data)
- Verbose for nested structures
- No abstraction mechanism (can't define reusable components)
- No REPL

### XML
**Rejected** because:
- Verbose: `<button label="Click" />` vs `(button #:label "Click")`
- No native support for functions or logic
- Parsing complexity
- Not homoiconic

### Custom DSL
**Rejected** because:
- Months of work to design, implement, and debug a parser
- No ecosystem or community
- Would likely converge on a Lisp-like syntax anyway
- No REPL infrastructure

## Real-World Usage

Example from `samples/counter/app.scm`:
```scheme
(define count (state 0))

(define (app)
  (column #:spacing 16 #:padding 16
    (text #:value (number->string (state-ref count))
          #:style "headline")
    (button #:label "Increment"
            #:on-click (lambda ()
              (state-update! count (lambda (n) (+ n 1)))))))
```

This 9-line app demonstrates:
- State management
- Reactive updates
- Event handling
- Layout composition

All in pure Scheme, no Kotlin code required.

## Related Decisions

- ADR-0001: Use Jetpack Compose (Scheme's declarative model maps well to Compose)
- ADR-0003: Reactive State (functional transformations with `derived`)

## References

- [Structure and Interpretation of Computer Programs](https://mitpress.mit.edu/sites/default/files/sicp/index.html)
- [Scheme Language Specification (R7RS)](https://small.r7rs.org/)
- [KleinLisp Implementation](https://github.com/danilo-favoratti/KleinLisp)
- [Lisp as DSL Success Stories](https://www.cliki.net/application)

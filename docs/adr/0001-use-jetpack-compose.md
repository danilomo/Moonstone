# ADR-0001: Use Jetpack Compose for Rendering

## Status

Accepted

## Context

Moonstone needed a modern, declarative UI rendering system that:
- Works across Desktop and Android platforms
- Provides high-quality native UI components
- Integrates well with reactive state management
- Has good performance characteristics
- Is actively maintained and has a strong ecosystem

Alternative options considered:
1. **Swing (Desktop) + Android Views** - Platform-specific implementations
2. **JavaFX** - Cross-platform but limited Android support
3. **Custom Canvas Rendering** - Full control but massive implementation effort
4. **Jetpack Compose Multiplatform** - Modern declarative framework

## Decision

We chose **Jetpack Compose Multiplatform** as the rendering layer for Moonstone.

### Key Reasons

**1. Declarative Paradigm Match**
Compose's declarative model aligns perfectly with Moonstone's Scheme-based declarative UI definitions. The mapping from `(button #:label "Click")` to `Button(text = "Click")` is natural and intuitive.

**2. Reactive State Integration**
Compose has built-in reactive state management through `mutableStateOf()` and snapshot system. This allows Moonstone's StateCell to integrate seamlessly:
```kotlin
class StateCell(initialValue: LispObject) {
    private val state = mutableStateOf(initialValue)
    var value: LispObject
        get() = state.value
        set(newValue) { state.value = newValue } // Triggers recomposition
}
```

**3. True Multiplatform**
Compose Multiplatform supports Desktop (JVM), Android, iOS (experimental), and Web (experimental). This gives Moonstone a clear path to expand beyond Desktop and Android.

**4. Material Design Components**
Material 3 provides 35+ high-quality, accessible UI components out of the box. This saved months of development time implementing custom widgets.

**5. Performance**
Compose's smart recomposition only updates changed parts of the UI. Combined with Compose's compiler optimizations, this provides excellent performance even for complex UIs.

**6. Active Development**
Backed by Google and JetBrains, Compose has strong industry support and a thriving ecosystem. Regular updates and improvements are guaranteed.

**7. Kotlin Integration**
Being a Kotlin-first framework, Compose integrates naturally with Moonstone's Kotlin codebase. No JNI bridges or FFI complexity.

## Consequences

### What Becomes Easier

**✓ Component Implementation**
Adding new components is straightforward - implement the `ComponentFactory` interface and write a `@Composable` function:
```kotlin
@Composable
override fun Render(element: UIElement, renderChild: @Composable (UIElement) -> Unit) {
    Button(onClick = { /* ... */ }) { Text("Click me") }
}
```

**✓ Reactive Updates**
State changes automatically trigger UI updates through Compose's snapshot system. No manual diffing or dirty checking required.

**✓ Layout System**
Compose's powerful layout system (Row, Column, Box) handles complex arrangements without manual calculations.

**✓ Styling and Theming**
Material Theme support provides consistent styling across the app. Dark mode works automatically.

**✓ Accessibility**
Material components include accessibility support out of the box.

### What Becomes Harder

**✗ Binary Size**
Compose adds ~8-10MB to the application size (Android APK/Desktop JAR). This is acceptable for most modern applications.

**✗ Learning Curve for Contributors**
Contributors need to understand both Scheme/Lisp and Compose. However, component implementation is well-isolated and examples are plentiful.

**✗ Platform-Specific Features**
Some platform-specific widgets require custom implementations. For example, Android's `WebView` needs special handling in Compose.

**✗ Version Coupling**
Moonstone's Compose version must stay reasonably current to get bug fixes and new features. This requires periodic dependency updates.

**✗ Debugging**
Compose's compiler-generated code can make debugging more complex. Stack traces may include synthetic functions.

### Mitigations

**Binary Size:** For desktop applications, size is rarely an issue. For Android, ProGuard/R8 can reduce the size further.

**Learning Curve:** Comprehensive examples in `samples/` and detailed component reference documentation help onboard contributors.

**Platform-Specific Features:** Create wrapper components for platform-specific widgets (e.g., `WebViewComponent` wraps `AndroidView` on Android).

**Version Coupling:** Maintain compatibility across a range of Compose versions. Test against both LTS and latest releases.

## Alternatives Considered

### Swing + Android Views
**Rejected** because it would require maintaining two completely separate rendering implementations, doubling the codebase and testing burden.

### JavaFX
**Rejected** due to limited Android support and uncertain future. JavaFX on Android requires third-party ports with questionable maintenance.

### Custom Canvas Rendering
**Rejected** because implementing layout, text rendering, accessibility, and 35+ widgets from scratch would take years and never match the quality of Material components.

## Related Decisions

- ADR-0002: Scheme as DSL (benefits from Compose's declarative model)
- ADR-0003: Reactive State (leverages Compose's snapshot system)

## References

- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Material 3 Design](https://m3.material.io/)

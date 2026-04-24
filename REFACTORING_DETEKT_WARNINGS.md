# User Story: Refactor Code to Resolve Remaining Detekt Warnings

## Overview

As a developer, I want to refactor the codebase to resolve the remaining detekt warnings so that the code is more maintainable, testable, and follows Kotlin best practices.

## Current Status

✅ **Completed:**
- Replaced unsafe casts with safe casts
- Converted `throw IllegalArgumentException` to `require()`
- Converted `throw IllegalStateException` to `check()` / `error()`
- Fixed line length violations
- Extracted duplicate string literals to constants (partial)
- Fixed all ktlint formatting issues

🔴 **Remaining Issues:**
- **Core module:** 207 warnings (primarily in DatabaseExtensions.kt)
- **Android module:** 45 warnings
- **Desktop module:** 7 warnings

## Detailed Breakdown

### 1. DatabaseExtensions.kt - String Literal Duplication (10 instances)

**Problem:** Database operation strings and error messages are duplicated throughout the file.

**Current State:**
```kotlin
// Lines 100, 243, 399, 787, etc.
throw IllegalArgumentException("db-table: first argument must be a table name symbol")
throw IllegalArgumentException("#:where requires a condition expression")
// ... many similar error messages
```

**Proposed Solution:**
- Extract error message templates to companion object constants
- Create error message builder functions
- Consider creating a DatabaseErrorMessages sealed class

**Example Refactor:**
```kotlin
companion object {
    private object ErrorMessages {
        const val TABLE_NAME_REQUIRED = "db-table: first argument must be a table name symbol"
        const val WHERE_CONDITION_REQUIRED = "#:where requires a condition expression"

        fun missingParameter(fnName: String, paramName: String) =
            "$fnName: missing parameter #:$paramName"
    }
}
```

**Effort:** Medium (2-3 hours)

---

### 2. Long Methods - Exceeding 40 Lines

**Problem:** Several functions exceed the 40-line threshold, making them hard to understand and test.

**Files Affected:**
- `desktop/Main.kt`:
  - `runWithDebugMode()` - 80 lines
  - `runNormalMode()` - 41 lines
- `core/DatabaseExtensions.kt`:
  - Multiple query building functions
- `core/components/CardComponent.kt`:
  - `Render()` - 86 lines

**Proposed Solution:**
- Extract logical blocks into smaller, focused functions
- Use composition and delegation patterns
- Apply Single Responsibility Principle

**Example Refactor for `runWithDebugMode`:**
```kotlin
// Before: 80-line function
fun runWithDebugMode() {
    // ... 80 lines of mixed concerns
}

// After: Multiple focused functions
fun runWithDebugMode() {
    val config = loadDebugConfiguration()
    val runtime = initializeRuntime(config)
    val repl = startReplServer(runtime)
    launchDebugWindow(runtime, repl)
}

private fun loadDebugConfiguration(): DebugConfig { ... }
private fun initializeRuntime(config: DebugConfig): MoonstoneRuntime { ... }
private fun startReplServer(runtime: MoonstoneRuntime): ReplServer { ... }
private fun launchDebugWindow(runtime: MoonstoneRuntime, repl: ReplServer) { ... }
```

**Effort:** High (1-2 days)

---

### 3. Cognitive Complexity - Exceeding Threshold of 15

**Problem:** Complex nested logic makes code difficult to understand and maintain.

**Files Affected:**
- `desktop/Main.kt`:
  - `runWithDebugMode()` - Complexity: 39
- `core/DatabaseExtensions.kt`:
  - Multiple query parsing functions

**Proposed Solution:**
- Reduce nesting by using early returns (guard clauses)
- Extract complex conditions into well-named boolean functions
- Replace nested if-else with polymorphism or strategy pattern where appropriate
- Consider using when expressions for complex branching

**Example Refactor:**
```kotlin
// Before: High cognitive complexity
fun processData(input: Data): Result {
    if (input.isValid()) {
        if (input.hasType()) {
            if (input.type == "special") {
                // ... nested logic
            } else {
                // ... more nested logic
            }
        } else {
            // ... error handling
        }
    } else {
        // ... validation errors
    }
}

// After: Reduced complexity with guard clauses
fun processData(input: Data): Result {
    if (!input.isValid()) return handleValidationError(input)
    if (!input.hasType()) return handleMissingType(input)

    return when (input.type) {
        "special" -> processSpecialType(input)
        else -> processNormalType(input)
    }
}

private fun handleValidationError(input: Data): Result { ... }
private fun handleMissingType(input: Data): Result { ... }
private fun processSpecialType(input: Data): Result { ... }
private fun processNormalType(input: Data): Result { ... }
```

**Effort:** High (2-3 days)

---

### 4. Nested Block Depth - Too Many Levels

**Problem:** Deeply nested code blocks are hard to read and reason about.

**Files Affected:**
- `desktop/Main.kt`:
  - `parseArgs()` - Line 136
  - `readDbLocation()` - Line 398
- `core/DatabaseExtensions.kt`:
  - `executeRawQuery()` - Line 224
- `core/SchemaRegistry.kt`:
  - `validateReferences()` - Line 56

**Proposed Solution:**
- Extract nested blocks into separate functions
- Use early returns to reduce nesting
- Consider using functional approaches (map, filter, fold)

**Example Refactor:**
```kotlin
// Before: Deeply nested
fun validateReferences() {
    for (table in tables) {
        for (column in table.columns) {
            if (column.hasReference) {
                for (refTable in tables) {
                    if (refTable.name == column.refTable) {
                        // ... validation logic
                    }
                }
            }
        }
    }
}

// After: Flattened with extracted functions
fun validateReferences() {
    tables.forEach { table ->
        table.columns
            .filter { it.hasReference }
            .forEach { column -> validateColumnReference(column, tables) }
    }
}

private fun validateColumnReference(column: Column, tables: List<Table>) {
    val refTable = tables.find { it.name == column.refTable }
        ?: error("Referenced table ${column.refTable} not found")
    // ... validation logic
}
```

**Effort:** Medium (1-2 days)

---

### 5. Too Many Functions in File - Exceeding Threshold of 15

**Problem:** Large files with many functions violate cohesion principles.

**Files Affected:**
- `desktop/Main.kt` - 15 functions
- Potentially other files in core module

**Proposed Solution:**
- Split large files into multiple focused files
- Group related functions into classes or objects
- Consider creating separate files for:
  - Configuration handling
  - Argument parsing
  - REPL server setup
  - Debug mode operations
  - Normal mode operations

**Example Refactor:**
```kotlin
// Before: Main.kt with 15 functions
// Main.kt
fun main(args: Array<String>) { ... }
fun parseArgs(...) { ... }
fun runNormalMode(...) { ... }
fun runWithDebugMode(...) { ... }
fun readDbLocation(...) { ... }
// ... 10 more functions

// After: Split into focused files
// Main.kt - Entry point only
fun main(args: Array<String>) {
    val config = ArgParser.parse(args)
    ApplicationRunner.run(config)
}

// ArgParser.kt
object ArgParser {
    fun parse(args: Array<String>): AppConfig { ... }
}

// ApplicationRunner.kt
object ApplicationRunner {
    fun run(config: AppConfig) {
        when {
            config.debugMode -> DebugModeRunner.run(config)
            else -> NormalModeRunner.run(config)
        }
    }
}

// DebugModeRunner.kt
object DebugModeRunner {
    fun run(config: AppConfig) { ... }
}

// NormalModeRunner.kt
object NormalModeRunner {
    fun run(config: AppConfig) { ... }
}
```

**Effort:** Medium (1 day)

---

### 6. Long Parameter List - Exceeding 4-6 Parameters

**Problem:** Functions with many parameters are hard to call and maintain.

**Files Affected:**
- `desktop/ReplAppWindow.kt`:
  - `runReplAppWindow()` - 7 parameters

**Proposed Solution:**
- Group related parameters into data classes
- Use builder pattern for complex object creation
- Apply parameter object refactoring

**Example Refactor:**
```kotlin
// Before: 7 parameters
fun runReplAppWindow(
    initialElement: UIElement,
    componentRegistry: ComponentRegistry,
    stateManager: StateManager,
    onRootCreated: (MutableState<UIElement>) -> Unit,
    onClosed: () -> Unit,
    windowWidth: Int,
    windowHeight: Int
) { ... }

// After: Grouped into data classes
data class ReplWindowConfig(
    val initialElement: UIElement,
    val componentRegistry: ComponentRegistry,
    val stateManager: StateManager,
    val windowSize: WindowSize = WindowSize.default()
)

data class ReplWindowCallbacks(
    val onRootCreated: (MutableState<UIElement>) -> Unit,
    val onClosed: () -> Unit
)

data class WindowSize(val width: Int, val height: Int) {
    companion object {
        fun default() = WindowSize(800, 600)
    }
}

fun runReplAppWindow(
    config: ReplWindowConfig,
    callbacks: ReplWindowCallbacks
) { ... }

// Usage
runReplAppWindow(
    config = ReplWindowConfig(
        initialElement = element,
        componentRegistry = registry,
        stateManager = manager
    ),
    callbacks = ReplWindowCallbacks(
        onRootCreated = { ... },
        onClosed = { ... }
    )
)
```

**Effort:** Low (2-3 hours)

---

## Acceptance Criteria

- [ ] All detekt warnings reduced to under 50 total across all modules
- [ ] No function exceeds 40 lines (or has explicit @Suppress with justification)
- [ ] No function has cognitive complexity > 15
- [ ] No nested blocks exceed 4 levels deep
- [ ] No file has more than 15 functions (or is explicitly justified)
- [ ] No function has more than 6 parameters
- [ ] All changes maintain or improve test coverage
- [ ] Code reviews confirm improved readability
- [ ] Documentation updated to reflect architectural changes

## Implementation Plan

### Phase 1: Quick Wins (1 day)
1. Fix long parameter lists with data classes
2. Extract string literal constants in DatabaseExtensions.kt
3. Split Main.kt into separate files

### Phase 2: Method Extraction (2-3 days)
1. Refactor `runWithDebugMode()` and `runNormalMode()`
2. Extract methods from long functions in DatabaseExtensions.kt
3. Refactor CardComponent.Render()

### Phase 3: Complexity Reduction (2-3 days)
1. Apply guard clauses to reduce nesting
2. Extract complex conditions into named functions
3. Refactor nested loops in SchemaRegistry and DatabaseExtensions

### Phase 4: Validation (1 day)
1. Run full test suite
2. Manual testing of affected features
3. Performance validation
4. Code review

## Technical Debt Notes

### Why These Warnings Exist

1. **DatabaseExtensions.kt**: This file handles complex database operations with many similar patterns. The original implementation prioritized getting functionality working over perfect code organization.

2. **Main.kt**: Desktop entry point grew organically as features were added (REPL, debug mode, normal mode, argument parsing).

3. **Render functions**: Compose UI rendering often requires many parameters and complex layout logic, naturally leading to longer functions.

### Benefits of Refactoring

- **Maintainability**: Smaller, focused functions are easier to understand and modify
- **Testability**: Extracted functions can be unit tested independently
- **Reusability**: Common logic can be shared across modules
- **Onboarding**: New developers can understand the codebase more quickly
- **Bug Prevention**: Simpler code reduces the surface area for bugs

### Risks

- **Regression**: Changes to working code may introduce bugs
- **Over-engineering**: Too much abstraction can hurt readability
- **Time Investment**: Significant refactoring takes time away from features

### Mitigation Strategy

- Maintain comprehensive test coverage
- Refactor incrementally with frequent testing
- Code review each change
- Keep refactorings focused and well-scoped
- Use feature flags if refactoring critical paths

## References

- [Detekt Documentation](https://detekt.dev/)
- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Clean Code by Robert C. Martin](https://www.oreilly.com/library/view/clean-code-a/9780136083238/)
- [Refactoring: Improving the Design of Existing Code](https://martinfowler.com/books/refactoring.html)

## Related Issues

- Current detekt configuration: `detekt.yml` in project root
- CI/CD integration: Consider adding detekt checks to prevent new violations
- Code review guidelines: Update to include complexity checks

---

**Created:** 2026-04-24
**Status:** 📋 Backlog
**Priority:** Medium
**Estimated Effort:** 1-2 weeks
**Labels:** `refactoring`, `code-quality`, `technical-debt`, `detekt`

# Contributing to Moonstone

Thank you for your interest in contributing to Moonstone! This document provides guidelines and information for contributors.

## Getting Started

### Prerequisites

- **JDK 17** or higher
- **Git** for version control
- **An IDE** - IntelliJ IDEA recommended (Community or Ultimate)

### Development Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/danilomo/Moonstone.git
   cd Moonstone
   ```

2. **Build the project**
   ```bash
   ./gradlew build
   ```

3. **Run a sample application**
   ```bash
   ./gradlew :desktop:run --args="samples/counter/app.scm"
   ```

4. **Run with hot reload** (for development)
   ```bash
   ./gradlew :desktop:run --args="--hot-reload samples/counter/app.scm"
   ```

### Running Tests

```bash
# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :core:test
```

## Code Style

Moonstone uses **ktlint** and **Detekt** for code style enforcement and static analysis. These are automatically checked in CI.

### Before Submitting

```bash
# Check code style
./gradlew ktlintCheck

# Run static analysis
./gradlew detekt

# Auto-format code
./gradlew formatAll

# Run all checks
./gradlew lintAll
```

### Style Guidelines

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Maximum line length: 120 characters
- No wildcard imports
- Use meaningful variable and function names

## Pull Request Workflow

1. **Fork the repository** and create your branch from `main`
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes** following the code style guidelines

3. **Add tests** for new functionality

4. **Run all checks locally**
   ```bash
   ./gradlew build
   ./gradlew test
   ./gradlew lintAll
   ```

5. **Commit your changes** with a clear, descriptive message
   ```bash
   git commit -m "Add feature: brief description"
   ```

6. **Push to your fork**
   ```bash
   git push origin feature/your-feature-name
   ```

7. **Open a Pull Request** against the `main` branch

### PR Guidelines

- Keep PRs focused on a single feature or fix
- Include a clear description of the changes
- Reference any related issues
- Ensure all CI checks pass
- Be responsive to feedback and review comments

## Adding New Components

Components live in `core/src/commonMain/kotlin/net/sourceforge/moonstone/components/impl/`.

### Component Structure

1. **Create your component file** following existing patterns:
   ```kotlin
   // MyComponent.kt
   package net.sourceforge.moonstone.components.impl

   import net.sourceforge.moonstone.components.base.LeafComponent
   import net.sourceforge.moonstone.runtime.KLValue
   import androidx.compose.runtime.Composable

   class MyComponent : LeafComponent("my-component") {
       override val supportedProps = setOf(
           "value",
           "on-click",
           // ... other props
       )

       @Composable
       override fun render(props: Map<String, KLValue>) {
           // Compose implementation
       }
   }
   ```

2. **Register the component** in the component registry

3. **Add documentation** to `docs/component-reference.md`

4. **Add a sample** demonstrating usage

### Component Guidelines

- Follow existing component patterns for consistency
- Support common props like `modifier`, `enabled`, etc.
- Handle null/missing props gracefully
- Add appropriate default values

## Adding Samples

Samples live in the `samples/` directory. Each sample should:

1. **Be self-contained** in its own directory
   ```
   samples/my-sample/
   └── app.scm
   ```

2. **Include clear comments** explaining the demonstrated concepts

3. **Follow KleinLisp conventions**
   - Use `#t`/`#f` for booleans
   - Use `state`, `state-ref`, `state-set!`, `state-update!` for state
   - Define an entry point function (`app`, `main`, etc.)

4. **Be runnable** with:
   ```bash
   ./gradlew :desktop:run --args="samples/my-sample/app.scm"
   ```

### Sample Categories

- **Beginner**: Basic concepts (hello-world, counter, greeting)
- **Intermediate**: Multiple components, state (todo, navigation, dialogs)
- **Advanced**: Database, complex state (database-crud, form-validation)

## Bug Reports

When filing a bug report, please include:

- **Description**: Clear description of the issue
- **Steps to reproduce**: Minimal steps to trigger the bug
- **Expected behavior**: What you expected to happen
- **Actual behavior**: What actually happened
- **Environment**: OS, JDK version, Moonstone version
- **Sample code**: Minimal Scheme code that reproduces the issue

## Feature Requests

For feature requests, please include:

- **Use case**: Why is this feature needed?
- **Proposed solution**: How do you envision this working?
- **Alternatives**: Any alternative solutions you've considered

## Questions and Discussions

- Check existing [issues](https://github.com/danilomo/Moonstone/issues) first
- Read the [documentation](docs/) for guidance
- Open a new issue for questions not covered elsewhere

## License

By contributing to Moonstone, you agree that your contributions will be licensed under the MIT License.

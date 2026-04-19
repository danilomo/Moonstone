# Moonstone Sample Applications

This directory contains example applications demonstrating Moonstone features. Start with the beginner samples and progress to more advanced examples.

## Running Samples

```bash
./gradlew :desktop:run --args="samples/<sample-name>/app.scm"

# With hot reload (recommended for learning)
./gradlew :desktop:run --args="--hot-reload samples/<sample-name>/app.scm"
```

## Beginner (Start Here)

| Sample | Description | Concepts |
|--------|-------------|----------|
| [hello-world](hello-world/) | Minimal app structure | Basic setup, entry point |
| [counter](counter/) | Increment a number | State, buttons, events |
| [greeting](greeting/) | Text input and display | Text fields, state binding |

## Intermediate

| Sample | Description | Concepts |
|--------|-------------|----------|
| [layouts](layouts/) | Box, row, column arrangements | Layout components, spacing |
| [lists](lists/) | Scrollable item lists | lazy-column, list-item |
| [interactive](interactive/) | Various input controls | Checkbox, switch, radio buttons |
| [navigation](navigation/) | Multi-screen app | Tab navigation, screen switching |
| [dialogs](dialogs/) | Modal dialogs and sheets | alert-dialog, bottom-sheet |
| [todo](todo/) | Task management app | CRUD operations, lists, state |

## Advanced

| Sample | Description | Concepts |
|--------|-------------|----------|
| [form-validation](form-validation/) | Input validation | Derived state, error handling |
| [derived-state](derived-state/) | Computed values | `derived`, dependency tracking |
| [new-components](new-components/) | Component showcase | Material Design 3 components |
| [database-crud](database-crud/) | Database operations | ORM, db-insert, db-query, db-update, db-delete |
| [database-transaction](database-transaction/) | Atomic operations | db-transaction, rollback |
| [database-migration](database-migration/) | Schema versioning | db-migrate, schema evolution |

## Utility

| Sample | Description | Concepts |
|--------|-------------|----------|
| [calc](calc/) | Calculator app | Complex state, expression evaluation |
| [update-app-demo](update-app-demo/) | Live code updates | Hot reload demonstration |

## Learning Path

1. **Start**: `hello-world` - understand basic structure
2. **State**: `counter` - learn state management
3. **Input**: `greeting` - handle user input
4. **Layout**: `layouts` - arrange components
5. **Lists**: `lists` and `todo` - work with collections
6. **Navigation**: `navigation` - build multi-screen apps
7. **Data**: `database-crud` - persist data

## Internal/Test Samples

The following samples in `internal/` are used for testing and development:

- `button-test` - Button component testing
- `database-benchmark` - Performance testing
- `database-test` - Database feature testing
- `orm-test-suite` - ORM integration tests
- `orm-simple-test` - Basic ORM testing
- `orm-mini-test` - Minimal ORM testing
- `test-card` - Card component testing
- `window-size-test` - Window sizing tests
- `shared-db-app1/2` - Shared database testing
- `tracing-viewer` - Debug tracing tool

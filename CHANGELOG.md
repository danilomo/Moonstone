# Changelog

All notable changes to Moonstone will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2025-01-01

### Added

#### Core Framework
- Scheme-based declarative UI using KleinLisp interpreter
- Reactive state management with `state`, `state-ref`, `state-set!`, `state-update!`
- Derived state with automatic dependency tracking via `derived`
- Cross-platform support for Desktop (Windows, macOS, Linux) and Android

#### Components (35 total)
- **Layout**: `box`, `column`, `row`, `surface`, `spacer`, `scaffold`
- **Display**: `text`, `icon`, `image`
- **Input**: `button`, `text-field`, `outlined-text-field`, `checkbox`, `switch`, `radio-button`
- **Lists**: `lazy-column`, `lazy-row`, `list-item`, `dynamic-list`
- **Navigation**: `top-app-bar`, `bottom-navigation`, `nav-item`
- **Dialogs**: `alert-dialog`, `bottom-sheet`, `snackbar`
- **Material Design 3**: `card`, `chip`, `slider`, `progress-indicator`, `fab`, `badge`, `divider`
- **Control Flow**: `switch-view`, `view`, `error-boundary`

#### Database ORM
- SQLite database support with cross-platform API
- Schema definition with typed columns and constraints
- CRUD operations: `db-insert`, `db-query`, `db-update`, `db-delete`
- Transaction support with `db-transaction`
- Schema migrations with `db-migrate`
- Foreign key relationships

#### Developer Tools
- Hot reload for instant code changes
- Debug mode with component inspector
- Debug overlay panel

#### Documentation
- Getting Started guide
- Component Reference (all 35 components)
- API Reference
- ORM Reference and Guide

#### Samples
- 28 sample applications covering basics to advanced features
- Categories: hello-world, counter, greeting, layouts, interactive, lists, navigation, dialogs, todo, form-validation, database operations

#### Code Quality
- ktlint integration for code style
- Detekt for static analysis
- CI/CD pipeline for linting

### Technical Details
- Built on Jetpack Compose Multiplatform 1.7.3
- Kotlin 2.1.0
- Material Design 3 theming
- Gradle 8.x build system

[Unreleased]: https://github.com/danilomo/Moonstone/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/danilomo/Moonstone/releases/tag/v0.1.0

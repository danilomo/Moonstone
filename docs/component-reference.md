# Component Reference

This document provides a complete reference for all Moonstone components.

## Table of Contents

- [Layout Components](#layout-components)
  - [box](#box)
  - [column](#column)
  - [row](#row)
  - [surface](#surface)
  - [spacer](#spacer)
  - [scaffold](#scaffold)
- [Display Components](#display-components)
  - [text](#text)
  - [icon](#icon)
- [Input Components](#input-components)
  - [button](#button)
  - [text-field](#text-field)
  - [outlined-text-field](#outlined-text-field)
  - [checkbox](#checkbox)
  - [switch](#switch)
  - [radio-button](#radio-button)
- [List Components](#list-components)
  - [lazy-column](#lazy-column)
  - [lazy-row](#lazy-row)
  - [list-item](#list-item)
- [Navigation Components](#navigation-components)
  - [top-app-bar](#top-app-bar)
  - [bottom-navigation](#bottom-navigation)
  - [nav-item](#nav-item)
- [Dialog Components](#dialog-components)
  - [alert-dialog](#alert-dialog)
  - [bottom-sheet](#bottom-sheet)
  - [snackbar](#snackbar)
- [Control Flow Components](#control-flow-components)
  - [switch-view](#switch-view)
  - [view](#view)
  - [error-boundary](#error-boundary)
- [Common Properties](#common-properties)

---

## Layout Components

### box

A container that can position a single child or stack multiple children.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:content-alignment` | symbol | `'top-start` | Child alignment |
| `#:padding` | number | - | Padding on all sides (dp) |
| `#:padding-horizontal` | number | - | Horizontal padding (dp) |
| `#:padding-vertical` | number | - | Vertical padding (dp) |
| `#:fill-max-size` | #t/#f | - | Fill available space |
| `#:fill-max-width` | #t/#f | - | Fill available width |
| `#:fill-max-height` | #t/#f | - | Fill available height |
| `#:width` | number | - | Fixed width (dp) |
| `#:height` | number | - | Fixed height (dp) |
| `#:background` | color | - | Background color |

**Content Alignment Values:**
`'center`, `'top-start`, `'top-center`, `'top-end`, `'center-start`, `'center-end`, `'bottom-start`, `'bottom-center`, `'bottom-end`

**Example:**

```scheme
(box
 #:fill-max-size #t
 #:content-alignment 'center
 #:background 'gray
 (text #:value "Centered content"))
```

---

### column

Arranges children vertically from top to bottom.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:spacing` | number | 0 | Space between children (dp) |
| `#:vertical-arrangement` | symbol | `'top` | Vertical arrangement |
| `#:horizontal-alignment` | symbol | `'start` | Horizontal alignment |
| `#:padding` | number | - | Padding (dp) |
| `#:fill-max-size` | #t/#f | - | Fill available space |

**Vertical Arrangement Values:**
`'top`, `'center`, `'bottom`, `'space-between`, `'space-around`, `'space-evenly`

**Horizontal Alignment Values:**
`'start`, `'center`, `'end`

**Example:**

```scheme
(column
 #:spacing 16
 #:padding 24
 #:horizontal-alignment 'center
 (text #:value "First")
 (text #:value "Second")
 (text #:value "Third"))
```

---

### row

Arranges children horizontally from start to end.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:spacing` | number | 0 | Space between children (dp) |
| `#:horizontal-arrangement` | symbol | `'start` | Horizontal arrangement |
| `#:vertical-alignment` | symbol | `'top` | Vertical alignment |
| `#:padding` | number | - | Padding (dp) |
| `#:fill-max-width` | #t/#f | - | Fill available width |

**Horizontal Arrangement Values:**
`'start`, `'center`, `'end`, `'space-between`, `'space-around`, `'space-evenly`

**Vertical Alignment Values:**
`'top`, `'center`, `'bottom`

**Example:**

```scheme
(row
 #:spacing 8
 #:vertical-alignment 'center
 (icon #:name "person")
 (text #:value "Username"))
```

---

### surface

A Material Design surface with elevation and shape.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:color` | color | - | Surface color |
| `#:shape` | symbol/number | `'rectangle` | Surface shape |
| `#:elevation` | number | 0 | Tonal elevation (dp) |

**Shape Values:**
`'rectangle`, `'rounded`, `'rounded-small`, `'rounded-medium`, `'rounded-large`, `'circle`, or a number for corner radius

**Example:**

```scheme
(surface
 #:color 'white
 #:shape 'rounded-large
 #:elevation 4
 #:padding 16
 (text #:value "Card content"))
```

---

### spacer

Adds empty space between elements.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:width` | number | - | Width (dp) |
| `#:height` | number | - | Height (dp) |

**Example:**

```scheme
(column
 (text #:value "Above")
 (spacer #:height 32)
 (text #:value "Below"))
```

---

### scaffold

Material Design scaffold with app bars and navigation.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:top-bar` | element | - | Top app bar |
| `#:bottom-bar` | element | - | Bottom navigation |
| `#:floating-action-button` | element | - | Floating action button |
| `#:floating-action-button-position` | symbol | `'end` | FAB position |

**FAB Position Values:**
`'end`, `'center`, `'end-overlay`

**Example:**

```scheme
(scaffold
 #:top-bar (top-app-bar #:title "My App")
 #:bottom-bar (bottom-navigation ...)
 (column
  #:padding 16
  (text #:value "Main content")))
```

---

## Display Components

### text

Displays text with styling.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:value` | string/state | **required** | Text to display |
| `#:style` | symbol | - | Typography style |
| `#:color` | color | - | Text color |
| `#:font-size` | number | - | Font size (sp) |
| `#:font-weight` | symbol | - | Font weight |
| `#:max-lines` | number | - | Maximum lines |

**Typography Styles:**
- Display: `'display-large`, `'display-medium`, `'display-small`
- Headline: `'headline-large`, `'headline-medium`, `'headline-small`
- Title: `'title-large`, `'title-medium`, `'title-small`
- Body: `'body-large`, `'body-medium`, `'body-small`
- Label: `'label-large`, `'label-medium`, `'label-small`

**Font Weight Values:**
`'thin`, `'light`, `'normal`, `'medium`, `'semi-bold`, `'bold`, `'extra-bold`

**Example:**

```scheme
(text #:value "Welcome!"
      #:style 'headline-large
      #:color "#1976D2"
      #:font-weight 'bold)
```

---

### icon

Displays a Material Design icon.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:name` | string | **required** | Icon name |
| `#:size` | number | 24 | Icon size (dp) |
| `#:tint` | color | - | Icon color |
| `#:on-click` | function | - | Click handler |
| `#:content-description` | string | - | Accessibility description |

**Available Icons:**
`"home"`, `"search"`, `"menu"`, `"settings"`, `"person"`, `"favorite"`, `"star"`, `"check"`, `"close"`, `"add"`, `"delete"`, `"edit"`, `"email"`, `"phone"`, `"notifications"`, `"warning"`, `"info"`, `"lock"`, `"done"`, `"arrow-back"`, `"arrow-forward"`, `"more-vert"`, `"refresh"`, `"share"`, `"visibility"`, `"visibility-off"`

**Example:**

```scheme
(icon #:name "favorite"
      #:size 32
      #:tint 'red
      #:on-click (lambda () (toggle-favorite)))
```

---

## Input Components

### button

A clickable button with multiple styles.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:on-click` | function | - | Click handler |
| `#:style` | symbol | `'filled` | Button style |
| `#:enabled` | #t/#f | 1 | Whether button is enabled |

**Style Values:**
`'filled`, `'outlined`, `'text`, `'elevated`, `'tonal`

**Example:**

```scheme
(button
 #:style 'outlined
 #:on-click (lambda () (submit-form))
 (text #:value "Submit"))
```

---

### text-field

A text input field.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:value` | state/string | **required** | Current value |
| `#:on-change` | function | - | Change handler (receives new value) |
| `#:label` | string | - | Floating label |
| `#:placeholder` | string | - | Placeholder text |
| `#:enabled` | #t/#f | 1 | Whether field is enabled |
| `#:single-line` | #t/#f | 1 | Single line input |
| `#:max-lines` | number | - | Maximum lines |
| `#:keyboard-type` | symbol | `'text` | Keyboard type |
| `#:fill-max-width` | #t/#f | - | Fill available width |

**Keyboard Types:**
`'text`, `'number`, `'email`, `'phone`, `'password`, `'uri`

**Example:**

```scheme
(define email (state ""))

(text-field
 #:value email
 #:label "Email"
 #:placeholder "you@example.com"
 #:keyboard-type 'email
 #:on-change (lambda (v) (state-set! email v)))
```

---

### outlined-text-field

A text field with an outlined border style.

**Props:** Same as `text-field`.

**Example:**

```scheme
(outlined-text-field
 #:value name
 #:label "Name"
 #:fill-max-width 1
 #:on-change (lambda (v) (state-set! name v)))
```

---

### checkbox

A checkbox toggle control.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:checked` | state/#t/#f | **required** | Checked state |
| `#:on-change` | function | - | Change handler (receives #t or #f) |
| `#:enabled` | #t/#f | 1 | Whether checkbox is enabled |

**Example:**

```scheme
(define agreed (state #f))

(checkbox
 #:checked agreed
 #:on-change (lambda (v) (state-set! agreed v))
 (text #:value "I agree to the terms"))
```

---

### switch

A toggle switch control.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:checked` | state/#t/#f | **required** | Checked state |
| `#:on-change` | function | - | Change handler (receives #t or #f) |
| `#:enabled` | #t/#f | 1 | Whether switch is enabled |

**Example:**

```scheme
(define dark-mode (state #f))

(switch
 #:checked dark-mode
 #:on-change (lambda (v) (state-set! dark-mode v))
 (text #:value "Dark Mode"))
```

---

### radio-button

A radio button for single selection from a group.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:selected` | state | **required** | State cell for selection |
| `#:value` | any | **required** | Value for this option |
| `#:on-select` | function | - | Selection handler |
| `#:enabled` | #t/#f | 1 | Whether button is enabled |

**Example:**

```scheme
(define size (state "medium"))

(column
 (radio-button
  #:selected size
  #:value "small"
  #:on-select (lambda () (state-set! size "small"))
  (text #:value "Small"))

 (radio-button
  #:selected size
  #:value "medium"
  #:on-select (lambda () (state-set! size "medium"))
  (text #:value "Medium"))

 (radio-button
  #:selected size
  #:value "large"
  #:on-select (lambda () (state-set! size "large"))
  (text #:value "Large")))
```

---

## List Components

### lazy-column

An efficient vertical scrolling list.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:spacing` | number | 0 | Space between items (dp) |
| `#:padding` | number | - | Content padding (dp) |
| `#:vertical-arrangement` | symbol | `'top` | Vertical arrangement |
| `#:horizontal-alignment` | symbol | `'start` | Horizontal alignment |

**Example:**

```scheme
(lazy-column
 #:spacing 8
 #:padding 16
 (list-item #:key "1" (text #:value "Item 1"))
 (list-item #:key "2" (text #:value "Item 2"))
 (list-item #:key "3" (text #:value "Item 3")))
```

---

### lazy-row

An efficient horizontal scrolling list.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:spacing` | number | 0 | Space between items (dp) |
| `#:padding` | number | - | Content padding (dp) |
| `#:horizontal-arrangement` | symbol | `'start` | Horizontal arrangement |
| `#:vertical-alignment` | symbol | `'top` | Vertical alignment |

**Example:**

```scheme
(lazy-row
 #:spacing 12
 (list-item #:key "a" (surface #:width 100 #:height 100 ...))
 (list-item #:key "b" (surface #:width 100 #:height 100 ...)))
```

---

### list-item

A wrapper for items in lazy lists. Provides a unique key for efficient updates.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:key` | string | **required** | Unique identifier |

**Example:**

```scheme
(list-item #:key "user-123"
  (row
   #:spacing 12
   (icon #:name "person")
   (text #:value "John Doe")))
```

---

## Navigation Components

### top-app-bar

A top application bar.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:title` | string/state | - | Title text |
| `#:style` | symbol | `'small` | Bar style |
| `#:navigation-icon` | element | - | Navigation icon (e.g., menu) |
| `#:actions` | element | - | Action icons (usually a row) |

**Style Values:**
`'small`, `'center-aligned`, `'medium`, `'large`

**Example:**

```scheme
(top-app-bar
 #:title "Messages"
 #:style 'center-aligned
 #:navigation-icon (icon #:name "menu" #:on-click open-drawer)
 #:actions (row
            (icon #:name "search" #:on-click open-search)
            (icon #:name "more-vert" #:on-click open-menu)))
```

---

### bottom-navigation

A bottom navigation bar.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:selected` | state | - | State for selected index |

**Example:**

```scheme
(define tab (state 0))

(bottom-navigation
 #:selected tab
 (nav-item #:icon "home" #:label "Home" #:value 0
           #:on-select (lambda () (state-set! tab 0)))
 (nav-item #:icon "search" #:label "Search" #:value 1
           #:on-select (lambda () (state-set! tab 1)))
 (nav-item #:icon "person" #:label "Profile" #:value 2
           #:on-select (lambda () (state-set! tab 2))))
```

---

### nav-item

A navigation item for bottom navigation.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:icon` | string | **required** | Icon name |
| `#:label` | string | **required** | Label text |
| `#:value` | any | **required** | Value to compare with selected |
| `#:on-select` | function | - | Selection handler |
| `#:enabled` | #t/#f | 1 | Whether item is enabled |

---

## Dialog Components

### alert-dialog

A modal alert dialog.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:visible` | state | **required** | Visibility state (#t/#f) |
| `#:title` | string | - | Dialog title |
| `#:text` | string | - | Dialog message |
| `#:icon` | element | - | Icon element |
| `#:confirm-button` | element | - | Confirm button |
| `#:dismiss-button` | element | - | Dismiss button |
| `#:on-dismiss` | function | - | Dismiss handler |

**Example:**

```scheme
(define show-dialog (state #f))

(alert-dialog
 #:visible show-dialog
 #:title "Delete Item"
 #:text "Are you sure you want to delete this item?"
 #:confirm-button (button
                   #:on-click (lambda () (delete-item) (state-set! show-dialog #f))
                   (text #:value "Delete"))
 #:dismiss-button (button
                   #:style 'text
                   #:on-click (lambda () (state-set! show-dialog #f))
                   (text #:value "Cancel"))
 #:on-dismiss (lambda () (state-set! show-dialog #f)))
```

---

### bottom-sheet

A modal bottom sheet.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:visible` | state | **required** | Visibility state |
| `#:on-dismiss` | function | - | Dismiss handler |
| `#:skip-partially-expanded` | #t/#f | 0 | Skip half-expanded state |

**Example:**

```scheme
(define show-sheet (state #f))

(bottom-sheet
 #:visible show-sheet
 #:on-dismiss (lambda () (state-set! show-sheet #f))
 (column
  #:padding 24
  #:spacing 16
  (text #:value "Select an option" #:style 'title-large)
  (button #:on-click option1 (text #:value "Option 1"))
  (button #:on-click option2 (text #:value "Option 2"))))
```

---

### snackbar

A brief notification message.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:visible` | state | **required** | Visibility state |
| `#:message` | string | **required** | Message text |
| `#:action-label` | string | - | Action button text |
| `#:on-action` | function | - | Action handler |
| `#:on-dismiss` | function | - | Dismiss handler |

**Example:**

```scheme
(define show-snackbar (state #f))

(snackbar
 #:visible show-snackbar
 #:message "Item deleted"
 #:action-label "Undo"
 #:on-action (lambda () (undo-delete) (state-set! show-snackbar #f))
 #:on-dismiss (lambda () (state-set! show-snackbar #f)))
```

---

## Control Flow Components

### switch-view

Conditionally renders one of multiple views based on state.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:selected` | state | **required** | Current selection state |

**Example:**

```scheme
(define tab (state 0))

(switch-view
 #:selected tab
 (view #:value 0 (home-screen))
 (view #:value 1 (search-screen))
 (view #:value 2 (profile-screen)))
```

---

### view

A view wrapper for switch-view children.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:value` | any | **required** | Value to match |

---

### error-boundary

Catches render errors and displays a fallback UI.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:fallback` | element | - | Fallback UI on error |
| `#:on-error` | function | - | Error handler |

**Example:**

```scheme
(error-boundary
 #:fallback (text #:value "Something went wrong")
 #:on-error (lambda (e) (log-error e))
 (risky-component))
```

---

## Common Properties

These properties are available on most layout components:

| Property | Type | Description |
|----------|------|-------------|
| `#:padding` | number | Padding on all sides (dp) |
| `#:padding-horizontal` | number | Horizontal padding (dp) |
| `#:padding-vertical` | number | Vertical padding (dp) |
| `#:fill-max-size` | #t/#f | Fill all available space |
| `#:fill-max-width` | #t/#f | Fill available width |
| `#:fill-max-height` | #t/#f | Fill available height |
| `#:width` | number | Fixed width (dp) |
| `#:height` | number | Fixed height (dp) |
| `#:background` | color | Background color |

### Color Values

Colors can be specified as:
- Named colors: `'red`, `'green`, `'blue`, `'white`, `'black`, `'gray`, `'cyan`, `'magenta`, `'yellow`, `'transparent`
- Hex strings: `"#FF5722"`, `"#80FF5722"` (with alpha)

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
  - [dynamic-list](#dynamic-list)
- [Navigation Components](#navigation-components)
  - [top-app-bar](#top-app-bar)
  - [bottom-navigation](#bottom-navigation)
  - [nav-item](#nav-item)
- [Material Design Components](#material-design-components)
  - [card](#card)
  - [chip](#chip)
  - [slider](#slider)
  - [progress-indicator](#progress-indicator)
  - [fab](#fab)
  - [badge](#badge)
  - [divider](#divider)
  - [image](#image)
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
| `#:on-start` | function | - | Called once when the scaffold first renders. Use for DB initialization, loading initial data. |
| `#:on-resume` | function | - | Called on every recomposition. Use for data refresh. |
| `#:on-close` | function | - | Called when the scaffold is disposed. Use for cleanup (close connections, save state). |

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

**Lifecycle hooks example:**

```scheme
(scaffold
 #:on-start (lambda ()
   (db-query load-data (lambda (rows err) ...)))
 #:on-close (lambda ()
   (db-close))
 #:top-bar (top-app-bar #:title "My App")
 (my-content))
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

*Navigation:* `"menu"`, `"home"`, `"arrow-back"` (or `"back"`), `"arrow-forward"` (or `"forward"`), `"close"`, `"more-vert"` (or `"more"`)

*Actions:* `"add"`, `"remove"`, `"delete"`, `"edit"`, `"search"`, `"settings"`, `"refresh"`, `"share"`, `"send"`, `"save"` (same as `"done"`)

*Communication:* `"email"` (or `"mail"`), `"phone"` (or `"call"`), `"message"` (or `"chat"`), `"notifications"`

*Content:* `"favorite"`, `"star"`, `"check"`, `"clear"`, `"info"`, `"warning"`

*Media:* `"play-arrow"` (or `"play"`)

*Social:* `"person"`, `"people"` (or `"group"`), `"account-circle"` (or `"account"`)

*Places:* `"location"` (or `"place"`)

*Misc:* `"lock"`, `"thumb-up"`, `"thumb-down"`, `"shopping-cart"` (or `"cart"`), `"done"`, `"keyboard-arrow-down"`, `"keyboard-arrow-up"`, `"keyboard-arrow-left"`, `"keyboard-arrow-right"`

**Note:** Icon names are case-insensitive. Unknown icon names default to `"info"`.

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

### dynamic-list

A state-driven list that automatically re-renders when items change. Ideal for rendering lists from state cells.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:items` | state | **required** | State cell containing list items |
| `#:render-item` | function | **required** | Function to render each item |
| `#:spacing` | number | 0 | Space between items (dp) |
| `#:vertical-arrangement` | symbol | `'top` | Vertical arrangement |
| `#:horizontal-alignment` | symbol | `'start` | Horizontal alignment |

**Example:**

```scheme
(define todos (state (list "Task 1" "Task 2" "Task 3")))

(define (render-todo todo)
  (surface #:padding 12 #:elevation 1
    (text #:value todo)))

(dynamic-list
 #:items todos
 #:render-item render-todo
 #:spacing 8)
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

## Material Design Components

### card

A Material Design card container for presenting content and actions.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:style` | symbol | `'filled` | Card style |
| `#:shape` | symbol/number | - | Card shape |
| `#:on-click` | function | - | Click handler |
| `#:enabled` | #t/#f | 1 | Whether card is enabled |
| `#:padding` | number | - | Content padding (dp) |

**Style Values:**
`'filled`, `'elevated`, `'outlined`

**Shape Values:**
`'rectangle`, `'rounded`, `'rounded-small`, `'rounded-medium`, `'rounded-large`, `'rounded-extra-large`, or a number for corner radius

**Example:**

```scheme
(card #:style 'elevated #:padding 16
  (column #:spacing 8
    (text #:value "Card Title" #:style 'title-medium)
    (text #:value "Card content goes here.")
    (button #:on-click action (text #:value "Action"))))
```

---

### chip

Material chip for filters, selections, and actions.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:label` | string | **required** | Chip label text |
| `#:style` | symbol | `'assist` | Chip style |
| `#:selected` | state/#t/#f | #f | Selected state (for filter chips) |
| `#:on-click` | function | - | Click handler |
| `#:on-select` | function | - | Select handler (for filter chips) |
| `#:on-dismiss` | function | - | Dismiss handler (for input chips) |
| `#:enabled` | #t/#f | 1 | Whether chip is enabled |

**Style Values:**
`'assist`, `'filter`, `'elevated-filter`, `'input`, `'suggestion`

**Example:**

```scheme
(define active (state #f))

(chip #:style 'filter
      #:label "Active"
      #:selected active
      #:on-select (lambda (v) (state-set! active (> v 0))))
```

---

### slider

A slider for selecting values from a continuous or discrete range.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:value` | state/number | **required** | Current value |
| `#:on-change` | function | - | Change handler (receives new value) |
| `#:min` | number | 0 | Minimum value |
| `#:max` | number | 1 | Maximum value |
| `#:steps` | number | 0 | Number of discrete steps |
| `#:enabled` | #t/#f | 1 | Whether slider is enabled |
| `#:fill-max-width` | #t/#f | - | Fill available width |

**Example:**

```scheme
(define volume (state 50))

(column #:spacing 8
  (text #:value (string-append "Volume: " (number->string (state-ref volume))))
  (slider #:value volume
          #:min 0
          #:max 100
          #:on-change (lambda (v) (state-set! volume v))
          #:fill-max-width #t))
```

---

### progress-indicator

Progress indicator for showing loading or progress states.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:style` | symbol | `'circular` | Indicator style |
| `#:value` | number/state | - | Progress value (0.0-1.0), omit for indeterminate |
| `#:color` | color | - | Indicator color |
| `#:track-color` | color | - | Track color |
| `#:stroke-width` | number | 4 | Stroke width (dp) for circular |
| `#:fill-max-width` | #t/#f | - | Fill available width (linear) |

**Style Values:**
`'circular`, `'linear`

**Example:**

```scheme
(define progress (state 0.3))

(column #:spacing 16
  (progress-indicator #:style 'circular)
  (progress-indicator #:style 'circular #:value progress)
  (progress-indicator #:style 'linear #:value progress #:fill-max-width #t))
```

---

### fab

Floating Action Button for primary actions.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:on-click` | function | **required** | Click handler |
| `#:style` | symbol | `'standard` | FAB size/style |
| `#:label` | string | - | Label text (for extended FAB) |
| `#:expanded` | #t/#f | 1 | Whether extended FAB shows text |
| `#:shape` | symbol/number | - | FAB shape |

**Style Values:**
`'standard`, `'small`, `'large`, `'extended`

**Example:**

```scheme
(fab #:on-click create-item
  (icon #:name "add"))

(fab #:style 'extended
     #:label "Create New"
     #:on-click create-item
  (icon #:name "add"))
```

---

### badge

Badge overlay for showing notifications, counts, or status.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:count` | number/state | - | Count to display (omit for dot badge) |
| `#:max-count` | number | 99 | Maximum count before showing "99+" |
| `#:color` | color | - | Badge background color |
| `#:content-color` | color | - | Badge text color |
| `#:visible` | #t/#f | 1 | Whether badge is visible |

**Example:**

```scheme
(define notifications (state 5))

(badge #:count notifications
  (icon #:name "notifications" #:size 32))

(badge (icon #:name "mail" #:size 32))
```

---

### divider

A visual divider for separating content.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:orientation` | symbol | `'horizontal` | Divider orientation |
| `#:thickness` | number | 1 | Divider thickness (dp) |
| `#:color` | color | - | Divider color |

**Orientation Values:**
`'horizontal`, `'vertical`

**Example:**

```scheme
(column #:spacing 8
  (text #:value "Item 1")
  (divider)
  (text #:value "Item 2")
  (divider #:thickness 2 #:color "blue")
  (text #:value "Item 3"))
```

---

### image

Image display component with shape and placeholder support.

**Props:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `#:source` | string | - | Image source path |
| `#:placeholder` | string | "Image" | Placeholder text |
| `#:content-description` | string | - | Accessibility description |
| `#:scale` | symbol | `'fit` | Content scaling mode |
| `#:shape` | symbol/number | - | Image shape |
| `#:width` | number | - | Image width (dp) |
| `#:height` | number | - | Image height (dp) |
| `#:size` | number | - | Image size (square, dp) |
| `#:on-click` | function | - | Click handler |

**Scale Values:**
`'crop`, `'fit`, `'fill-bounds`, `'fill-width`, `'fill-height`, `'inside`, `'none`

**Shape Values:**
`'rectangle`, `'circle`, `'rounded`, `'rounded-small`, `'rounded-medium`, `'rounded-large`, or a number for corner radius

**Example:**

```scheme
(row #:spacing 16
  (image #:placeholder "100x100" #:size 100 #:shape 'rectangle)
  (image #:placeholder "Circle" #:size 80 #:shape 'circle)
  (image #:placeholder "Avatar" #:width 120 #:height 80 #:shape 'rounded-large))
```

**Note:** This component currently shows placeholders. Platform-specific image loaders (like Coil) can be integrated for actual image loading.

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

## Common Patterns

### Pattern: Conditional Rendering

Use `if` or `switch-view` to conditionally render components based on state.

**Using if:**

```scheme
(define logged-in (state #f))

(if (state-ref logged-in)
    (text #:value "Welcome back!")
    (button #:on-click login (text #:value "Login")))
```

**Using switch-view:**

```scheme
(define view-mode (state 'list))

(switch-view #:selected view-mode
  (view #:value 'list (list-view-component))
  (view #:value 'grid (grid-view-component))
  (view #:value 'table (table-view-component)))
```

---

### Pattern: List Rendering

Use `lazy-column` with `map` to render lists of data.

**Static list with map:**

```scheme
(define items (list "Apple" "Banana" "Cherry"))

(lazy-column #:spacing 8
  (map (lambda (item)
         (list-item #:key item
           (text #:value item)))
       items))
```

**Dynamic list with state:**

```scheme
(define todos (state (list "Task 1" "Task 2" "Task 3")))

(dynamic-list
 #:items todos
 #:render-item (lambda (todo)
                 (surface #:padding 12 #:elevation 1
                   (text #:value todo)))
 #:spacing 8)
```

---

### Pattern: Error Handling

Handle errors in callbacks and display error states.

**Error state pattern:**

```scheme
(define error-msg (state #f))
(define loading (state #f))

(define (fetch-data)
  (state-set! loading #t)
  (state-set! error-msg #f)
  (http-get "/api/data"
    (lambda (result error)
      (state-set! loading #f)
      (if error
          (state-set! error-msg error)
          (process-result result)))))

(column #:spacing 16
  (button #:on-click fetch-data (text #:value "Load Data"))

  (if (state-ref loading)
      (progress-indicator)
      (if (state-ref error-msg)
          (text #:value (state-ref error-msg) #:color 'red)
          (data-view))))
```

**Error boundary:**

```scheme
(error-boundary
 #:fallback (column #:padding 16
              (icon #:name "warning" #:size 48 #:tint 'red)
              (text #:value "Something went wrong" #:style 'headline-small))
 #:on-error (lambda (e) (log-error e))
 (risky-component-tree))
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

# Building Real Apps with Moonstone

This guide walks through the patterns you need to build a complete, production-quality app. It is based on `samples/daily-metrics/app.scm` — a working app that demonstrates all these patterns together. Read this after the [Getting Started](getting-started.md) tutorial and the [Component Reference](component-reference.md).

---

## 1. Project Structure

### app.conf

Place an `app.conf` file in the same folder as your `.scm` file to configure the app:

```ini
[app]
name = Daily Metrics
window-width = 480
window-height = 720
db-location = daily-metrics.db
```

Every key becomes a `*key-name*` variable in Lisp automatically:

```scheme
(println "DB path:" *db-location*)
(println "Window width:" *window-width*)
```

### Organizing a multi-view app

For apps with more than one screen, use a single `.scm` file with view-rendering functions. The top of the file is for:
1. DB schema declarations (`db-table`, `db-query`)
2. State definitions
3. Helper/validation functions
4. View functions (one per screen)
5. The `app` entry point at the bottom

```scheme
; --- schema ---
(db-table items ...)
(db-query all-items ...)

; --- state ---
(define current-tab (state 0))
(define items-list (state '()))

; --- helpers ---
(define (load-items) ...)

; --- views ---
(define (home-screen) ...)
(define (history-screen) ...)

; --- entry point ---
(define (app) ...)
```

---

## 2. App Initialization

### Why `app` runs on every recompose

The `app` function is called on every recompose — which happens whenever any state changes. Putting a side effect like a database query directly in `app` creates an infinite loop: the query updates state, state triggers a recompose, recompose calls `app`, which fires the query again.

```scheme
; WRONG — creates an infinite loop
(define (app)
  (load-data-from-db)  ; runs on every state change!
  (column ...))
```

### Using `#:on-start` to run code once

Pass an initialization function to `scaffold`'s `#:on-start` hook. It runs exactly once, when the component first appears.

```scheme
(define data (state '()))
(define loaded? (state #f))

(define (init-app)
  (load-data)
  (state-set! loaded? #t))

(define (load-data)
  (my-query
    (lambda (rows err)
      (if (not err)
          (state-set! data rows)))))

(define (app)
  (scaffold
    #:on-start init-app
    (if (state-ref loaded?)
        (column ...)          ; show content
        (progress-indicator)) ; show spinner while loading
    ))
```

### DB table creation and initial data loading

If your app uses a database, create tables and load initial data inside `#:on-start`. The DB is available by the time the hook runs.

```scheme
(define today-date (state ""))
(define today-entry (state #f))

(define (init-app)
  (let ((today (current-date-string)))
    (state-set! today-date today)
    (find-today-entry
      (lambda (result err)
        (if (not err)
            (state-set! today-entry result)))
      #:date today)))
```

---

## 3. Database Patterns

### Schema definition

Declare tables at the top level of your script. `#:serial` creates an auto-incrementing integer primary key.

```scheme
(db-table entries
  (id #:serial)
  (date #:string #:not-null #:unique)
  (weight #:real)
  (notes #:text))
```

Column type keywords: `#:string`, `#:integer`, `#:real`, `#:boolean`, `#:text`. Constraints: `#:not-null`, `#:unique`, `#:default value`.

### db-query vs. db-query-single

`db-query` declares a named function that returns all matching rows as a list.
`db-query-single` returns the first matching row, or `#f` if no rows match.

```scheme
; Returns a list of rows (possibly empty)
(db-query all-entries-sorted
  #:from entries
  #:order-by (date #:desc))

; Returns one row or #f
(db-query-single find-by-date
  #:from entries
  #:where (= date ?date)
  #:params (date))
```

Call them like functions, passing a callback as the first argument. Query parameters follow as keyword arguments:

```scheme
; No parameters
(all-entries-sorted
  (lambda (rows err) ...))

; With a parameter
(find-by-date
  (lambda (result err) ...)
  #:date "2026-04-29")
```

### Callback signature: result is first

**All DB callbacks use `(result error)` order — result comes first, error second.** This is easy to get backwards. When there is no error, `error` is `#f`. When there is an error, `error` is a string message and `result` is `#f`.

```scheme
(my-query
  (lambda (rows error)      ; result first, error second
    (if (not error)
        (state-set! data rows)
        (println "Query failed:" error))))
```

Callback shapes by operation:

| Operation | Callback |
|-----------|---------|
| `db-query` | `(lambda (rows error) ...)` — rows is a list |
| `db-query-single` | `(lambda (row error) ...)` — row is a p-map or `#f` |
| `db-insert` | `(lambda (id error) ...)` — id is the new row's integer ID |
| `db-update` | `(lambda (count error) ...)` — count is rows affected |
| `db-delete` | `(lambda (count error) ...)` — count is rows deleted |

### Inserting with p-map

```scheme
(db-insert entries
  #:values (p-map #:date "2026-04-29"
                  #:weight 75.5
                  #:notes "Feeling good")
  (lambda (id err)
    (if (not err)
        (println "Inserted row with id:" id))))
```

### Updating with parameter binding

Use `?name` placeholders in `#:where` and supply values as keyword arguments after the callback:

```scheme
(db-update entries
  #:set (p-map #:weight 76.0 #:notes "Updated")
  #:where (= id ?id)
  #:id entry-id
  (lambda (count err)
    (if (not err)
        (println "Updated" count "rows"))))
```

### Accessing row columns

Use `p-map-get` with a keyword matching the column name:

```scheme
(lambda (result err)
  (if (not err)
      (let ((entry-id   (p-map-get result #:id))
            (entry-date (p-map-get result #:date))
            (weight     (p-map-get result #:weight)))
        ...)))
```

### Handling nullable columns

Columns without `#:not-null` may contain `db-null`. Always check before using the value:

```scheme
(let ((weight (p-map-get row #:weight)))
  (if (db-null? weight)
      "(not recorded)"
      (number->string weight)))
```

---

## 4. State Management Patterns

### One state cell per UI concern

Define separate state cells for each independent piece of UI state. Avoid putting everything in one map — it makes updates harder and recompose less efficient.

```scheme
; Good — separate cells
(define weight-input  (state ""))
(define weight-error  (state ""))
(define entries-list  (state '()))
(define current-tab   (state 0))

; Avoid — one big blob
(define app-state (state (list "" "" '() 0)))
```

### Derived state for computed values

Use `derived` when a value is always a pure function of other state. It automatically recomputes when its dependencies change.

```scheme
(define entry-count (derived (lambda () (length (state-ref entries-list)))))

; Use like any state cell
(text #:value entry-count)
```

**When NOT to use derived:** if the computation is expensive and runs on every recompose, it can hurt performance. For cheap string/arithmetic operations it is fine.

### Passing state to view functions

Pass state cells as arguments to view functions rather than relying on globals. This makes views easier to test and reuse.

```scheme
(define (entry-card entry-state on-edit)
  (card #:padding 16
    (text #:value (state-ref entry-state))
    (button #:on-click on-edit
      (text #:value "Edit"))))
```

---

## 5. Navigation

### Tab-based navigation

Use `scaffold` with `#:bottom-bar` for tab navigation. The selected tab is controlled by a state cell.

```scheme
(define current-tab (state 0))

(define (app)
  (scaffold
    #:on-start init-app
    #:top-bar (top-app-bar #:title "My App" #:style 'center-aligned)
    #:bottom-bar (bottom-navigation #:selected current-tab
      (nav-item #:icon "home"   #:label "Home"    #:value 0
        #:on-select (lambda () (state-set! current-tab 0)))
      (nav-item #:icon "list"   #:label "History" #:value 1
        #:on-select (lambda () (state-set! current-tab 1))))
    (switch-view #:selected current-tab
      (view #:value 0 (home-screen))
      (view #:value 1 (history-screen)))))
```

### Switching views with state

For sub-views within a tab (e.g., entry form → confirmation screen → edit form), use a state cell holding a symbol:

```scheme
(define view-mode (state 'entry))  ; 'entry | 'saved | 'edit

(define (home-screen)
  (let ((mode (state-ref view-mode)))
    (cond
      ((eq? mode 'entry) (entry-form))
      ((eq? mode 'saved) (saved-view))
      ((eq? mode 'edit)  (edit-form)))))
```

### Switching to a tab from code

Set the tab state directly in any callback:

```scheme
(define (after-save)
  (load-history)
  (state-set! current-tab 1))  ; jump to History tab
```

---

## 6. Form Validation

### Validate before writing to DB

Always validate inputs before firing a DB write. Return a boolean from your validation function and only proceed when it returns `#t`.

```scheme
(define weight-error (state ""))

(define (validate-form)
  (state-set! weight-error "")          ; clear previous errors
  (let ((valid #t)
        (w (state-ref weight-input)))
    (if (string=? w "")
        (begin (state-set! weight-error "Weight is required") (set! valid #f))
        (if (not (string->number w))
            (begin (state-set! weight-error "Must be a valid number") (set! valid #f))))
    valid))

(define (save-entry)
  (if (validate-form)
      (db-insert ...)))
```

### Inline error messages

Show error text below each field. Use `(spacer #:height 0)` as a no-op placeholder to keep layout stable when there is no error.

```scheme
(outlined-text-field
  #:value weight-input
  #:label "Weight (kg)"
  #:keyboard-type 'number
  #:on-change (lambda (v)
    (state-set! weight-input v)
    (state-set! weight-error "")))   ; clear error on edit

(if (not (string=? (state-ref weight-error) ""))
    (text #:value (state-ref weight-error) #:color 'error #:size 'small)
    (spacer #:height 0))
```

### Number parsing

`string->number` returns the number on success and `#f` on failure. Use this to validate numeric inputs:

```scheme
(define (valid-number? str)
  (if (string=? str "")
      #t                          ; empty is allowed (optional field)
      (not (not (string->number str)))))

(define (parse-optional-number str)
  (if (string=? str "") #f (string->number str)))
```

---

## 7. Error Handling

### DB callback error branch

Don't silently swallow errors with `(begin)`. At minimum, log them. In production UI, surface them to the user.

```scheme
; Minimal — log to stdout during development
(my-query
  (lambda (rows error)
    (if error
        (println "Query error:" error)
        (state-set! data rows))))

; Better — show error in UI
(define db-error (state ""))

(my-query
  (lambda (rows error)
    (if error
        (state-set! db-error error)
        (begin
          (state-set! db-error "")
          (state-set! data rows)))))

; In the view
(if (not (string=? (state-ref db-error) ""))
    (text #:value (state-ref db-error) #:color 'error)
    (spacer #:height 0))
```

### Distinguishing "no rows" from "query error"

For `db-query-single`, `result` is `#f` for both "no rows found" and "query error". Check `error` first to tell them apart:

```scheme
(find-by-date
  (lambda (result error)
    (cond
      (error           (println "DB error:" error))
      ((not result)    (state-set! view-mode 'entry))   ; no row for this date
      (else            (populate-form result)            ; row found
                       (state-set! view-mode 'saved))))
  #:date today)
```

### Showing errors with a snackbar

```scheme
(define error-message (state ""))

; trigger in a callback:
(state-set! error-message "Failed to save. Please try again.")

; in the view:
(scaffold
  #:snackbar (if (not (string=? (state-ref error-message) ""))
                 (snackbar #:message error-message
                           #:on-dismiss (lambda () (state-set! error-message "")))
                 #f)
  ...)
```

---

## 8. Debugging Tips

### Tracing state with println

`println` writes to stdout and is the simplest way to debug:

```scheme
(println "view-mode is:" (state-ref view-mode))
(println "entries count:" (length (state-ref entries-list)))
```

`print` outputs without a newline; `display` is an alias for `println`.

### Hot reload during development

Pass `--hot-reload` (or `-w`) so the app reloads the script on every save, without restarting the JVM:

```bash
./gradlew :desktop:run --args="--hot-reload my-app.scm"
```

Combine with `--debug` for the error panel:

```bash
./gradlew :desktop:run --args="-d -w my-app.scm"
```

### Commenting out sections

Standard Scheme line comments work — use `;` to narrow down a problem:

```scheme
; (load-history)   ; temporarily disabled to isolate bug
(init-app)
```

### Building incrementally

Start with hardcoded data, verify the UI renders correctly, then wire up the DB:

```scheme
; Step 1 — hardcoded
(define entries-list (state (list
  (p-map #:id 1 #:date "2026-04-29" #:weight 75.5)
  (p-map #:id 2 #:date "2026-04-28" #:weight 75.2))))

; Step 2 — replace with real query once layout is right
(define (load-entries)
  (all-entries-sorted
    (lambda (rows err)
      (if (not err) (state-set! entries-list rows)))))
```

---

## Complete Example

The `samples/daily-metrics/app.scm` application combines all of these patterns in a single working file (~327 lines). It covers:

- `app.conf` for window size and DB location
- `#:on-start` initialization with date detection and DB load
- `db-query` / `db-query-single` with parameterized queries
- Insert-or-update logic using query result to decide which DB call to make
- Nullable column handling with `db-null?`
- Multi-field form validation with inline error display
- Tab navigation between Home and History screens
- Sub-view switching with a `view-mode` state symbol
- Historical entry editing that navigates back to the entry tab

Run it with:

```bash
./gradlew :desktop:run --args="samples/daily-metrics/app.scm"
```

# Moonstone Common Pitfalls & Troubleshooting

Quick reference for issues that frequently cost developers time. Each entry shows the wrong pattern, the right pattern, and why it matters.

---

## 1. App function runs on every recompose — side effects cause infinite loops

```scheme
; WRONG — runs on every state change, causes infinite loop
(define (app)
  (load-data!)
  (scaffold ...))

; RIGHT — #:on-start runs once, on first render
(define (app)
  (scaffold
    #:on-start load-data!
    ...))
```

**Why:** Compose re-calls `app` whenever any state changes. `#:on-start` uses `LaunchedEffect(Unit)` internally, which runs exactly once.

---

## 2. DB callbacks are `(result error)` — result comes first

```scheme
; WRONG — Node.js-style error-first
(my-query (lambda (err result) ...))

; RIGHT — Moonstone is result-first
(my-query (lambda (result err) ...))
```

**Why:** Moonstone's convention is result-first. Swapping the arguments silently processes the error value as data and ignores the real result.

---

## 3. DB inserts require `p-map` wrapper for values

```scheme
; WRONG — bare keyword args are not valid here
(db-insert entries
  #:date today
  #:weight 75.5
  callback)

; RIGHT — values must be wrapped in p-map
(db-insert entries
  #:values (p-map #:date today #:weight 75.5)
  callback)
```

---

## 4. `p-map-get` requires a keyword argument, not a symbol

```scheme
; WRONG — silently returns wrong value
(p-map-get row 'id)

; RIGHT
(p-map-get row #:id)
```

**Why:** `'id` is a quoted symbol; `#:id` is a keyword. They are different types and the lookup will fail silently with a symbol.

---

## 5. `#:where` parameter binding uses `?name` syntax

```scheme
; WRONG — entry-id is treated as a column reference, not a variable
(db-update entries
  #:set (p-map #:weight 75.5)
  #:where (= id entry-id)
  callback)

; RIGHT — use ?name placeholder and pass the value as a keyword arg
(db-update entries
  #:set (p-map #:weight 75.5)
  #:where (= id ?id)
  #:id entry-id
  callback)
```

**Why:** Bare names in `#:where` are column references. Use `?param-name` for runtime values, then supply the value as `#:param-name`.

---

## 6. Booleans: only `#t` and `#f` — not `true`/`false`

```scheme
; WRONG — true/false are identifiers (variable names), not booleans
(if true ...)
(define flag false)

; RIGHT
(if #t ...)
(define flag #f)
```

**Why:** `true` and `false` parse as unbound variable names. This causes silent failures or runtime errors rather than obvious syntax errors.

---

## 7. String comparison: use `string=?` not `=`

```scheme
; WRONG — = is numeric comparison only
(if (= name "Alice") ...)

; RIGHT
(if (string=? name "Alice") ...)
; Also available: string<?, string>?, string-contains?
```

---

## 8. Null checks: use `db-null?`

```scheme
; WRONG — will crash or produce wrong results
(if (not value) ...)

; RIGHT
(if (db-null? value)
    ""
    (number->string value))
```

**Why:** SQL NULL is not the same as `#f` or `0`. Only `db-null?` correctly identifies a NULL value from a query result.

---

## 9. `println` and `print` are available for debugging

```scheme
; These work — use them freely
(println "debug:" (state-ref my-state))
(print "value: ")
(println some-value)
```

`display` is an alias for `println`. Output goes to stdout.

---

## 10. Semicolons work as comments

```scheme
; This is a valid comment
(define x 42) ; inline comment also works
```

Standard Scheme comment syntax has been supported since March 2026.

---

## 11. Hot reload avoids full Gradle restarts

```bash
# Slow — restarts the JVM on every run
./gradlew :desktop:run --args="my-app.scm"

# Fast — watches for file changes, reloads automatically
./gradlew :desktop:run --args="--hot-reload my-app.scm"
# or: --args="-w my-app.scm"
```

**Why:** A full Gradle restart takes 5–15 seconds. Hot reload reloads in under a second by keeping the JVM alive and re-evaluating only the Scheme script.

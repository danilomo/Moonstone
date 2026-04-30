# KleinLisp ORM Reference

Complete API reference for the SQLite ORM in Moonstone applications.

> **📚 New to the ORM?** See [ORM Guide](orm-guide.md) for tutorials and best practices.

## Table of Contents

1. [Quick Reference](#quick-reference)
2. [Table Definitions](#table-definitions)
3. [Query Functions](#query-functions)
4. [Data Manipulation](#data-manipulation)
5. [Transactions](#transactions)
6. [Raw SQL](#raw-sql)
7. [Migrations](#migrations)
8. [Working with Results](#working-with-results)
9. [Error Handling](#error-handling)
10. [Complete API Index](#complete-api-index)

---

## Quick Reference

### Column Types

| Type | SQLite | Scheme Type | Description |
|------|--------|-------------|-------------|
| `#:serial` | INTEGER PRIMARY KEY AUTOINCREMENT | int | Auto-increment ID |
| `#:int` | INTEGER | int | 32-bit integer |
| `#:long` | INTEGER | int | 64-bit integer |
| `#:string` | TEXT | string | Text string |
| `#:text` | TEXT | string | Alias for string |
| `#:real` | REAL | number | Floating point |
| `#:boolean` | INTEGER | boolean | 0/1 → #f/#t |
| `#:timestamp` | INTEGER | int | Unix timestamp (ms) |
| `#:blob` | BLOB | string | Base64 binary |

### Constraints

| Constraint | Example |
|------------|---------|
| `#:pk` | `(id #:int #:pk)` |
| `#:not-null` | `(name #:string #:not-null)` |
| `#:unique` | `(email #:string #:unique)` |
| `#:default value` | `(active #:boolean #:default #t)` |
| `#:default 'now` | `(created-at #:timestamp #:default 'now)` |
| `#:size n` | `(title #:string #:size 200)` |
| `#:references table` | `(user-id #:long #:references users)` |

### Core Functions

| Function | Purpose |
|----------|---------|
| `db-table` | Define table schema |
| `db-query` | Define multi-row query |
| `db-query-single` | Define single-row query |
| `db-insert` | Insert row(s) |
| `db-update` | Update rows |
| `db-delete` | Delete rows |
| `db-count` | Count rows |
| `db-transaction` | Atomic operations |
| `db-execute` | Raw SELECT |
| `db-execute-update` | Raw INSERT/UPDATE/DELETE |
| `db-migrate` | Schema migration |

---

## Table Definitions

### db-table

Define a database table schema.

**Syntax:**
```scheme
(db-table table-name
  (column-name #:type constraint1 constraint2 ...)
  ...)
```

**Example:**
```scheme
(db-table users
  (id #:serial)
  (name #:string #:not-null)
  (email #:string #:unique)
  (age #:int)
  (active #:boolean #:default #t)
  (created-at #:timestamp #:default 'now))

(db-table orders
  (id #:serial)
  (user-id #:long #:references users #:not-null)
  (total #:real #:not-null)
  (status #:string #:default "pending")
  (ordered-at #:timestamp #:default 'now))
```

**Notes:**
- Scheme names with hyphens (e.g., `user-id`) → SQL underscores (e.g., `user_id`)
- Results returned with hyphenated names
- Define all tables before queries
- Tables are created/updated automatically

---

## Query Functions

### db-query

Define a reusable multi-row query function.

**Syntax:**
```scheme
(db-query function-name
  #:from table-name
  #:columns (col1 col2 ...)     ; Optional, default: all
  #:join (table #:on condition) ; Optional INNER JOIN
  #:left-join (table #:on cond) ; Optional LEFT JOIN
  #:where condition             ; Optional filter
  #:order-by (col #:asc ...)    ; Optional ordering
  #:limit n                     ; Optional row limit
  #:params (param1 param2 ...)) ; Required parameters
```

**Example:**
```scheme
(db-query active-users
  #:from users
  #:where (= active #t)
  #:order-by (name #:asc))

(db-query user-orders
  #:from orders
  #:join (users #:on (= orders.user-id users.id))
  #:columns (orders.id users.name orders.total)
  #:where (= orders.user-id ?uid)
  #:params (uid))

;; Usage:
(active-users (lambda (rows error)
  (if error
      (handle-error error)
      (process-users rows))))

(user-orders #:uid 123 (lambda (rows error) ...))
```

---

### db-query-single

Define a single-row query. Returns one p-map or `#f` if not found.

**Syntax:**
```scheme
(db-query-single function-name
  #:from table-name
  #:where condition
  #:params (param-names ...))
```

**Example:**
```scheme
(db-query-single find-user-by-id
  #:from users
  #:where (= id ?id)
  #:params (id))

;; Usage:
(find-user-by-id #:id 1 (lambda (user error)
  (if error
      (println error)
      (if user
          (println (p-map-get user #:name))
          (println "Not found")))))
```

---

### Condition Syntax

**Comparison:**
```scheme
(= column value)        ; Equal
(<> column value)       ; Not equal
(!= column value)       ; Not equal (alias)
(< column value)        ; Less than
(> column value)        ; Greater than
(<= column value)       ; Less/equal
(>= column value)       ; Greater/equal
```

**Logical:**
```scheme
(and cond1 cond2 ...)   ; All true
(or cond1 cond2 ...)    ; Any true
(not condition)         ; Negate
```

**Special:**
```scheme
(like column "%pattern%")       ; Pattern matching
(in column (val1 val2 val3))    ; Value in list
(in column ?param)              ; Value in param list
(is-null column)                ; NULL check
(is-not-null column)            ; NOT NULL check
(between column low high)       ; Range check
```

**Parameters:**
```scheme
;; Use ?param-name to reference parameters
(db-query find-by-status
  #:from orders
  #:where (and (= user-id ?uid) (= status ?status))
  #:params (uid status))

;; Usage:
(find-by-status #:uid 1 #:status "pending" callback)
```

---

### db-count

Count rows matching a condition.

**Syntax:**
```scheme
(db-count table-name
  #:where condition  ; Optional
  param-bindings     ; If using ?params in where
  callback)
```

**Example:**
```scheme
;; Count all
(db-count users (lambda (count error) ...))

;; Count with condition
(db-count users
  #:where (= active #t)
  (lambda (count error) ...))

;; Count with parameters
(db-count orders
  #:where (= user-id ?uid)
  #:uid 1
  (lambda (count error) ...))
```

---

## Data Manipulation

### db-insert

Insert one or more rows.

**Syntax:**
```scheme
;; Single row
(db-insert table-name
  #:values (p-map #:col1 val1 #:col2 val2 ...)
  callback)

;; Multiple rows
(db-insert table-name
  #:values (list (p-map ...) (p-map ...) ...)
  callback)
```

**Example:**
```scheme
;; Single insert
(db-insert users
  #:values (p-map #:name "Alice" #:email "alice@example.com")
  (lambda (id error)
    (if error
        (println error)
        (println (string-append "ID: " (number->string id))))))

;; Batch insert
(db-insert users
  #:values (list
    (p-map #:name "Alice" #:email "alice@example.com")
    (p-map #:name "Bob" #:email "bob@example.com"))
  (lambda (ids error)
    (println (string-append "Inserted " (number->string (length ids)) " rows"))))
```

**Callback:** `(lambda (id-or-ids error) ...)`
- Single insert: `id` is the new row ID (number)
- Batch insert: `ids` is a list of new row IDs

---

### db-update

Update rows matching a condition.

**Syntax:**
```scheme
(db-update table-name
  #:set (p-map #:col1 new-val1 #:col2 new-val2 ...)
  #:where condition
  param-bindings  ; If using ?params
  callback)
```

**Example:**
```scheme
(db-update users
  #:set (p-map #:active #f)
  #:where (= id ?id)
  #:id 1
  (lambda (count error)
    (println (string-append "Updated " (number->string count) " rows"))))
```

**Callback:** `(lambda (affected-count error) ...)`
- `affected-count`: Number of rows updated

---

### db-delete

Delete rows matching a condition.

**Syntax:**
```scheme
;; Delete with condition
(db-delete table-name
  #:where condition
  param-bindings
  callback)

;; Delete all (requires explicit flag)
(db-delete table-name
  #:all #t
  callback)
```

**Example:**
```scheme
(db-delete users
  #:where (= id ?id)
  #:id 1
  (lambda (count error)
    (println (string-append "Deleted " (number->string count) " rows"))))

;; Delete all inactive users
(db-delete users
  #:where (= active #f)
  (lambda (count error) ...))
```

**Callback:** `(lambda (deleted-count error) ...)`

---

## Transactions

Execute multiple operations atomically with automatic rollback on failure.

### db-transaction

**Syntax:**
```scheme
(db-transaction
  (lambda (tx)
    ;; Use tx-* functions (synchronous)
    ;; Return #t to commit, #f to rollback
    #t)
  callback)
```

### Transaction Functions

All `tx-*` functions are **synchronous** inside the transaction:

| Function | Returns |
|----------|---------|
| `(tx-insert tx table #:values p-map)` | Inserted ID (number) |
| `(tx-update tx table #:set p-map #:where cond)` | Affected count |
| `(tx-delete tx table #:where cond)` | Deleted count |
| `(tx-query tx table #:where cond ...)` | List of p-maps |
| `(tx-query-single tx table #:where cond)` | p-map or #f |

**Example:**
```scheme
(db-transaction
  (lambda (tx)
    ;; Create user
    (define user-id (tx-insert tx users
      #:values (p-map #:name "Alice" #:email "alice@example.com")))

    ;; Create related data
    (tx-insert tx user-settings
      #:values (p-map #:user-id user-id #:theme "dark"))

    ;; Verify data
    (define user (tx-query-single tx users #:where (= id user-id)))

    ;; Commit
    #t)
  (lambda (success error)
    (if error
        (println (string-append "Failed: " error))
        (println "Transaction committed"))))
```

**Automatic Rollback:**
- Lambda returns `#f`
- Exception is thrown
- Any `tx-*` operation fails

---

## Raw SQL

### db-execute

Execute raw SELECT queries.

**Syntax:**
```scheme
(db-execute sql-string
  #:params (param-list)  ; Optional
  callback)
```

**Example:**
```scheme
(db-execute "SELECT * FROM users WHERE age > ?"
  #:params (list 18)
  (lambda (rows error)
    (if error
        (println error)
        (for-each (lambda (row) (println row)) rows))))
```

**Callback:** `(lambda (rows error) ...)`
- `rows`: List of p-maps

---

### db-execute-update

Execute raw INSERT/UPDATE/DELETE queries.

**Syntax:**
```scheme
(db-execute-update sql-string
  #:params (param-list)  ; Optional
  callback)
```

**Example:**
```scheme
(db-execute-update "UPDATE users SET active = ? WHERE id = ?"
  #:params (list #f 1)
  (lambda (affected-count error)
    (println (string-append "Updated " (number->string affected-count) " rows"))))
```

**Callback:** `(lambda (affected-count error) ...)`

---

## Migrations

### db-migrate

Run schema migrations with version tracking.

**Syntax:**
```scheme
(db-migrate version
  sql-statement-1
  sql-statement-2
  ...
  callback)
```

**Example:**
```scheme
(db-migrate 2
  "ALTER TABLE users ADD COLUMN verified INTEGER DEFAULT 0"
  "CREATE INDEX idx_users_verified ON users(verified)"
  (lambda (success error)
    (if error
        (println (string-append "Migration failed: " error))
        (println "Migration to version 2 complete"))))
```

**Notes:**
- Migrations run only once per version
- Version stored in `schema_version` table
- Migrations run in transaction (atomic)
- Failed migrations prevent version increment

**Version Check:**
```scheme
;; Check current schema version
(db-execute "SELECT version FROM schema_version LIMIT 1"
  (lambda (rows error)
    (if (and (not error) (not (null? rows)))
        (println (p-map-get (car rows) #:version))
        (println "No version"))))
```

---

## Working with Results

### Result Types

**Single Query:** Returns p-map or `#f`
```scheme
(find-user #:id 1 (lambda (user error)
  (if user
      (p-map-get user #:name)  ; Returns "Alice"
      (println "Not found"))))
```

**Multi Query:** Returns list of p-maps
```scheme
(all-users (lambda (rows error)
  (for-each (lambda (row)
    (println (p-map-get row #:name)))
    rows)))
```

### p-map Functions

**Access values:**
```scheme
(p-map-get row #:column-name)          ; Get single value
(p-map-get row #:column-name default)  ; With default if NULL
```

**Check for NULL:**
```scheme
(db-null? (p-map-get row #:column))  ; Returns #t if NULL
```

**Iterate:**
```scheme
(for-each (lambda (row)
  (define name (p-map-get row #:name))
  (define email (p-map-get row #:email))
  (println (string-append name ": " email)))
  rows)
```

**Map:**
```scheme
(define names (map (lambda (row) (p-map-get row #:name)) rows))
```

**Filter:**
```scheme
(define adults (filter
  (lambda (row) (>= (p-map-get row #:age) 18))
  rows))
```

---

## Error Handling

All async functions use the callback pattern: `(lambda (result error) ...)` — **result first, error second**. This is the opposite of Node.js convention. See the [Troubleshooting Guide](troubleshooting.md#2-db-callbacks-are-result-error--result-comes-first) if callbacks are returning unexpected values.

**Check for errors:**
```scheme
(db-query-fn (lambda (result error)
  (if error
      ;; Handle error
      (state-set! error-msg error)
      ;; Handle success
      (state-set! data result))))
```

**Common error cases:**
- SQL syntax errors
- Constraint violations (UNIQUE, NOT NULL, FOREIGN KEY)
- Type mismatches
- Missing required parameters
- Database locked (concurrent access)

**Error messages** are strings describing the failure.

---

## Complete API Index

### Table Definition
- `(db-table name (col-def) ...)`

### Query Definition
- `(db-query name #:from tbl ...)`
- `(db-query-single name #:from tbl ...)`

### Query Execution
- `(query-fn #:param val ... callback)`
- `(db-count tbl #:where cond callback)`

### Data Manipulation
- `(db-insert tbl #:values p-map callback)`
- `(db-update tbl #:set p-map #:where cond callback)`
- `(db-delete tbl #:where cond callback)`

### Transactions
- `(db-transaction fn callback)`
- `(tx-insert tx tbl #:values p-map)`
- `(tx-update tx tbl #:set p-map #:where cond)`
- `(tx-delete tx tbl #:where cond)`
- `(tx-query tx tbl ...)`
- `(tx-query-single tx tbl ...)`

### Raw SQL
- `(db-execute sql #:params list callback)`
- `(db-execute-update sql #:params list callback)`

### Migrations
- `(db-migrate version sql ... callback)`

### Utilities
- `(db-null? value)` - Check if value is SQL NULL
- `(p-map #:key1 val1 #:key2 val2 ...)` - Create property map
- `(p-map-get p-map #:key [default])` - Get value from p-map

---

## Architecture

```
Scheme Script (app.scm)
        │
        ▼
DatabaseExtensions (Kotlin)
        │
        ▼
AppDatabaseHelper (SQLiteOpenHelper)
        │
        ▼
app.db (SQLite file in app folder)
```

**Database Location:** Each app has its own `app.db` stored in the app folder alongside `app.scm`

**Naming Convention:** Scheme hyphens (`user-id`) ↔ SQL underscores (`user_id`)

# KleinLisp ORM Reference

A lightweight, idiomatic SQLite ORM for Moonstone Android applications.

## Table of Contents

1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Table Definitions](#table-definitions)
4. [Query Functions](#query-functions)
5. [Data Manipulation](#data-manipulation)
6. [Transactions](#transactions)
7. [Raw SQL](#raw-sql)
8. [Migrations](#migrations)
9. [Working with Results](#working-with-results)
10. [Error Handling](#error-handling)
11. [Best Practices](#best-practices)
12. [API Reference](#api-reference)

---

## Overview

The KleinLisp ORM provides a Scheme-idiomatic way to interact with SQLite databases in Moonstone Android applications. Each app has its own database (`app.db`) stored in the app folder alongside `app.scm`.

### Key Features

- **Declarative schema definitions** using `db-table`
- **Reusable query functions** with `db-query` and `db-query-single`
- **Type-safe operations** with automatic type mapping
- **Async execution** with callback-based API
- **JOIN support** for complex queries
- **Transactions** for atomic operations
- **Automatic migrations** for schema changes

### Architecture

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
app.db (SQLite)
```

---

## Quick Start

### 1. Define Your Schema

```scheme
(db-table users
  (id #:serial)
  (name #:string #:not-null)
  (email #:string #:unique)
  (age #:int)
  (active #:boolean #:default #t)
  (created-at #:timestamp #:default 'now))
```

### 2. Define Queries

```scheme
(db-query all-users
  #:from users
  #:order-by (name #:asc))

(db-query-single find-user-by-id
  #:from users
  #:where (= id ?user-id)
  #:params (user-id))
```

### 3. Use in Your App

```scheme
;; Insert a user
(db-insert users
  #:values (p-map #:name "Alice" #:email "alice@example.com" #:age 30)
  (lambda (id error)
    (if error
        (println error)
        (println (string-append "Created user: " (number->string id))))))

;; Query users
(all-users (lambda (rows error)
  (if error
      (println error)
      (for-each (lambda (row)
        (println (p-map-get row #:name)))
        rows))))

;; Find single user
(find-user-by-id #:user-id 1 (lambda (user error)
  (if error
      (println error)
      (if user
          (println (p-map-get user #:name))
          (println "Not found")))))
```

---

## Table Definitions

### Syntax

```scheme
(db-table table-name
  (column-name #:type constraint1 constraint2 ...)
  ...)
```

### Column Types

| Type | SQLite Type | Scheme Type | Description |
|------|-------------|-------------|-------------|
| `#:serial` | INTEGER PRIMARY KEY AUTOINCREMENT | int | Auto-incrementing ID |
| `#:int` | INTEGER | int | 32-bit integer |
| `#:long` | INTEGER | int | 64-bit integer |
| `#:string` | TEXT | string | Text string |
| `#:text` | TEXT | string | Alias for string |
| `#:real` | REAL | number | Floating point |
| `#:boolean` | INTEGER | boolean | Stored as 0/1, returned as #f/#t |
| `#:timestamp` | INTEGER | int | Unix timestamp (milliseconds) |
| `#:blob` | BLOB | string | Base64 encoded binary |

### Constraints

| Constraint | Description | Example |
|------------|-------------|---------|
| `#:pk` | Primary key | `(id #:int #:pk)` |
| `#:not-null` | Cannot be NULL | `(name #:string #:not-null)` |
| `#:unique` | Unique values | `(email #:string #:unique)` |
| `#:default value` | Default value | `(active #:boolean #:default #t)` |
| `#:default 'now` | Current timestamp | `(created-at #:timestamp #:default 'now)` |
| `#:size n` | Max length hint | `(title #:string #:size 200)` |
| `#:references table` | Foreign key | `(user-id #:long #:references users)` |

### Examples

```scheme
;; Basic table
(db-table products
  (id #:serial)
  (name #:string #:not-null)
  (price #:real #:not-null)
  (in-stock #:boolean #:default #t))

;; Table with foreign key
(db-table orders
  (id #:serial)
  (user-id #:long #:references users #:not-null)
  (product-id #:long #:references products #:not-null)
  (quantity #:int #:not-null #:default 1)
  (ordered-at #:timestamp #:default 'now))

;; Table with composite constraints
(db-table user-roles
  (id #:serial)
  (user-id #:long #:references users #:not-null)
  (role #:string #:not-null)
  (granted-at #:timestamp #:default 'now))
```

### Naming Convention

Scheme names use hyphens (e.g., `user-id`), which are automatically converted to underscores for SQL (e.g., `user_id`). Results are returned with hyphenated names.

---

## Query Functions

### db-query

Defines a reusable query function that returns multiple rows.

```scheme
(db-query function-name
  #:from table-name
  #:columns (col1 col2 ...)     ; Optional, defaults to all
  #:join (table #:on condition) ; Optional INNER JOIN
  #:left-join (table #:on cond) ; Optional LEFT JOIN
  #:where condition             ; Optional filter
  #:order-by (col #:asc ...)    ; Optional ordering
  #:limit n                     ; Optional limit
  #:params (param1 param2 ...)) ; Required parameters
```

### db-query-single

Same as `db-query` but returns a single row (p-map) or `#f` if not found.

```scheme
(db-query-single function-name
  #:from table-name
  #:where condition
  #:params (param-names ...))
```

### Condition Syntax

Conditions use prefix notation:

#### Comparison Operators

```scheme
(= column value)        ; Equal
(<> column value)       ; Not equal
(!= column value)       ; Not equal (alias)
(< column value)        ; Less than
(> column value)        ; Greater than
(<= column value)       ; Less than or equal
(>= column value)       ; Greater than or equal
```

#### Logical Operators

```scheme
(and cond1 cond2 ...)   ; All conditions must be true
(or cond1 cond2 ...)    ; Any condition must be true
(not condition)         ; Negate condition
```

#### Special Operators

```scheme
(like column "%pattern%")       ; Pattern matching
(in column (val1 val2 val3))    ; Value in list
(in column ?param)              ; Value in parameter list
(is-null column)                ; Column is NULL
(is-not-null column)            ; Column is not NULL
(between column low high)       ; Value in range
```

#### Parameter References

Use `?param-name` to reference parameters:

```scheme
(db-query find-by-email
  #:from users
  #:where (= email ?email)
  #:params (email))

;; Usage:
(find-by-email #:email "alice@example.com" callback)
```

### JOIN Queries

```scheme
;; INNER JOIN
(db-query user-orders
  #:from orders
  #:columns (orders.id users.name products.name orders.quantity)
  #:join (users #:on (= orders.user-id users.id))
  #:join (products #:on (= orders.product-id products.id))
  #:where (= orders.user-id ?uid)
  #:params (uid))

;; LEFT JOIN
(db-query users-with-orders
  #:from users
  #:columns (users.name orders.id)
  #:left-join (orders #:on (= users.id orders.user-id)))
```

### db-count

Count rows matching a condition.

```scheme
;; Count all
(db-count users callback)

;; Count with condition
(db-count users
  #:where (= active #t)
  callback)

;; Count with parameters
(db-count orders
  #:where (= user-id ?uid)
  #:uid 1
  callback)
```

---

## Data Manipulation

### db-insert

Insert one or more rows.

#### Single Row Insert

```scheme
(db-insert table-name
  #:values (p-map #:col1 val1 #:col2 val2 ...)
  callback)

;; Example
(db-insert users
  #:values (p-map #:name "Alice" #:email "alice@example.com" #:age 30)
  (lambda (id error)
    (if error
        (println (string-append "Error: " error))
        (println (string-append "Inserted with ID: " (number->string id))))))
```

#### Batch Insert

```scheme
(db-insert table-name
  #:values (list
    (p-map #:col1 val1 ...)
    (p-map #:col1 val2 ...))
  callback)

;; Example
(db-insert users
  #:values (list
    (p-map #:name "Alice" #:email "alice@example.com")
    (p-map #:name "Bob" #:email "bob@example.com")
    (p-map #:name "Charlie" #:email "charlie@example.com"))
  (lambda (ids error)
    (if error
        (println error)
        (println (string-append "Inserted " (number->string (length ids)) " rows")))))
```

### db-update

Update rows matching a condition.

```scheme
(db-update table-name
  #:set (p-map #:col1 new-val1 #:col2 new-val2 ...)
  #:where condition
  callback)

;; Example
(db-update users
  #:set (p-map #:active #f #:updated-at (current-timestamp))
  #:where (= id 1)
  (lambda (count error)
    (if error
        (println error)
        (println (string-append "Updated " (number->string count) " rows")))))
```

### db-delete

Delete rows matching a condition.

```scheme
;; Delete with condition
(db-delete table-name
  #:where condition
  callback)

;; Delete all (requires explicit flag)
(db-delete table-name
  #:all #t
  callback)

;; Example
(db-delete users
  #:where (and (= active #f) (< last-login ?cutoff))
  #:cutoff 1609459200000
  (lambda (count error)
    (if error
        (println error)
        (println (string-append "Deleted " (number->string count) " inactive users")))))
```

---

## Transactions

Execute multiple operations atomically.

### db-transaction

```scheme
(db-transaction
  (lambda (tx)
    ;; Use tx-* functions here
    ;; Return #t to commit, #f to rollback
    #t)
  callback)
```

### Transaction Functions

Inside the transaction lambda, use synchronous `tx-*` functions:

| Function | Description | Returns |
|----------|-------------|---------|
| `(tx-insert tx table #:values p-map)` | Insert row | Inserted ID |
| `(tx-update tx table #:set p-map #:where cond)` | Update rows | Affected count |
| `(tx-delete tx table #:where cond)` | Delete rows | Deleted count |
| `(tx-query tx table #:where cond ...)` | Query rows | List of p-maps |
| `(tx-query-single tx table #:where cond)` | Query single | p-map or #f |

### Example

```scheme
(db-transaction
  (lambda (tx)
    ;; Create user
    (define user-id (tx-insert tx users
      #:values (p-map #:name "Alice" #:email "alice@example.com")))

    ;; Create initial post
    (tx-insert tx posts
      #:values (p-map #:user-id user-id #:title "Hello World"))

    ;; Create settings
    (tx-insert tx user-settings
      #:values (p-map #:user-id user-id #:theme "dark"))

    ;; Return #t to commit
    #t)
  (lambda (success error)
    (if error
        (begin
          (println "Transaction failed:")
          (println error))
        (println "User created with all related data"))))
```

### Rollback

The transaction is rolled back if:
- The lambda returns `#f`
- An exception is thrown
- Any `tx-*` operation fails

---

## Raw SQL

For complex queries not covered by the ORM.

### db-execute

Execute a SELECT query.

```scheme
(db-execute "SQL query string"
  #:params (val1 val2 ...)  ; Optional
  callback)

;; Example: Complex aggregation
(db-execute
  "SELECT u.name, COUNT(p.id) as post_count, MAX(p.created_at) as last_post
   FROM users u
   LEFT JOIN posts p ON u.id = p.user_id
   GROUP BY u.id
   HAVING COUNT(p.id) > ?
   ORDER BY post_count DESC"
  #:params (5)
  (lambda (rows error)
    (if error
        (println error)
        (for-each (lambda (row)
          (println (string-append
            (p-map-get row #:name) ": "
            (number->string (p-map-get row #:post-count)) " posts")))
          rows))))
```

### db-execute-update

Execute INSERT, UPDATE, or DELETE.

```scheme
(db-execute-update "SQL statement"
  #:params (val1 val2 ...)  ; Optional
  callback)

;; Example: Bulk update
(db-execute-update
  "UPDATE users SET last_seen = ? WHERE active = 1"
  #:params ((current-timestamp))
  (lambda (count error)
    (if error
        (println error)
        (println (string-append "Updated " (number->string count) " users")))))
```

---

## Migrations

### Automatic Migrations

The ORM automatically handles simple schema changes:
- **New tables** are created automatically
- **New columns** are added with NULL default

### Manual Migrations

For complex changes (renaming, type changes, data migration):

```scheme
(db-migrate version
  "SQL statement 1"
  "SQL statement 2"
  ...
  callback)

;; Example: Rename column
(db-migrate 2
  "ALTER TABLE users RENAME TO users_old"
  "CREATE TABLE users (id INTEGER PRIMARY KEY, full_name TEXT, email TEXT)"
  "INSERT INTO users (id, full_name, email) SELECT id, name, email FROM users_old"
  "DROP TABLE users_old"
  (lambda (success error)
    (if error
        (println (string-append "Migration failed: " error))
        (println "Migration completed"))))
```

---

## Working with Results

### Result Types

| Operation | Success Result | Error Result |
|-----------|---------------|--------------|
| `db-query` | List of p-maps | Error string |
| `db-query-single` | p-map or #f | Error string |
| `db-count` | Integer | Error string |
| `db-insert` (single) | Inserted ID | Error string |
| `db-insert` (batch) | List of IDs | Error string |
| `db-update` | Affected count | Error string |
| `db-delete` | Deleted count | Error string |
| `db-transaction` | #t | Error string |
| `db-execute` | List of p-maps | Error string |
| `db-execute-update` | Affected count | Error string |

### Working with p-map Results

```scheme
;; Access values
(p-map-get row #:name)           ; Get value
(p-map-get row #:bio "No bio")   ; Get with default
(row #:name)                      ; Callable syntax
(row #:missing "default")         ; With default

;; Check for keys
(p-map-contains? row #:email)    ; Returns #t/#f

;; NULL handling
(define bio (p-map-get row #:bio))
(if (db-null? bio)
    (println "No bio set")
    (println bio))

;; Iterate entries
(for-each (lambda (entry)
  (define key (car entry))
  (define value (cadr entry))
  (println (string-append (symbol->string key) ": " (->string value))))
  (p-map-entries row))
```

### Working with Lists

```scheme
(all-users (lambda (users error)
  (if (not error)
      (begin
        ;; Length
        (define count (length users))

        ;; First/rest
        (define first-user (car users))
        (define other-users (cdr users))

        ;; Map
        (define names (map (lambda (u) (p-map-get u #:name)) users))

        ;; Filter
        (define active (filter (lambda (u) (p-map-get u #:active)) users))

        ;; Find
        (define admin (find (lambda (u)
          (string=? (p-map-get u #:role) "admin")) users))))))
```

---

## Error Handling

### Callback Pattern

All async operations use two-argument callbacks: `(result error)`

```scheme
(db-insert users #:values data
  (lambda (id error)
    (if error
        (handle-error error)
        (handle-success id))))
```

### Error Messages

| Error | Cause |
|-------|-------|
| `"Database not available"` | Database couldn't be opened |
| `"Table not found: X"` | Referenced table doesn't exist |
| `"Column not found: X"` | Referenced column doesn't exist |
| `"Constraint violation: UNIQUE"` | UNIQUE constraint failed |
| `"Constraint violation: NOT NULL"` | NOT NULL constraint failed |
| `"Constraint violation: FOREIGN KEY"` | Foreign key constraint failed |
| `"SQL error: X"` | Raw SQL syntax error |
| `"Parameter missing: X"` | Required parameter not provided |
| `"Migration failed: X"` | Migration SQL failed |

### Error Handling Example

```scheme
(define (safe-insert-user name email callback)
  (db-insert users
    #:values (p-map #:name name #:email email)
    (lambda (id error)
      (cond
        ((not error)
         (callback id #f))
        ((string-contains? error "UNIQUE")
         (callback #f "Email already exists"))
        ((string-contains? error "NOT NULL")
         (callback #f "Name is required"))
        (else
         (callback #f (string-append "Database error: " error)))))))
```

---

## Best Practices

### 1. Define Schema First

Always define all tables at the top of your script before any queries.

```scheme
;; Good: Schema at top
(db-table users ...)
(db-table posts ...)
(db-table comments ...)

(db-query all-users ...)
(db-query user-posts ...)
```

### 2. Use Parameterized Queries

Always use parameters for user input to prevent SQL injection.

```scheme
;; Good: Parameterized
(db-query search-users
  #:from users
  #:where (like name ?pattern)
  #:params (pattern))

;; Bad: String concatenation (DON'T DO THIS)
;; This would be vulnerable to SQL injection
```

### 3. Handle Errors

Always check for errors in callbacks.

```scheme
;; Good: Error handling
(all-users (lambda (rows error)
  (if error
      (state-set! error-state error)
      (state-set! users-state rows))))

;; Bad: Ignoring errors
(all-users (lambda (rows error)
  (state-set! users-state rows)))  ; Might set to NIL on error
```

### 4. Use Transactions for Related Operations

```scheme
;; Good: Atomic operation
(db-transaction
  (lambda (tx)
    (tx-delete tx cart-items #:where (= cart-id ?cid))
    (tx-insert tx orders #:values order-data)
    (tx-update tx inventory #:set (p-map #:quantity new-qty) #:where ...)
    #t)
  callback)

;; Bad: Separate operations (could leave inconsistent state)
(db-delete cart-items ...)
(db-insert orders ...)
(db-update inventory ...)
```

### 5. Use Appropriate Query Types

```scheme
;; Use db-query-single for ID lookups
(db-query-single find-user-by-id
  #:from users
  #:where (= id ?id)
  #:params (id))

;; Use db-query for lists
(db-query recent-posts
  #:from posts
  #:order-by (created-at #:desc)
  #:limit 10)
```

### 6. Index Considerations

While SQLite creates indexes for PRIMARY KEY and UNIQUE constraints automatically, consider your query patterns:

```scheme
;; If you frequently query by email, make it UNIQUE
(db-table users
  (id #:serial)
  (email #:string #:unique))  ; Indexed automatically
```

---

## API Reference

### Table Definition

| Function | Description |
|----------|-------------|
| `(db-table name (col-def) ...)` | Define a table schema |

### Query Definition

| Function | Description |
|----------|-------------|
| `(db-query name #:from tbl ...)` | Define multi-row query |
| `(db-query-single name #:from tbl ...)` | Define single-row query |

### Query Execution

| Function | Description |
|----------|-------------|
| `(query-fn #:param val ... callback)` | Execute defined query |
| `(db-count tbl #:where cond callback)` | Count rows |

### Data Manipulation

| Function | Description |
|----------|-------------|
| `(db-insert tbl #:values p-map callback)` | Insert row(s) |
| `(db-update tbl #:set p-map #:where cond callback)` | Update rows |
| `(db-delete tbl #:where cond callback)` | Delete rows |

### Transactions

| Function | Description |
|----------|-------------|
| `(db-transaction fn callback)` | Execute atomic operations |
| `(tx-insert tx tbl #:values p-map)` | Insert in transaction |
| `(tx-update tx tbl #:set p-map #:where cond)` | Update in transaction |
| `(tx-delete tx tbl #:where cond)` | Delete in transaction |
| `(tx-query tx tbl ...)` | Query in transaction |
| `(tx-query-single tx tbl ...)` | Single query in transaction |

### Raw SQL

| Function | Description |
|----------|-------------|
| `(db-execute sql #:params list callback)` | Execute SELECT |
| `(db-execute-update sql #:params list callback)` | Execute INSERT/UPDATE/DELETE |

### Migrations

| Function | Description |
|----------|-------------|
| `(db-migrate version sql ... callback)` | Run migration |

### Utilities

| Function | Description |
|----------|-------------|
| `(db-null? value)` | Check if value is SQL NULL |

---

## Version History

- **1.0** - Initial release with full ORM functionality
  - Table definitions with all column types
  - Query functions with JOIN support
  - Data manipulation (insert, update, delete)
  - Transactions
  - Raw SQL execution
  - Manual migrations

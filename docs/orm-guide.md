# Moonstone ORM Guide

A comprehensive guide to using the Object-Relational Mapping (ORM) system in Moonstone for Android.

## Table of Contents

1. [Overview](#overview)
2. [Schema Definitions](#schema-definitions)
3. [Query Definitions](#query-definitions)
4. [Dynamic API](#dynamic-api)
5. [CRUD Operations](#crud-operations)
6. [Transactions](#transactions)
7. [Raw SQL](#raw-sql)
8. [Error Handling](#error-handling)
9. [Best Practices](#best-practices)
10. [Complete Example](#complete-example)

## Overview

The Moonstone ORM provides a declarative way to interact with SQLite databases on Android. It features:

- **Declarative schema definitions** using `db-table` (macro) or `define-table` (function)
- **Type-safe queries** using `db-query` (macro) or `query-table` (function)
- **Async operations** with callback-based API
- **Transaction support** with automatic rollback
- **Raw SQL** for complex queries

All database operations are asynchronous and use callbacks to return results.

### Macro API vs. Dynamic API

The ORM offers two styles:

| Style | When to use |
|-------|-------------|
| **Macro** (`db-table`, `db-query`) | Static schemas and named reusable queries known at compile time |
| **Dynamic** (`define-table`, `query-table`) | Schemas or queries built at runtime from data — e.g., generated from user input, config files, or loaded from a list |

Both styles use exactly the same underlying engine. You can mix them freely.

## Schema Definitions

### Basic Table Definition

Use `db-table` to define your database tables:

```scheme
(db-table users
  (id #:serial)
  (username #:string #:not-null #:unique)
  (email #:string #:unique)
  (age #:int)
  (balance #:real #:default 0.0)
  (is-active #:boolean #:default #t)
  (bio #:text)
  (created-at #:timestamp #:default 'now)
  (updated-at #:timestamp))
```

### Column Types

| Type | Description | SQLite Type |
|------|-------------|-------------|
| `#:serial` | Auto-incrementing primary key | INTEGER PRIMARY KEY AUTOINCREMENT |
| `#:int` | Integer values | INTEGER |
| `#:real` | Floating-point numbers | REAL |
| `#:string` | Short text (up to 255 chars) | TEXT |
| `#:text` | Long text | TEXT |
| `#:boolean` | True/false values | INTEGER (0/1) |
| `#:timestamp` | Date/time values | TEXT (ISO format) |

### Column Constraints

| Constraint | Description |
|------------|-------------|
| `#:not-null` | Column cannot be NULL |
| `#:unique` | Column must have unique values |
| `#:default <value>` | Default value if not provided |

Special default values:
- `'now` - Current timestamp for `#:timestamp` columns

### Foreign Keys

Define relationships between tables:

```scheme
(db-table posts
  (id #:serial)
  (user-id #:int #:not-null #:references users)
  (title #:string #:not-null)
  (content #:text)
  (is-published #:boolean #:default #f))

(db-table comments
  (id #:serial)
  (post-id #:int #:not-null #:references posts)
  (user-id #:int #:not-null #:references users)
  (content #:text #:not-null))
```

## Query Definitions

### Basic Query

Define reusable queries with `db-query`:

```scheme
(db-query find-all-users
  #:from users)
```

Use the query:
```scheme
(find-all-users
  (lambda (results error)
    (if error
        (display error)
        (for-each display-user results))))
```

### Query with Conditions

```scheme
(db-query find-active-users
  #:from users
  #:where (= is-active #t))

(db-query find-user-by-id
  #:from users
  #:where (= id ?user-id)
  #:limit 1
  #:params (user-id))
```

### Parameterized Queries

Use `?param-name` for parameters and declare them with `#:params`:

```scheme
(db-query find-user-by-username
  #:from users
  #:where (= username ?username)
  #:limit 1
  #:params (username))

;; Usage
(find-user-by-username #:username "john"
  (lambda (user error)
    (if error
        (handle-error error)
        (display-user user))))
```

### Query Operators

#### Comparison Operators
```scheme
(= column value)      ;; Equal
(<> column value)     ;; Not equal
(!= column value)     ;; Not equal (alias)
(< column value)      ;; Less than
(> column value)      ;; Greater than
(<= column value)     ;; Less than or equal
(>= column value)     ;; Greater than or equal
```

#### Logical Operators
```scheme
(and condition1 condition2 ...)   ;; All conditions must be true
(or condition1 condition2 ...)    ;; At least one condition must be true
(not condition)                    ;; Negate condition
```

#### Special Operators
```scheme
(like column pattern)              ;; SQL LIKE pattern matching
(in column (list val1 val2 ...))   ;; Value in list
(is-null column)                   ;; Column is NULL
(is-not-null column)               ;; Column is not NULL
(between column min max)           ;; Value between min and max
```

### Complex Query Examples

```scheme
;; LIKE pattern matching
(db-query search-users
  #:from users
  #:where (like username ?pattern)
  #:params (pattern))

;; Usage: (search-users #:pattern "john%" callback)

;; Multiple conditions
(db-query find-premium-users
  #:from users
  #:where (and
            (= is-active #t)
            (or (> age 25) (>= balance 100.0))
            (is-not-null email))
  #:order-by (balance #:desc)
  #:limit 10)

;; BETWEEN query
(db-query find-users-by-balance
  #:from users
  #:where (between balance ?min ?max)
  #:params (min max))

;; IS NULL query
(db-query find-users-without-bio
  #:from users
  #:where (is-null bio))
```

### Ordering and Limiting

```scheme
(db-query recent-posts
  #:from posts
  #:where (= is-published #t)
  #:order-by (created-at #:desc)
  #:limit 20)

;; Multiple order columns (flat list format)
(db-query sorted-users
  #:from users
  #:order-by (is-active #:desc username #:asc))
```

### Single Result Queries

Use `db-query-single` when you expect exactly one result:

```scheme
;; Returns a single object (or #f if not found), not a list
(db-query-single find-user-by-id
  #:from users
  #:where (= id ?user-id)
  #:params (user-id))

;; Usage - result is the user object directly
(find-user-by-id #:user-id 123
  (lambda (user error)
    (if error
        (handle-error error)
        (if user
            (display (p-map-get user #:username))
            (display "User not found")))))
```

Note: Regular `db-query` with `#:limit 1` still returns a list. Use `db-query-single` for cleaner single-result access.

### JOIN Queries

```scheme
(db-query posts-with-authors
  #:from posts
  #:join (users #:on (= posts.user-id users.id))
  #:columns (posts.id posts.title users.username)
  #:where (= posts.is-published #t))
```

## Dynamic API

The dynamic API uses plain functions instead of macros. Because arguments are ordinary Scheme values (quoted lists), tables and queries can be constructed at runtime.

### define-table

```scheme
(define-table '(users
  (id #:serial)
  (username #:string #:not-null #:unique)
  (email #:string #:unique)
  (age #:int)
  (balance #:real #:default 0.0)
  (is-active #:boolean #:default #t)
  (bio #:text)
  (created-at #:timestamp #:default 'now)
  (updated-at #:timestamp)))
```

The argument is a quoted list with the same structure as the `db-table` macro body. All column types and constraints work identically.

**Dynamic table creation example:**

```scheme
;; Build a schema from a config or data structure
(define tables
  (list
    '(events (id #:serial) (name #:string #:not-null) (at #:timestamp #:default 'now))
    '(tags   (id #:serial) (label #:string #:unique))))

(for-each define-table tables)
```

### query-table

Execute a query immediately without defining a named function:

```scheme
;; Fetch all rows
(query-table '(#:from users)
  (lambda (results error)
    (if error
        (display error)
        (for-each display-user results))))

;; With filter and ordering
(query-table
  '(#:from users
    #:where (= is-active #t)
    #:order-by (username #:asc)
    #:limit 20)
  callback)
```

### query-table with parameters

Declare parameters inside the options list with `#:params`, then supply values as keyword arguments:

```scheme
(query-table
  '(#:from users
    #:where (= username ?username)
    #:params (username))
  #:username "alice"
  (lambda (results error)
    (if error
        (display error)
        (for-each display-user results))))
```

Multiple parameters work the same way:

```scheme
(query-table
  '(#:from users
    #:where (and (= is-active #t) (between balance ?min ?max))
    #:order-by (balance #:desc)
    #:params (min max))
  #:min 100.0
  #:max 500.0
  callback)
```

### query-table-single

Returns one row (a p-map) or `#f` if nothing is found:

```scheme
(query-table-single
  '(#:from users #:where (= id ?id) #:params (id))
  #:id 42
  (lambda (user error)
    (if error
        (handle-error error)
        (if user
            (display (p-map-get user #:username))
            (display "Not found")))))
```

### Building queries from data

Because the options list is just a Scheme list, you can construct it programmatically:

```scheme
(define (find-users-where condition params-spec param-values callback)
  (apply query-table
    (cons (append '(#:from users #:where) (list condition)
                  '(#:params) (list params-spec))
          (append param-values (list callback)))))
```

## CRUD Operations

### Insert (Create)

```scheme
;; Single insert
(db-insert users
  #:values (p-map #:username "john" #:email "john@example.com" #:age 25)
  (lambda (inserted-id error)
    (if error
        (display error)
        (display (string-append "Created user with ID: " (number->string inserted-id))))))

;; Batch insert
(db-insert users
  #:values (list
    (p-map #:username "user1" #:email "user1@example.com")
    (p-map #:username "user2" #:email "user2@example.com")
    (p-map #:username "user3" #:email "user3@example.com"))
  (lambda (count error)
    (if error
        (display error)
        (display (string-append "Inserted " (number->string count) " rows")))))
```

### Update

```scheme
(db-update users
  #:set (p-map #:age 26 #:balance 100.50)
  #:where (= username "john")
  (lambda (affected-count error)
    (if error
        (display error)
        (display (string-append "Updated " (number->string affected-count) " rows")))))
```

### Delete

```scheme
;; Delete with condition
(db-delete users
  #:where (= username "john")
  (lambda (deleted-count error)
    (if error
        (display error)
        (display (string-append "Deleted " (number->string deleted-count) " rows")))))

;; Delete all (requires explicit #:all flag for safety)
(db-delete users
  #:all #t
  (lambda (deleted-count error)
    (display (string-append "Deleted all " (number->string deleted-count) " rows"))))
```

### Count

```scheme
;; Count all
(db-count users
  (lambda (count error)
    (display (string-append "Total users: " (number->string count)))))

;; Count with condition
(db-count users
  #:where (= is-active #t)
  (lambda (count error)
    (display (string-append "Active users: " (number->string count)))))
```

## Transactions

Wrap multiple operations in a transaction for atomicity:

```scheme
(db-transaction
  (lambda (tx)
    ;; Insert within transaction
    (tx-insert tx users
      #:values (p-map #:username "new-user" #:email "new@example.com"))

    ;; Return #t to commit, #f to rollback
    #t)
  (lambda (success error)
    (if success
        (display "Transaction committed")
        (display (string-append "Transaction failed: " error)))))
```

### Transaction with Rollback

```scheme
(db-transaction
  (lambda (tx)
    (tx-insert tx users
      #:values (p-map #:username "temp-user" #:email "temp@example.com"))

    ;; Some condition check
    (if (some-validation-fails)
        #f   ;; Rollback - return false
        #t)) ;; Commit - return true
  (lambda (success error)
    (if success
        (display "Committed")
        (display "Rolled back"))))
```

### Transaction Functions

| Function | Description |
|----------|-------------|
| `tx-insert` | Insert within transaction |
| `tx-update` | Update within transaction |
| `tx-delete` | Delete within transaction |
| `tx-query` | Query within transaction (returns list) |
| `tx-query-single` | Query single result within transaction |

## Schema Migration

Tables are automatically created when the app starts. To run migrations explicitly:

```scheme
(db-migrate
  (lambda (success error)
    (if success
        (display "Migration complete")
        (display (string-append "Migration failed: " error)))))
```

The migration system:
- Creates tables that don't exist
- Does NOT modify existing tables (add columns, change types, etc.)
- Is safe to call multiple times

For schema changes to existing tables, you'll need to use raw SQL or implement custom migration logic.

## Raw SQL

For complex queries not supported by the ORM:

### Raw Query (SELECT)

```scheme
(db-execute "SELECT username, email FROM users WHERE age > 25 ORDER BY username"
  (lambda (rows error)
    (if error
        (display error)
        (for-each
          (lambda (row)
            (display (p-map-get row #:username)))
          rows))))
```

### Raw Update (INSERT/UPDATE/DELETE)

```scheme
(db-execute-update "UPDATE users SET balance = balance + 10 WHERE username LIKE 'premium%'"
  (lambda (affected-count error)
    (if error
        (display error)
        (display (string-append "Updated " (number->string affected-count) " rows")))))
```

## Error Handling

All callbacks receive an `error` parameter:

```scheme
(find-user-by-id #:user-id 123
  (lambda (user error)
    (cond
      (error
        (cond
          ((string-contains? error "UNIQUE constraint")
           (display "Duplicate entry"))
          ((string-contains? error "FOREIGN KEY constraint")
           (display "Referenced record not found"))
          ((string-contains? error "NOT NULL constraint")
           (display "Required field missing"))
          (else
           (display (string-append "Database error: " error)))))
      ((not user)
        (display "User not found"))
      (else
        (display-user user)))))
```

### Common Errors

| Error | Cause |
|-------|-------|
| `UNIQUE constraint failed` | Duplicate value in unique column |
| `FOREIGN KEY constraint failed` | Referenced record doesn't exist |
| `NOT NULL constraint failed` | Required field not provided |
| `no such table` | Table doesn't exist |

## Best Practices

### 1. Always Handle Errors

```scheme
;; Good
(find-user-by-id #:user-id 123
  (lambda (user error)
    (if error
        (handle-error error)
        (process-user user))))

;; Bad - ignores errors
(find-user-by-id #:user-id 123
  (lambda (user error)
    (process-user user)))
```

### 2. Use Parameterized Queries

```scheme
;; Good - prevents SQL injection
(db-query find-user
  #:from users
  #:where (= username ?username)
  #:params (username))

;; Bad - vulnerable to SQL injection
(db-execute (string-append "SELECT * FROM users WHERE username = '" username "'")
  callback)
```

### 3. Use Transactions for Related Operations

```scheme
;; Good - atomic operation
(db-transaction
  (lambda (tx)
    (tx-insert tx orders #:values order-data)
    (tx-update tx inventory #:set new-quantity #:where (= id item-id))
    #t)
  callback)

;; Bad - can leave data inconsistent
(db-insert orders #:values order-data
  (lambda (id error)
    (db-update inventory #:set new-quantity #:where (= id item-id)
      callback)))
```

### 4. Clean Up Test Data

```scheme
(define (cleanup-all-data)
  (db-delete comments #:all #t
    (lambda (c1 e1)
      (db-delete posts #:all #t
        (lambda (c2 e2)
          (db-delete users #:all #t
            (lambda (c3 e3)
              (display "All data cleaned up"))))))))
```

### 5. Use Meaningful Query Names

```scheme
;; Good
(db-query find-active-premium-users ...)
(db-query search-posts-by-title ...)

;; Bad
(db-query q1 ...)
(db-query get-data ...)
```

## Complete Example

Here's a complete example showing a simple blog system:

```scheme
;; Schema
(db-table users
  (id #:serial)
  (username #:string #:not-null #:unique)
  (email #:string #:unique))

(db-table posts
  (id #:serial)
  (user-id #:int #:not-null #:references users)
  (title #:string #:not-null)
  (content #:text)
  (is-published #:boolean #:default #f)
  (created-at #:timestamp #:default 'now))

;; Queries
(db-query find-user-by-username
  #:from users
  #:where (= username ?username)
  #:limit 1
  #:params (username))

(db-query find-published-posts
  #:from posts
  #:where (= is-published #t)
  #:order-by (created-at #:desc))

(db-query find-posts-by-author
  #:from posts
  #:where (= user-id ?user-id)
  #:order-by (created-at #:desc)
  #:params (user-id))

;; Create a new post
(define (create-post username title content callback)
  (find-user-by-username #:username username
    (lambda (user error)
      (if error
          (callback #f error)
          (if (not user)
              (callback #f "User not found")
              (db-insert posts
                #:values (p-map
                  #:user-id (p-map-get user #:id)
                  #:title title
                  #:content content
                  #:is-published #t)
                (lambda (post-id error2)
                  (if error2
                      (callback #f error2)
                      (callback post-id #f)))))))))

;; Usage
(create-post "john" "My First Post" "Hello, World!"
  (lambda (post-id error)
    (if error
        (toast (string-append "Error: " error))
        (toast (string-append "Created post #" (number->string post-id))))))
```

## API Reference

### Macros

| Macro | Description |
|-------|-------------|
| `db-table` | Define a database table |
| `db-query` | Define a reusable query (returns list) |
| `db-query-single` | Define a query returning single result |
| `db-insert` | Insert one or more records |
| `db-update` | Update records |
| `db-delete` | Delete records |
| `db-count` | Count records |

### Dynamic API Functions

| Function | Description |
|----------|-------------|
| `define-table` | Define a table schema from a quoted list |
| `query-table` | Execute a query from a quoted options list (returns list) |
| `query-table-single` | Execute a query returning single result or `#f` |

### Functions

| Function | Description |
|----------|-------------|
| `db-transaction` | Execute operations in a transaction |
| `db-execute` | Execute raw SELECT query |
| `db-execute-update` | Execute raw INSERT/UPDATE/DELETE |
| `db-migrate` | Run schema migrations |
| `db-null?` | Check if a value is database NULL |

### Transaction Functions

| Function | Description |
|----------|-------------|
| `tx-insert` | Insert within transaction |
| `tx-update` | Update within transaction |
| `tx-delete` | Delete within transaction |
| `tx-query` | Query within transaction (returns list) |
| `tx-query-single` | Query single result within transaction |

### Data Access

Query results are returned as persistent maps (`p-map`). Use these functions to access data:

| Function | Description |
|----------|-------------|
| `(p-map-get result #:column)` | Get column value |
| `(p-map-contains? result #:column)` | Check if column exists |
| `(p-map-keys result)` | Get all column names |
| `(db-null? value)` | Check if value is database NULL |

Example of NULL handling:
```scheme
(find-user-by-id #:user-id 123
  (lambda (user error)
    (if (not error)
        (let ((bio (p-map-get user #:bio)))
          (if (db-null? bio)
              (display "No bio set")
              (display bio))))))
```

## Platform Support

The ORM is currently available only on **Android**. Desktop support is planned for a future release.

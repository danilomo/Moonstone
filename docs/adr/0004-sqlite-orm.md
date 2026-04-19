# ADR-0004: SQLite ORM and Database Abstraction

## Status

Accepted

## Context

Many applications need persistent data storage. Moonstone needed a database solution that:
- Works on both Desktop and Android platforms
- Provides a simple API accessible from Scheme
- Supports common CRUD operations
- Enables schema definition from Scheme scripts
- Handles async operations properly (especially on Android)
- Is lightweight and embeddable

Alternative options considered:
1. **No built-in database** - Let users handle persistence themselves
2. **File-based JSON/serialization** - Simple key-value storage
3. **Room (Android) + Exposed (Desktop)** - Platform-specific ORMs
4. **Custom SQLite wrapper** - Direct SQL with helper functions
5. **Declarative ORM in Scheme** - Define tables and queries in Scheme

## Decision

We implemented a **custom declarative ORM** that wraps SQLite and exposes a Scheme-friendly API for defining schemas and performing queries.

### Architecture

```
Scheme Code → Database Extensions → SchemaRegistry → ConnectionPool → SQLite
                                  → QueryBuilder
                                  → TransactionContext
```

**Core Components:**
- `DatabaseHandler` - Manages connection, schema, and operations
- `SchemaRegistry` - Stores table and column definitions
- `TableDefinition` - Represents a database table
- `ColumnDefinition` - Represents a column with type and constraints
- `QueryBuilder` - Constructs SQL with parameter binding
- `ConnectionPool` - Manages database connections
- `TransactionContext` - Handles atomic operations

### Key Reasons

**1. Declarative Schema Definition**
Tables are defined in Scheme with clear, readable syntax:
```scheme
(db-table users
  (id #:serial)
  (name #:string #:not-null)
  (email #:string #:unique)
  (age #:int)
  (created-at #:timestamp #:default (current-timestamp)))
```

**2. Type-Safe Column Types**
Explicit type markers prevent confusion:
- `#:serial` - Auto-incrementing integer (primary key)
- `#:int`, `#:long`, `#:real` - Numeric types
- `#:string`, `#:text` - Text types
- `#:boolean` - Boolean type
- `#:timestamp` - DateTime type
- `#:blob` - Binary data

**3. Query Functions**
Queries are defined as Scheme functions that accept parameters:
```scheme
(db-query users-by-age
  #:from users
  #:where (>= age ?min-age)
  #:params (min-age))

; Call it:
(users-by-age #:min-age 18 callback)
```

**4. Async-First API**
All database operations use callbacks to work on Android's main thread:
```scheme
(db-insert users
  #:values (p-map #:name "Alice" #:age 25)
  (lambda (result error)
    (if error
        (show-toast error)
        (show-toast (string-append "ID: " (number->string result))))))
```

**5. Transaction Support**
Atomic operations via transaction lambda:
```scheme
(db-transaction
  (lambda (tx)
    (tx-insert tx users #:values (p-map #:name "Alice"))
    (tx-insert tx posts #:values (p-map #:title "First Post"))
    #t)  ; Return #t to commit, #f to rollback
  callback)
```

**6. NULL Handling**
Use the `'null` symbol for SQL NULL:
```scheme
(db-insert users
  #:values (p-map #:name "Bob" #:email 'null)  ; email is NULL
  callback)
```

**7. Migration Support**
Version-controlled schema changes:
```scheme
(db-migrate 1 "ALTER TABLE users ADD COLUMN phone TEXT" callback)
```

## Consequences

### What Becomes Easier

**✓ No Boilerplate**
Define a table and start using it immediately. No DAO classes, no entity annotations, no code generation.

**✓ Schema as Code**
Database schema lives in the Scheme script alongside UI code:
```scheme
; Schema
(db-table todos
  (id #:serial)
  (text #:string #:not-null)
  (done #:boolean #:default #f))

; Usage in UI
(db-query all-todos #:from todos)
```

**✓ Parameterized Queries**
SQL injection protection built-in:
```scheme
(db-query user-by-email
  #:from users
  #:where (= email ?email)
  #:params (email))

(user-by-email #:email "alice@example.com" callback)  ; Safe
```

**✓ Reusable Queries**
Queries are named functions that can be called anywhere:
```scheme
(db-query active-users #:from users #:where (= active #t))

; Later:
(active-users handle-users)
(active-users export-users)
```

**✓ Cross-Platform**
Same API works on Desktop (JDBC) and Android (AndroidX SQLite). No platform-specific code.

**✓ REPL-Friendly**
Define tables and run queries interactively:
```scheme
> (db-table test (id #:serial) (data #:string))
> (db-insert test #:values (p-map #:data "Hello") show-result)
```

### What Becomes Harder

**✗ No Compile-Time Validation**
Schema errors are caught at runtime, not compile-time:
```scheme
(db-table users (id #:unknown-type))  ; Runtime error
```

**✗ No Type Inference**
Return types are dynamic. All results are `LispObject`:
```scheme
(all-users (lambda (users error)
  (for-each (lambda (user)
              (define name (pmap-ref user #:name))  ; Must know structure
              ...)
            users)))
```

**✗ Callback Nesting**
Multiple async operations lead to callback pyramids:
```scheme
(db-insert users #:values user-data
  (lambda (user-id error)
    (db-insert posts #:values post-data
      (lambda (post-id error)
        (db-insert tags #:values tag-data
          (lambda (tag-id error)
            ...))))))
```

**✗ Limited Query Optimization**
The query builder is simple. Complex queries (joins, subqueries, CTEs) require raw SQL:
```scheme
(db-execute "SELECT u.name, COUNT(p.id) FROM users u LEFT JOIN posts p ON u.id = p.user_id GROUP BY u.id"
  callback)
```

**✗ No ORM Relationships**
No automatic foreign key handling or relationship loading. Must query related tables manually.

### Mitigations

**Compile-Time Validation:** Comprehensive error messages with hints:
```
ColumnTypeException: Unknown column type: :unknown-type
Hint: Use one of: #:serial, #:int, #:long, #:string, #:text, #:real, #:boolean, #:timestamp, #:blob
```

**Type Inference:** Document return structure for each query in comments:
```scheme
; Returns: list of p-maps with #:id, #:name, #:email
(db-query all-users #:from users)
```

**Callback Nesting:** Use helper functions to flatten:
```scheme
(define (create-user-with-post user-data post-data callback)
  (db-insert users #:values user-data
    (lambda (user-id error)
      (if error
          (callback #f error)
          (db-insert posts #:values (pmap-set post-data #:user-id user-id)
            callback)))))
```

**Query Optimization:** Provide `db-execute` for raw SQL when needed. Most apps don't need complex queries.

**Relationships:** Provide patterns in documentation:
```scheme
; Manual join
(db-query posts-with-users
  #:from "posts p INNER JOIN users u ON p.user_id = u.id"
  #:select "p.*, u.name as user_name")
```

## Alternatives Considered

### No Built-in Database
**Rejected** because:
- Most apps need persistence
- Users would reinvent SQL wrappers
- Inconsistent APIs across apps
- Missed opportunity to showcase Scheme's expressiveness

### File-Based JSON/Serialization
**Rejected** because:
- No querying capability (must load all data)
- No concurrency control
- No ACID guarantees
- Doesn't scale beyond trivial apps

### Platform-Specific ORMs (Room + Exposed)
**Rejected** because:
- Requires Kotlin code for each table
- Can't be defined dynamically from Scheme
- Different APIs on each platform
- Doesn't fit Moonstone's "pure Scheme" vision

### Direct SQL Wrapper
```scheme
(db-execute "INSERT INTO users (name, age) VALUES (?, ?)" (list "Alice" 25) callback)
```
**Rejected** because:
- Error-prone SQL strings
- No schema validation
- No type safety
- Verbose

## Real-World Usage

Complete CRUD app from `samples/database-crud/app.scm`:

```scheme
; Schema
(db-table items
  (id #:serial)
  (name #:string #:not-null)
  (description #:text)
  (price #:real)
  (in-stock #:boolean #:default #t)
  (created-at #:timestamp #:default (current-timestamp)))

; Queries
(db-query all-items #:from items #:order-by (created-at #:desc))
(db-query-single item-by-id #:from items #:where (= id ?id) #:params (id))

; UI with insert
(define (add-item name price)
  (db-insert items
    #:values (p-map #:name name #:price price)
    (lambda (id error)
      (if error
          (show-error error)
          (refresh-items)))))

; UI with update
(define (toggle-stock item-id)
  (db-update items
    #:set (p-map #:in-stock (not (pmap-ref current-item #:in-stock)))
    #:where (= id ?id)
    #:params (id item-id)
    refresh-items))

; UI with delete
(define (delete-item item-id)
  (db-delete items
    #:where (= id ?id)
    #:params (id item-id)
    refresh-items))
```

This 40-line script demonstrates a complete database-backed app with no Kotlin code.

## Performance Characteristics

**Table Definition:**
- O(1) - Registers in SchemaRegistry
- Lazy CREATE TABLE (on first use)

**Insert:**
- O(1) - Single row insert
- O(n) - Batch insert with n rows

**Query:**
- O(r) - Where r is result row count
- Plus SQLite query cost (indexed: O(log n), full scan: O(n))

**Update/Delete:**
- O(m) - Where m is matched row count
- Plus SQLite WHERE clause evaluation

**Transaction:**
- O(operations) - Same as individual ops
- Plus BEGIN/COMMIT overhead (minimal)

## Schema Registry Design

The `SchemaRegistry` stores table definitions in memory:

```kotlin
class SchemaRegistry {
    private val tables = mutableMapOf<String, TableDefinition>()

    fun registerTable(table: TableDefinition) {
        tables[table.name] = table
    }

    fun getTable(name: String): TableDefinition? = tables[name]

    fun hasTable(name: String): Boolean = tables.containsKey(name)

    fun createTablesSQL(): List<String> =
        tables.values.map { it.toCreateTableSQL() }
}
```

This enables:
- Schema introspection from Scheme
- Validation before SQL execution
- Separation of schema and storage

## Connection Management

Desktop and Android use different connection strategies:

**Desktop (JDBC):**
```kotlin
class JDBCConnectionPool(dbPath: File) {
    private val url = "jdbc:sqlite:${dbPath.absolutePath}"
    private val connections = mutableListOf<Connection>()

    fun getConnection(): Connection =
        connections.firstOrNull() ?: DriverManager.getConnection(url)
}
```

**Android (AndroidX SQLite):**
```kotlin
class AndroidConnectionPool(context: Context, dbName: String) {
    private val helper = SQLiteOpenHelper(context, dbName, null, VERSION)

    fun getConnection(): SQLiteDatabase = helper.writableDatabase
}
```

Both implement the same `ConnectionPool` interface, ensuring cross-platform compatibility.

## Testing

ORM tests cover all operations:
- `OrmIntegrationTest.kt` - 30+ tests
- Table definition and constraints
- CRUD operations
- Transactions
- NULL handling
- Migrations
- Error cases

All tests use in-memory databases for fast execution.

## Related Decisions

- ADR-0002: Scheme as DSL (enables declarative schema definition)
- ADR-0003: Reactive State (database results can be stored in state cells)

## References

- [SQLite Documentation](https://www.sqlite.org/docs.html)
- [AndroidX SQLite](https://developer.android.com/reference/androidx/sqlite/db/package-summary)
- [JDBC API](https://docs.oracle.com/javase/8/docs/api/java/sql/package-summary.html)
- [Database Design Principles](https://www.postgresql.org/docs/current/ddl.html)

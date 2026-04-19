;; KleinLisp ORM Comprehensive Test Suite
;; Tests all ORM functionality including edge cases and error handling


;; ============================================================
;; SCHEMA DEFINITIONS
;; ============================================================

(db-table test-users
  (id #:serial)
  (username #:string #:not-null #:unique)
  (email #:string #:unique)
  (age #:int)
  (balance #:real #:default 0.0)
  (is-active #:boolean #:default #t)
  (bio #:text)
  (created-at #:timestamp #:default 'now)
  (updated-at #:timestamp))

(db-table test-posts
  (id #:serial)
  (user-id #:long #:references test-users #:not-null)
  (title #:string #:not-null #:size 200)
  (content #:text)
  (view-count #:int #:default 0)
  (is-published #:boolean #:default #f)
  (published-at #:timestamp)
  (created-at #:timestamp #:default 'now))

(db-table test-comments
  (id #:serial)
  (post-id #:long #:references test-posts #:not-null)
  (user-id #:long #:references test-users #:not-null)
  (content #:text #:not-null)
  (created-at #:timestamp #:default 'now))

(db-table test-tags
  (id #:serial)
  (name #:string #:not-null #:unique))

(db-table test-post-tags
  (id #:serial)
  (post-id #:long #:references test-posts #:not-null)
  (tag-id #:long #:references test-tags #:not-null))

;; ============================================================
;; QUERY DEFINITIONS
;; ============================================================

;; Basic queries
(db-query all-test-users
  #:from test-users
  #:order-by (username #:asc))

(db-query active-test-users
  #:from test-users
  #:columns (id username email)
  #:where (= is-active #t)
  #:order-by (created-at #:desc))

(db-query-single find-test-user-by-id
  #:from test-users
  #:where (= id ?user-id)
  #:params (user-id))

(db-query-single find-test-user-by-username
  #:from test-users
  #:where (= username ?username)
  #:params (username))


;; Query with LIKE
(db-query search-test-users
  #:from test-users
  #:where (like username ?pattern)
  #:params (pattern))

;; Query with IN
(db-query find-test-users-by-ids
  #:from test-users
  #:where (in id ?ids)
  #:params (ids))

;; Query with NULL check
(db-query users-without-bio
  #:from test-users
  #:where (is-null bio))

(db-query users-with-bio
  #:from test-users
  #:where (is-not-null bio))

;; Query with age range parameters
(db-query find-test-users-by-age-range
  #:from test-users
  #:where (and (>= age ?min-age) (<= age ?max-age))
  #:order-by (age #:asc)
  #:params (min-age max-age))

;; Query with BETWEEN
(db-query users-by-balance-range
  #:from test-users
  #:where (between balance ?min ?max)
  #:params (min max))

;; Query with complex conditions
(db-query complex-user-query
  #:from test-users
  #:where (and
            (= is-active #t)
            (or (> age 25) (>= balance 100.0))
            (is-not-null email))
  #:order-by (balance #:desc)
  #:limit 10)

;; Posts queries
(db-query all-test-posts
  #:from test-posts
  #:order-by (created-at #:desc))

(db-query published-test-posts
  #:from test-posts
  #:where (= is-published #t)
  #:order-by (published-at #:desc))

(db-query user-test-posts
  #:from test-posts
  #:where (= user-id ?uid)
  #:order-by (created-at #:desc)
  #:params (uid))

(db-query popular-test-posts
  #:from test-posts
  #:where (and (= is-published #t) (> view-count ?min-views))
  #:order-by (view-count #:desc)
  #:limit ?limit
  #:params (min-views limit))

;; JOIN queries
(db-query posts-with-authors
  #:from test-posts
  #:columns (test-posts.id test-posts.title test-users.username)
  #:join (test-users #:on (= test-posts.user-id test-users.id))
  #:order-by (test-posts.created-at #:desc))

(db-query posts-with-comment-count
  #:from test-posts
  #:columns (test-posts.id test-posts.title)
  #:left-join (test-comments #:on (= test-posts.id test-comments.post-id))
  #:where (= test-posts.user-id ?uid)
  #:params (uid))

(db-query full-post-details
  #:from test-posts
  #:columns (test-posts.id test-posts.title test-posts.content
             test-users.username test-users.email)
  #:join (test-users #:on (= test-posts.user-id test-users.id))
  #:where (= test-posts.id ?post-id)
  #:params (post-id))

;; ============================================================
;; TEST STATE
;; ============================================================

(define current-test (state ""))
(define test-result (state ""))
(define test-details (state ""))
(define tests-passed (state 0))
(define tests-failed (state 0))
(define test-log (state '()))

(define (log-test name result details)
  (state-update! test-log (lambda (log)
    (cons (list name result details) log))))

(define (run-test name test-fn)
  (state-set! current-test name)
  (state-set! test-result "Running...")
  (state-set! test-details "")
  (test-fn))

(define (test-pass details)
  (state-set! test-result "PASS")
  (state-set! test-details details)
  (state-update! tests-passed (lambda (n) (+ n 1)))
  (log-test (state-ref current-test) "PASS" details))

(define (test-fail details)
  (state-set! test-result "FAIL")
  (state-set! test-details details)
  (state-update! tests-failed (lambda (n) (+ n 1)))
  (log-test (state-ref current-test) "FAIL" details))

;; ============================================================
;; TEST FUNCTIONS
;; ============================================================

;; Test 1: Basic Insert
(define (test-basic-insert)
  (run-test "Basic Insert"
    (lambda ()
      (db-insert test-users
        #:values (p-map #:username "testuser1" #:email "test1@example.com" #:age 25)
        (lambda (id error)
          (if error
              (test-fail error)
              (if (> id 0)
                  (test-pass (string-append "Inserted user with ID: " (number->string id)))
                  (test-fail "Invalid ID returned"))))))))

;; Test 2: Insert with defaults
(define (test-insert-defaults)
  (run-test "Insert with Defaults"
    (lambda ()
      (db-insert test-users
        #:values (p-map #:username "testuser2" #:email "test2@example.com")
        (lambda (id error)
          (if error
              (test-fail error)
              (find-test-user-by-id #:user-id id
                (lambda (user error2)
                  (if error2
                      (test-fail error2)
                      (if (and user
                               (p-map-get user #:is-active)
                               (= (p-map-get user #:balance) 0.0))
                          (test-pass "Defaults applied correctly")
                          (test-fail "Defaults not applied")))))))))))

;; Test 3: Batch Insert
(define (test-batch-insert)
  (run-test "Batch Insert"
    (lambda ()
      (db-insert test-users
        #:values (list
          (p-map #:username "batch1" #:email "batch1@example.com" #:age 20)
          (p-map #:username "batch2" #:email "batch2@example.com" #:age 25)
          (p-map #:username "batch3" #:email "batch3@example.com" #:age 30))
        (lambda (ids error)
          (if error
              (test-fail error)
              (if (= (length ids) 3)
                  (test-pass (string-append "Inserted " (number->string (length ids)) " users"))
                  (test-fail (string-append "Expected 3, got " (number->string (length ids)))))))))))

;; Test 4: Query All
(define (test-query-all)
  (run-test "Query All Users"
    (lambda ()
      (all-test-users
        (lambda (rows error)
          (if error
              (test-fail error)
              (if (> (length rows) 0)
                  (test-pass (string-append "Found " (number->string (length rows)) " users"))
                  (test-fail "No users found"))))))))

;; Test 5: Query Single Found
(define (test-query-single-found)
  (run-test "Query Single (Found)"
    (lambda ()
      (find-test-user-by-username #:username "testuser1"
        (lambda (user error)
          (if error
              (test-fail error)
              (if user
                  (if (string=? (p-map-get user #:username) "testuser1")
                      (test-pass "Found correct user")
                      (test-fail "Wrong user returned"))
                  (test-fail "User not found"))))))))

;; Test 6: Query Single Not Found
(define (test-query-single-not-found)
  (run-test "Query Single (Not Found)"
    (lambda ()
      (find-test-user-by-username #:username "nonexistent"
        (lambda (user error)
          (if error
              (test-fail error)
              (if (not user)
                  (test-pass "Correctly returned #f for missing user")
                  (test-fail "Should have returned #f"))))))))

;; Test 7: Query with Parameters
(define (test-query-params)
  (run-test "Query with Parameters"
    (lambda ()
      (find-test-users-by-age-range #:min-age 20 #:max-age 30
        (lambda (users error)
          (if error
              (test-fail error)
              (test-pass (string-append "Found " (number->string (length users)) " users in age range"))))))))

;; Test 8: Query with LIKE
(define (test-query-like)
  (run-test "Query with LIKE"
    (lambda ()
      (search-test-users #:pattern "batch%"
        (lambda (users error)
          (if error
              (test-fail error)
              (if (= (length users) 3)
                  (test-pass "LIKE pattern matched correctly")
                  (test-fail (string-append "Expected 3, got " (number->string (length users)))))))))))

;; Test 9: db-count
(define (test-count)
  (run-test "Count Query"
    (lambda ()
      (db-count test-users
        (lambda (count error)
          (if error
              (test-fail error)
              (test-pass (string-append "Total users: " (number->string count)))))))))

;; Test 10: db-count with condition
(define (test-count-condition)
  (run-test "Count with Condition"
    (lambda ()
      (db-count test-users #:where (= is-active #t)
        (lambda (count error)
          (if error
              (test-fail error)
              (test-pass (string-append "Active users: " (number->string count)))))))))

;; Test 11: Update
(define (test-update)
  (run-test "Update"
    (lambda ()
      (db-update test-users
        #:set (p-map #:age 26 #:balance 100.50)
        #:where (= username "testuser1")
        (lambda (count error)
          (if error
              (test-fail error)
              (if (= count 1)
                  (find-test-user-by-username #:username "testuser1"
                    (lambda (user error2)
                      (if error2
                          (test-fail error2)
                          (if (and (= (p-map-get user #:age) 26)
                                   (= (p-map-get user #:balance) 100.5))
                              (test-pass "Update verified")
                              (test-fail "Update values incorrect")))))
                  (test-fail (string-append "Expected 1 row, updated " (number->string count))))))))))

;; Test 12: Delete
(define (test-delete)
  (run-test "Delete"
    (lambda ()
      (db-insert test-users
        #:values (p-map #:username "to-delete" #:email "delete@example.com")
        (lambda (id error)
          (if error
              (test-fail error)
              (db-delete test-users
                #:where (= username "to-delete")
                (lambda (count error2)
                  (if error2
                      (test-fail error2)
                      (if (= count 1)
                          (test-pass "Deleted 1 row")
                          (test-fail (string-append "Deleted " (number->string count) " rows"))))))))))))

;; Test 13: UNIQUE constraint violation
(define (test-unique-violation)
  (run-test "UNIQUE Constraint Violation"
    (lambda ()
      (db-insert test-users
        #:values (p-map #:username "testuser1" #:email "duplicate@example.com")
        (lambda (id error)
          (if error
              (if (string-contains? error "UNIQUE")
                  (test-pass "UNIQUE violation detected correctly")
                  (test-fail (string-append "Wrong error: " error)))
              (test-fail "Should have failed with UNIQUE violation")))))))

;; Test 14: NOT NULL constraint violation
(define (test-not-null-violation)
  (run-test "NOT NULL Constraint Violation"
    (lambda ()
      (db-insert test-posts
        #:values (p-map #:user-id 1)
        (lambda (id error)
          (if error
              (if (string-contains? error "NOT NULL")
                  (test-pass "NOT NULL violation detected correctly")
                  (test-fail (string-append "Wrong error: " error)))
              (test-fail "Should have failed with NOT NULL violation")))))))

;; Test 15: NULL value handling
(define (test-null-handling)
  (run-test "NULL Value Handling"
    (lambda ()
      (db-insert test-users
        #:values (p-map #:username "null-test" #:email "null@example.com" #:bio 'null)
        (lambda (id error)
          (if error
              (test-fail error)
              (find-test-user-by-id #:user-id id
                (lambda (user error2)
                  (if error2
                      (test-fail error2)
                      (if (db-null? (p-map-get user #:bio))
                          (test-pass "NULL value stored and retrieved correctly")
                          (test-fail "NULL value not handled correctly")))))))))))

;; Test 16: Transaction commit
;; Note: Using let instead of inner define because KleinLisp evaluates
;; inner define values at compile time, but tx-insert only exists at runtime
(define (test-transaction-commit)
  (run-test "Transaction Commit"
    (lambda ()
      (db-transaction
        (lambda (tx)
          (let ((user-id (tx-insert tx test-users
                           #:values (p-map #:username "tx-user" #:email "tx@example.com"))))
            (tx-insert tx test-posts
              #:values (p-map #:user-id user-id #:title "Transaction Post"))
            #t))
        (lambda (success error)
          (if error
              (test-fail error)
              (find-test-user-by-username #:username "tx-user"
                (lambda (user error2)
                  (if error2
                      (test-fail error2)
                      (if user
                          (test-pass "Transaction committed successfully")
                          (test-fail "User not found after commit")))))))))))

;; Test 17: Transaction rollback
(define (test-transaction-rollback)
  (run-test "Transaction Rollback"
    (lambda ()
      (db-transaction
        (lambda (tx)
          (tx-insert tx test-users
            #:values (p-map #:username "rollback-user" #:email "rollback@example.com"))
          #f)
        (lambda (success error)
          (find-test-user-by-username #:username "rollback-user"
            (lambda (user error2)
              (if user
                  (test-fail "User should not exist after rollback")
                  (test-pass "Transaction rolled back correctly")))))))))

;; Test 18: Raw SQL query
(define (test-raw-query)
  (run-test "Raw SQL Query"
    (lambda ()
      (db-execute "SELECT username, email FROM test_users WHERE username LIKE 'batch%' ORDER BY username"
        (lambda (rows error)
          (if error
              (test-fail error)
              (if (= (length rows) 3)
                  (test-pass "Raw SQL query returned correct results")
                  (test-fail (string-append "Expected 3 rows, got " (number->string (length rows)))))))))))

;; Test 19: Raw SQL update
(define (test-raw-update)
  (run-test "Raw SQL Update"
    (lambda ()
      (db-execute-update "UPDATE test_users SET balance = balance + 10 WHERE username LIKE 'batch%'"
        (lambda (count error)
          (if error
              (test-fail error)
              (test-pass (string-append "Raw update affected " (number->string count) " rows"))))))))

;; Test 20: JOIN query
(define (test-join-query)
  (run-test "JOIN Query"
    (lambda ()
      (find-test-user-by-username #:username "testuser1"
        (lambda (user err0)
          (if err0
              (test-fail err0)
              (if (not user)
                  (test-fail "No user found for JOIN test")
                  (let ((user-id (p-map-get user #:id)))
                    (db-insert test-posts
                      #:values (p-map #:user-id user-id #:title "Join Test Post" #:is-published #t)
                      (lambda (id error)
                        (if error
                            (test-fail error)
                            (posts-with-authors
                (lambda (posts error2)
                  (if error2
                      (test-fail error2)
                      (if (> (length posts) 0)
                          (let ((post (car posts)))
                            (if (and (p-map-contains? post #:title)
                                     (p-map-contains? post #:username))
                                (test-pass "JOIN query returned merged data")
                                (test-fail "JOIN data incomplete")))
                          (test-fail "No posts returned"))))))))))))))))

;; Test 21: Complex condition query
(define (test-complex-conditions)
  (run-test "Complex Conditions"
    (lambda ()
      (complex-user-query
        (lambda (users error)
          (if error
              (test-fail error)
              (test-pass (string-append "Complex query returned " (number->string (length users)) " users"))))))))

;; Test 22: Query with IS NULL
(define (test-is-null-query)
  (run-test "IS NULL Query"
    (lambda ()
      (users-without-bio
        (lambda (users error)
          (if error
              (test-fail error)
              (test-pass (string-append "Found " (number->string (length users)) " users without bio"))))))))

;; Test 23: Query with IS NOT NULL
(define (test-is-not-null-query)
  (run-test "IS NOT NULL Query"
    (lambda ()
      (db-insert test-users
        #:values (p-map #:username "has-bio" #:email "bio@example.com" #:bio "I have a bio!")
        (lambda (id error)
          (if error
              (test-fail error)
              (users-with-bio
                (lambda (users error2)
                  (if error2
                      (test-fail error2)
                      (if (> (length users) 0)
                          (test-pass (string-append "Found " (number->string (length users)) " users with bio"))
                          (test-fail "Should have found users with bio")))))))))))

;; Test 24: BETWEEN query
(define (test-between-query)
  (run-test "BETWEEN Query"
    (lambda ()
      (users-by-balance-range #:min 50.0 #:max 150.0
        (lambda (users error)
          (if error
              (test-fail error)
              (test-pass (string-append "Found " (number->string (length users)) " users in balance range"))))))))

;; Test 25: Delete All (safety check)
(define (test-delete-all-safety)
  (run-test "Delete All Safety"
    (lambda ()
      (db-insert test-tags
        #:values (p-map #:name "temp-tag")
        (lambda (id error)
          (if error
              (test-fail error)
              (db-delete test-tags
                #:all #t
                (lambda (count error2)
                  (if error2
                      (test-fail error2)
                      (db-count test-tags
                        (lambda (remaining error3)
                          (if error3
                              (test-fail error3)
                              (if (= remaining 0)
                                  (test-pass "Delete all worked with #:all flag")
                                  (test-fail "Delete all did not remove all rows"))))))))))))))

;; ============================================================
;; TEST RUNNER
;; ============================================================

(define all-tests (list
  test-basic-insert
  test-insert-defaults
  test-batch-insert
  test-query-all
  test-query-single-found
  test-query-single-not-found
  test-query-params
  test-query-like
  test-count
  test-count-condition
  test-update
  test-delete
  test-unique-violation
  test-not-null-violation
  test-null-handling
  test-transaction-commit
  test-transaction-rollback
  test-raw-query
  test-raw-update
  test-join-query
  test-complex-conditions
  test-is-null-query
  test-is-not-null-query
  test-between-query
  test-delete-all-safety))

(define current-test-index (state 0))
(define is-running (state #f))

(define num-tests 25)

(define (run-next-test)
  (let ((index (state-ref current-test-index)))
    (toast (string-append "run-next-test index=" (number->string index)))
    (if (< index num-tests)
        (begin
          (toast (string-append "Fetching test at " (number->string index)))
          (let ((test-fn (list-ref all-tests index)))
            (toast "list-ref done, updating index")
            (state-set! current-test-index (+ index 1))
            (toast "Calling test function now")
            (test-fn)))
        (begin
          (toast "All tests complete")
          (state-set! is-running #f)))))

(define (run-all-tests)
  (state-set! tests-passed 0)
  (state-set! tests-failed 0)
  (state-set! test-log '())
  (state-set! current-test-index 0)
  (state-set! is-running #t)
  (run-next-test))

(define (cleanup-test-data)
  (db-delete test-comments #:all #t (lambda (c1 e1)
    (db-delete test-post-tags #:all #t (lambda (c2 e2)
      (db-delete test-posts #:all #t (lambda (c3 e3)
        (db-delete test-tags #:all #t (lambda (c4 e4)
          (db-delete test-users #:all #t (lambda (c5 e5)
            (state-set! test-details "All test data cleaned up"))))))))))))

;; ============================================================
;; UI
;; ============================================================

(define (render-test-summary)
  (surface #:padding 16 #:shape 'rounded #:color "surfaceVariant"
    (column #:spacing 8
      (text #:value "Test Summary" #:style 'title-medium)
      (row #:spacing 16
        (text #:value tests-passed #:color "green")
        (text #:value tests-failed #:color "red"))
      (text #:value "Total: 25"))))

(define (render-current-test)
  (surface #:padding 16 #:shape 'rounded #:color "primaryContainer"
    (column #:spacing 8
      (text #:value "Current Test" #:style 'title-medium)
      (text #:value current-test #:style 'body-large)
      (text #:value test-result #:style 'headline-small)
      (text #:value test-details #:style 'body-small))))

(define (render-controls)
  (surface #:padding 16 #:shape 'rounded
    (column #:spacing 12
      (text #:value "Controls" #:style 'title-medium)
      (column #:spacing 8
        (row #:spacing 8
          (button #:on-click run-all-tests
            (text #:value "Run All"))
          (button #:on-click run-next-test
            (text #:value "Next")))
        (button #:on-click cleanup-test-data #:style 'outlined
          (text #:value "Cleanup Data"))))))

(define (render-test-log-item entry)
  (let ((name (car entry))
        (result (cadr entry))
        (details (caddr entry)))
    (surface #:padding 8 #:shape 'rounded
             #:color (if (string=? result "PASS") "tertiaryContainer" "errorContainer")
      (column #:spacing 4
        (row #:spacing 8
          (text #:value result #:style 'label-medium
                #:color (if (string=? result "PASS") "green" "red"))
          (text #:value name #:style 'body-medium))
        (text #:value details #:style 'body-small)))))

(define (app)
  (scaffold
    #:top-bar (top-app-bar #:title "ORM Test Suite")
    (column #:padding 16 #:spacing 12
      (render-test-summary)
      (render-current-test)
      (render-controls)
      (text #:value "Test Log" #:style 'title-medium)
      (dynamic-list
        #:items test-log
        #:render-item render-test-log-item
        #:spacing 8))))

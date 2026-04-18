(db-table users
  (id #:serial)
  (name #:string #:not-null))

(db-query all-users
  #:from users
  #:order-by (name #:asc))

(define test-result (state "Not started"))

(define (insert-user)
  (state-set! test-result "Inserting...")
  (db-insert users
    #:values (p-map #:name "Test User")
    (lambda (id error)
      (if error
          (state-set! test-result error)
          (state-set! test-result
            (string-append "Inserted user with ID: " (number->string id)))))))

(define (run-query)
  (state-set! test-result "Querying...")
  (all-users
    (lambda (rows error)
      (if error
          (state-set! test-result error)
          (state-set! test-result
            (string-append "Found " (number->string (length rows)) " users"))))))

(define (app)
  (column #:padding 16 #:spacing 16
    (text #:value "ORM Simple Test" #:style 'headline-medium)
    (text #:value test-result #:style 'body-large)
    (row #:spacing 8
      (button #:on-click insert-user
        (text #:value "Insert User"))
      (button #:on-click run-query
        (text #:value "Run Query")))))

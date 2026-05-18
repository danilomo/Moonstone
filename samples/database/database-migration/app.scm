(db-table users
  (id #:serial)
  (username #:string #:not-null))

(define message (state "Ready"))

(define (add-email-column)
  (db-migrate 2
    "ALTER TABLE users ADD COLUMN email TEXT"
    (lambda (success error)
      (if error
        (state-set! message (string-append "Migration failed: " error))
        (state-set! message "Email column added")))))

(define (insert-user)
  (db-execute-update "INSERT INTO users (username, email) VALUES (?, ?)"
    #:params (list "alice" "alice@example.com")
    (lambda (id error)
      (if error
        (state-set! message (string-append "Error: " error))
        (state-set! message (string-append "Inserted ID: " (number->string id)))))))

(define (query-users)
  (db-execute "SELECT * FROM users" #:params (list)
    (lambda (rows error)
      (if error
        (state-set! message (string-append "Error: " error))
        (state-set! message (string-append "Found " (number->string (length rows)) " users"))))))

(define (app)
  (column #:padding 16 #:spacing 8 #:fill-max-size #t
    (text #:value "Migration Test" #:style 'headline-medium)

    (row #:spacing 8
      (button #:on-click (lambda () (add-email-column))
        (text #:value "Migrate"))
      (button #:on-click (lambda () (insert-user))
        (text #:value "Insert"))
      (button #:on-click (lambda () (query-users))
        (text #:value "Query")))

    (surface #:padding 16 #:color 'surface-variant
      (text #:value message))))

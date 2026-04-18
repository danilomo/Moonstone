(db-table users
  (id #:serial)
  (name #:string #:not-null)
  (age #:int))

(define message (state "Click a button to test database"))

(define (app)
  (column #:padding 16 #:spacing 8
    (text #:value message #:style 'body-large)

    (button #:on-click (lambda ()
      (db-insert users #:values (p-map #:name "Alice" #:age 30)
        (lambda (id error)
          (if error
            (state-set! message error)
            (state-set! message (string-append "Inserted user with ID: " (number->string id)))))))
      (text #:value "Insert Alice"))

    (button #:on-click (lambda ()
      (db-insert users #:values (p-map #:name "Bob" #:age 25)
        (lambda (id error)
          (if error
            (state-set! message error)
            (state-set! message (string-append "Inserted user with ID: " (number->string id)))))))
      (text #:value "Insert Bob"))

    (button #:on-click (lambda ()
      (db-execute "SELECT * FROM users" #:params (list)
        (lambda (rows error)
          (if error
            (state-set! message error)
            (state-set! message (string-append "Found users: " (number->string (length rows))))))))
      (text #:value "Query Users"))

    (button #:on-click (lambda ()
      (db-count users
        (lambda (count error)
          (if error
            (state-set! message error)
            (state-set! message (string-append "Total users: " (number->string count)))))))
      (text #:value "Count Users"))))

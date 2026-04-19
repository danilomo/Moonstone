;; Minimal ORM test - step by step
;; Testing the table and query definitions

(db-table mini-users
  (id #:serial)
  (name #:string #:not-null))

(db-query all-mini-users
  #:from mini-users
  #:order-by (name #:asc))

(define result (state "Not started"))

(define (run-test)
  (state-set! result "Running...")
  (db-count mini-users
    (lambda (count error)
      (if error
          (state-set! result error)
          (state-set! result (string-append "Count: " (number->string count)))))))

(define (app)
  (column #:padding 16 #:spacing 16
    (text #:value "ORM Mini Test" #:style 'headline-medium)
    (text #:value result #:style 'body-large)
    (button #:on-click run-test
      (text #:value "Run Test"))))

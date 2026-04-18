(db-table accounts
  (id #:serial)
  (name #:string #:not-null)
  (balance #:int #:not-null #:default 0))

(define message (state "Ready"))

(define (setup-accounts)
  (db-insert accounts #:values (list
    (p-map #:name "Alice" #:balance 100)
    (p-map #:name "Bob" #:balance 50))
    (lambda (ids error)
      (if error
        (state-set! message (string-append "Error: " error))
        (state-set! message "Accounts created")))))

(define (add-account-tx)
  (db-transaction
    (lambda (tx)
      (tx-insert tx accounts #:values (p-map #:name "Charlie" #:balance 200))
      #t)
    (lambda (success error)
      (if error
        (state-set! message (string-append "Transaction failed: " error))
        (state-set! message "Account added in transaction")))))

(define (check-balances)
  (db-execute "SELECT name, balance FROM accounts ORDER BY name" #:params (list)
    (lambda (rows error)
      (if error
        (state-set! message (string-append "Error: " error))
        (state-set! message (string-append "Found " (number->string (length rows)) " accounts"))))))

(define (clear-accounts)
  (db-delete accounts #:all #t
    (lambda (count error)
      (if error
        (state-set! message (string-append "Error: " error))
        (state-set! message (string-append "Cleared " (number->string count) " accounts"))))))

(define (app)
  (column #:padding 16 #:spacing 8 #:fill-max-size #t
    (text #:value "Transaction Test" #:style 'headline-medium)

    (row #:spacing 8
      (button #:on-click (lambda () (setup-accounts))
        (text #:value "Setup"))
      (button #:on-click (lambda () (add-account-tx))
        (text #:value "Add (TX)"))
      (button #:on-click (lambda () (check-balances))
        (text #:value "Check"))
      (button #:on-click (lambda () (clear-accounts))
        (text #:value "Clear")))

    (surface #:padding 16 #:color 'surface-variant
      (text #:value message))))

(db-table items
  (id #:serial)
  (name #:string #:not-null)
  (value #:int))

(define message (state "Ready"))

(define (benchmark-insert-single)
  (db-insert items #:values (p-map #:name "Test Item" #:value 42)
    (lambda (id error)
      (if error
        (state-set! message (string-append "Error: " error))
        (state-set! message (string-append "Inserted ID: " (number->string id)))))))

(define (benchmark-insert-batch)
  (db-insert items #:values (list
    (p-map #:name "Item 1" #:value 1)
    (p-map #:name "Item 2" #:value 2)
    (p-map #:name "Item 3" #:value 3)
    (p-map #:name "Item 4" #:value 4)
    (p-map #:name "Item 5" #:value 5)
    (p-map #:name "Item 6" #:value 6)
    (p-map #:name "Item 7" #:value 7)
    (p-map #:name "Item 8" #:value 8)
    (p-map #:name "Item 9" #:value 9)
    (p-map #:name "Item 10" #:value 10))
    (lambda (ids error)
      (if error
        (state-set! message (string-append "Error: " error))
        (state-set! message (string-append "Inserted " (number->string (length ids)) " items"))))))

(define (benchmark-query)
  (db-execute "SELECT * FROM items" #:params (list)
    (lambda (rows error)
      (if error
        (state-set! message (string-append "Error: " error))
        (state-set! message (string-append "Queried " (number->string (length rows)) " rows"))))))

(define (benchmark-count)
  (db-count items
    (lambda (count error)
      (if error
        (state-set! message (string-append "Error: " error))
        (state-set! message (string-append "Total items: " (number->string count)))))))

(define (clear-items)
  (db-delete items #:all #t
    (lambda (count error)
      (if error
        (state-set! message (string-append "Error: " error))
        (state-set! message (string-append "Deleted " (number->string count) " items"))))))

(define (app)
  (column #:padding 16 #:spacing 8 #:fill-max-size #t
    (text #:value "Database Benchmark" #:style 'headline-medium)

    (row #:spacing 8
      (button #:on-click (lambda () (benchmark-insert-single))
        (text #:value "Insert 1"))
      (button #:on-click (lambda () (benchmark-insert-batch))
        (text #:value "Insert 10")))

    (row #:spacing 8
      (button #:on-click (lambda () (benchmark-query))
        (text #:value "Query All"))
      (button #:on-click (lambda () (benchmark-count))
        (text #:value "Count"))
      (button #:on-click (lambda () (clear-items))
        (text #:value "Clear")))

    (surface #:padding 16 #:color 'surface-variant
      (text #:value message))))

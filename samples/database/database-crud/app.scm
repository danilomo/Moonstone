(db-table products
  (id #:serial)
  (name #:string #:not-null)
  (price #:real #:not-null)
  (in-stock #:boolean #:default #t))

(define message (state "Ready"))

(define (insert-product)
  (db-insert products #:values (p-map #:name "Widget" #:price 19.99 #:in-stock #t)
    (lambda (id error)
      (if error
        (state-set! message (string-append "Error: " error))
        (state-set! message (string-append "Inserted ID: " (number->string id)))))))

(define (query-products)
  (db-execute "SELECT * FROM products" #:params (list)
    (lambda (rows error)
      (if error
        (state-set! message (string-append "Error: " error))
        (state-set! message (string-append "Found " (number->string (length rows)) " products"))))))

(define (update-product)
  (db-update products #:set (p-map #:price 24.99) #:where (= name "Widget")
    (lambda (count error)
      (if error
        (state-set! message (string-append "Error: " error))
        (state-set! message (string-append "Updated " (number->string count) " rows"))))))

(define (delete-product)
  (db-delete products #:where (= name "Widget")
    (lambda (count error)
      (if error
        (state-set! message (string-append "Error: " error))
        (state-set! message (string-append "Deleted " (number->string count) " rows"))))))

(define (app)
  (column #:padding 16 #:spacing 8 #:fill-max-size #t
    (text #:value "Database CRUD Test" #:style 'headline-medium)

    (row #:spacing 8
      (button #:on-click (lambda () (insert-product))
        (text #:value "Insert"))
      (button #:on-click (lambda () (query-products))
        (text #:value "Query"))
      (button #:on-click (lambda () (update-product))
        (text #:value "Update"))
      (button #:on-click (lambda () (delete-product))
        (text #:value "Delete")))

    (surface #:padding 16 #:color 'surface-variant
      (text #:value message))))

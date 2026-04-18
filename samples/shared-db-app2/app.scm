(db-table messages
  (id #:serial)
  (content #:string #:not-null)
  (created-at #:string))

(db-query all-messages #:from messages)

(define messages-list (state (list)))
(define status (state ""))

(define (load-messages)
  (state-set! status "Loading...")
  (all-messages
    (lambda (rows error)
      (if error
          (state-set! status (string-append "Error: " error))
          (begin
            (state-set! messages-list rows)
            (state-set! status (string-append "Loaded " (number->string (length rows)) " messages")))))))

(define (render-message msg)
  (let ((id (p-map-ref msg 'id))
        (content (p-map-ref msg 'content)))
    (surface #:padding 12 #:shape 'rounded #:color "#f0f0f0"
      (column #:spacing 4
        (text #:value (string-append "#" (number->string id)) #:style 'label-small #:color "gray")
        (text #:value content #:style 'body-medium)))))

(define (app)
  (column #:padding 24 #:spacing 16 #:fill-max-size #t
    (text #:value "Message Reader" #:style 'headline-medium)
    (text #:value "Reads from shared database" #:style 'body-medium #:color "gray")
    (spacer #:height 8)
    (row #:spacing 16
      (button #:on-click load-messages
        (text #:value "Refresh"))
      (text #:value status #:color "gray"))
    (dynamic-list #:items messages-list #:render-item render-message #:spacing 8)))

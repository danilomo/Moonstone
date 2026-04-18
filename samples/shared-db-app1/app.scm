(db-table messages
  (id #:serial)
  (content #:string #:not-null)
  (created-at #:string))

(define message-input (state ""))
(define status (state ""))

(define (save-message)
  (let ((content (state-ref message-input)))
    (if (string=? content "")
        (state-set! status "Please enter a message")
        (db-insert messages
          #:values (p-map #:content content #:created-at "now")
          (lambda (id error)
            (if error
                (state-set! status (string-append "Error: " error))
                (begin
                  (state-set! status (string-append "Saved message #" (number->string id)))
                  (state-set! message-input ""))))))))

(define (app)
  (column #:padding 24 #:spacing 16 #:fill-max-size #t
    (text #:value "Message Writer" #:style 'headline-medium)
    (text #:value "Writes to shared database" #:style 'body-medium #:color "gray")
    (spacer #:height 16)
    (outlined-text-field
      #:value message-input
      #:label "Enter a message"
      #:on-change (lambda (v) (state-set! message-input v))
      #:fill-max-width #t)
    (button #:on-click save-message
      (text #:value "Save Message"))
    (text #:value status #:color "green")))

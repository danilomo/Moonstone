(define clicks (state 0))

(define (on-click)
  (state-update! clicks (lambda (n) (+ n 1))))

(define (app)
  (column #:padding 16 #:spacing 16
    (text #:value "Button Click Test" #:style 'headline-medium)
    (text #:value clicks #:style 'body-large)
    (button #:on-click on-click
      (text #:value "Click Me"))))

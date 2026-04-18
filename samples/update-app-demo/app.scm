(define counter (state 0))

(define (app-v1)
  (column #:spacing 16 #:padding 32 #:fill-max-size #t
          #:horizontal-alignment 'center #:vertical-arrangement 'center
    (surface #:color "#E3F2FD" #:shape 'rounded #:padding 24
      (column #:spacing 16 #:horizontal-alignment 'center
        (text #:value "Version 1" #:style 'headline-medium #:color "#1565C0")
        (text #:value "Blue Theme" #:style 'body-large)))
    (spacer #:height 16)
    (text #:value (derived (lambda ()
      (string-append "Counter: " (number->string (state-ref counter))))))
    (row #:spacing 12
      (button #:on-click (lambda () (state-update! counter (lambda (n) (- n 1))))
        (text #:value "-"))
      (button #:on-click (lambda () (state-update! counter (lambda (n) (+ n 1))))
        (text #:value "+")))
    (spacer #:height 32)
    (button #:on-click (lambda () (update-app app-v2)) #:style 'tonal
      (text #:value "Switch to Version 2"))))

(define (app-v2)
  (column #:spacing 16 #:padding 32 #:fill-max-size #t
          #:horizontal-alignment 'center #:vertical-arrangement 'center
    (surface #:color "#F3E5F5" #:shape 'rounded #:padding 24
      (column #:spacing 16 #:horizontal-alignment 'center
        (text #:value "Version 2" #:style 'headline-medium #:color "#7B1FA2")
        (text #:value "Purple Theme" #:style 'body-large)))
    (spacer #:height 16)
    (text #:value (derived (lambda ()
      (string-append "Counter value is still: " (number->string (state-ref counter))))))
    (row #:spacing 12
      (button #:on-click (lambda () (state-update! counter (lambda (n) (* n 2)))) #:style 'outlined
        (text #:value "Double"))
      (button #:on-click (lambda () (state-set! counter 0)) #:style 'outlined
        (text #:value "Reset")))
    (spacer #:height 32)
    (button #:on-click (lambda () (update-app app-v1)) #:style 'tonal
      (text #:value "Switch to Version 1"))))

(define (app) (app-v1))

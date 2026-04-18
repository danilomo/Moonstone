(define count (state 0))

(define count-display (derived (lambda ()
  (string-append "Count: " (number->string (state-ref count))))))

(define doubled (derived (lambda ()
  (* 2 (state-ref count)))))

(define doubled-display (derived (lambda ()
  (string-append "Doubled: " (number->string (state-ref doubled))))))

(define status-text (derived (lambda ()
  (if (> (state-ref count) 5) "High" "Low"))))

(define (app)
  (box #:padding 32 #:fill-max-size #t #:content-alignment 'center
    (column #:spacing 16 #:horizontal-alignment 'center
      (text #:value "Derived State Demo" #:style 'headline-medium)

      (spacer #:height 16)

      (surface #:color 'gray #:shape 'rounded #:padding 24
        (column #:spacing 12 #:horizontal-alignment 'center
          (text #:value count-display #:style 'title-large)
          (text #:value doubled-display #:style 'body-large)
          (text #:value status-text #:style 'body-medium #:color 'blue)))

      (spacer #:height 16)

      (row #:spacing 16
        (button #:on-click (lambda () (state-update! count (lambda (x) (- x 1))))
          (text #:value "-"))
        (button #:on-click (lambda () (state-update! count (lambda (x) (+ x 1))))
          (text #:value "+"))))))

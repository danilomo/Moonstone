(define count (state 0))

(define count-display (derived (lambda ()
  (number->string (state-ref count)))))

(define doubled-display (derived (lambda ()
  (number->string (* 2 (state-ref count))))))

(define status-text (derived (lambda ()
  (if (> (state-ref count) 5) "High" "Low"))))

(define (app)
  (scaffold
    #:top-bar (top-app-bar #:title "Derived State" #:style 'center-aligned)
    (box #:fill-max-size 1 #:content-alignment 'center
      (column #:spacing 24 #:horizontal-alignment 'center #:padding 24

        (text #:value "Derived values recompute automatically when their source state changes."
              #:style 'body-medium
              #:color "gray"
              #:text-align "center")

        (card #:style 'outlined #:fill-max-width #t #:padding 24
          (column #:spacing 20 #:horizontal-alignment 'center

            (column #:spacing 4 #:horizontal-alignment 'center
              (text #:value "count" #:style 'label-medium #:color "gray")
              (text #:value count-display #:style 'display-small))

            (divider)

            (row #:spacing 48 #:horizontal-arrangement 'center
              (column #:horizontal-alignment 'center #:spacing 4
                (text #:value "doubled" #:style 'label-medium #:color "gray")
                (text #:value doubled-display #:style 'headline-large))
              (column #:horizontal-alignment 'center #:spacing 4
                (text #:value "status" #:style 'label-medium #:color "gray")
                (text #:value status-text #:style 'headline-large)))))

        (row #:spacing 16
          (button #:style 'outlined
                  #:on-click (lambda () (state-update! count (lambda (x) (- x 1))))
            (text #:value "-"))
          (button #:style 'filled
                  #:on-click (lambda () (state-update! count (lambda (x) (+ x 1))))
            (text #:value "+")))))))

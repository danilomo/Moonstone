(define count (state 0))

(define (increment)
  (state-update! count (lambda (x) (+ x 1))))

(define (decrement)
  (state-update! count (lambda (x) (- x 1))))

(define (reset)
  (state-set! count 0))

(define (app)
  (scaffold
   #:top-bar (top-app-bar #:title "Counter" #:style 'center-aligned)
   (box #:fill-max-size 1 #:content-alignment 'center
     (column
      #:spacing 32
      #:horizontal-alignment 'center

      (text #:value count
            #:style 'display-large)

      (row
       #:spacing 16

       (button
        #:style 'filled
        #:on-click decrement
        (text #:value "-"))

       (button
        #:style 'outlined
        #:on-click reset
        (text #:value "Reset"))

       (button
        #:style 'filled
        #:on-click increment
        (text #:value "+")))))))


(define count (state 0))

(define (increment)
  (state-update! count (lambda (x) (+ x 1))))

(define (decrement)
  (state-update! count (lambda (x) (- x 1))))

(define (reset)
  (state-set! count 0))

(define (app)
  (surface
   #:fill-max-size 1
   #:color 'white
   (column
    #:fill-max-size 1
    #:padding 32
    #:spacing 24
    #:vertical-arrangement 'center
    #:horizontal-alignment 'center

    (text #:value "Counter Demo"
          #:style 'headline-large
          #:color 'blue)

    (text #:value count
          #:style 'display-large)

    (row
     #:spacing 16
     #:horizontal-arrangement 'center

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
      (text #:value "+"))))))


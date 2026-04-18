(define greeting (state "Hello, KleinLisp GUI!"))

(define (app)
  (box
   #:fill-max-size 1
   #:content-alignment 'center
   #:padding 16
   (text #:value (state-ref greeting)
         #:style 'headline-medium
         #:color 'blue)))

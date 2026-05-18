(define name (state ""))
(define greeting (state ""))

(define (greet)
  (state-set! greeting
    (string-append "Hello, " (state-ref name) "!")))

(define (app)
  (scaffold
   (column
    #:padding 24
    #:spacing 16
    #:vertical-arrangement 'center
    #:horizontal-alignment 'center
    #:fill-max-size 1

    (text #:value "Welcome!"
          #:style 'headline-large)

    (spacer #:height 8)

    (outlined-text-field
     #:value name
     #:label "Enter your name"
     #:fill-max-width 1
     #:on-change (lambda (v) (state-set! name v)))

    (spacer #:height 8)

    (button
     #:on-click greet
     (text #:value "Greet Me"))

    (spacer #:height 16)

    (text #:value greeting
          #:style 'title-large
          #:color "green"))))

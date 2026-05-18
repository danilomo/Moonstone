(define name-input (state ""))
(define agree-terms (state #f))
(define notifications (state #t))
(define selected-option (state 1))
(define status (state "Ready"))

(define (update-status msg)
  (state-set! status msg))

(define (app)
  (scaffold
   (column
    #:fill-max-size 1
    #:padding 24
    #:spacing 16

    (text #:value "Interactive Components Demo"
          #:style 'headline-medium)

    (spacer #:height 8)

    (text #:value "Text Input" #:style 'title-medium)
    (outlined-text-field
     #:value name-input
     #:label "Your name"
     #:fill-max-width 1
     #:on-change (lambda (v) (state-set! name-input v)))

    (spacer #:height 8)

    (text #:value "Buttons" #:style 'title-medium)
    (row
     #:spacing 12
     (button
      #:style 'filled
      #:on-click (lambda () (update-status "Filled button clicked!"))
      (text #:value "Filled"))
     (button
      #:style 'outlined
      #:on-click (lambda () (update-status "Outlined button clicked!"))
      (text #:value "Outlined"))
     (button
      #:style 'text
      #:on-click (lambda () (update-status "Text button clicked!"))
      (text #:value "Text")))

    (spacer #:height 8)

    (text #:value "Toggle Controls" #:style 'title-medium)
    (checkbox
     #:checked agree-terms
     #:on-change (lambda (v) (state-set! agree-terms v))
     (text #:value "I agree to the terms"))

    (switch
     #:checked notifications
     #:on-change (lambda (v) (state-set! notifications v))
     (text #:value "Enable notifications"))

    (spacer #:height 8)

    (text #:value "Radio Buttons" #:style 'title-medium)
    (column
     #:spacing 4
     (radio-button
      #:selected selected-option
      #:value 1
      #:on-select (lambda () (state-set! selected-option 1) (update-status "Selected: Option A"))
      (text #:value "Option A"))
     (radio-button
      #:selected selected-option
      #:value 2
      #:on-select (lambda () (state-set! selected-option 2) (update-status "Selected: Option B"))
      (text #:value "Option B"))
     (radio-button
      #:selected selected-option
      #:value 3
      #:on-select (lambda () (state-set! selected-option 3) (update-status "Selected: Option C"))
      (text #:value "Option C")))

    (spacer #:height 16)

    (card
     #:style 'outlined
     #:shape 'rounded
     #:padding 12
     #:fill-max-width #t
     (text #:value status
           #:style 'body-large)))))

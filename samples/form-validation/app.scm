(define name (state ""))
(define email (state ""))
(define password (state ""))
(define agreed-to-terms (state #f))
(define submitted (state #f))

(define name-error (state ""))
(define email-error (state ""))
(define password-error (state ""))
(define terms-error (state ""))

(define (on-name-change v)
  (state-set! name v)
  (state-set! name-error ""))

(define (on-email-change v)
  (state-set! email v)
  (state-set! email-error ""))

(define (on-password-change v)
  (state-set! password v)
  (state-set! password-error ""))

(define (on-terms-change v)
  (state-set! agreed-to-terms v)
  (state-set! terms-error ""))

(define (validate-name)
  (if (string=? (state-ref name) "")
      (begin
        (state-set! name-error "Name is required")
        #f)
      #t))

(define (validate-email)
  (if (string=? (state-ref email) "")
      (begin
        (state-set! email-error "Email is required")
        #f)
      (if (not (string-contains? (state-ref email) "@"))
          (begin
            (state-set! email-error "Email must contain @")
            #f)
          #t)))

(define (validate-password)
  (if (string=? (state-ref password) "")
      (begin
        (state-set! password-error "Password is required")
        #f)
      (if (< (string-length (state-ref password)) 8)
          (begin
            (state-set! password-error "Password must be at least 8 characters")
            #f)
          #t)))

(define (validate-terms)
  (if (not (state-ref agreed-to-terms))
      (begin
        (state-set! terms-error "You must agree to the terms")
        #f)
      #t))

(define (validate-and-submit)
  (define name-valid (validate-name))
  (define email-valid (validate-email))
  (define password-valid (validate-password))
  (define terms-valid (validate-terms))
  (if (and (and (and name-valid email-valid) password-valid) terms-valid)
      (state-set! submitted #t)))

(define (reset-form)
  (state-set! name "")
  (state-set! email "")
  (state-set! password "")
  (state-set! agreed-to-terms #f)
  (state-set! submitted #f)
  (state-set! name-error "")
  (state-set! email-error "")
  (state-set! password-error "")
  (state-set! terms-error ""))

(define (error-message error-state)
  (if (not (string=? (state-ref error-state) ""))
      (text #:value error-state #:style 'body-small #:color 'red)
      (spacer #:height 4)))

(define (registration-form)
  (column
   #:fill-max-width 1
   #:spacing 16
   #:padding 24

   (text #:value "Create Account"
         #:style 'headline-large)

   (spacer #:height 8)

   (column
    #:fill-max-width 1
    #:spacing 4
    (outlined-text-field
     #:value name
     #:label "Name"
     #:placeholder "Enter your full name"
     #:fill-max-width 1
     #:on-change on-name-change)
    (error-message name-error))

   (column
    #:fill-max-width 1
    #:spacing 4
    (outlined-text-field
     #:value email
     #:label "Email"
     #:placeholder "you@example.com"
     #:fill-max-width 1
     #:keyboard-type 'email
     #:on-change on-email-change)
    (error-message email-error))

   (column
    #:fill-max-width 1
    #:spacing 4
    (outlined-text-field
     #:value password
     #:label "Password"
     #:placeholder "Enter a password (min 8 chars)"
     #:fill-max-width 1
     #:keyboard-type 'password
     #:on-change on-password-change)
    (error-message password-error))

   (column
    #:fill-max-width 1
    #:spacing 4
    (row
     #:spacing 8
     #:vertical-alignment 'center
     (checkbox
      #:checked agreed-to-terms
      #:on-change on-terms-change)
     (text #:value "I agree to the Terms of Service"))
    (error-message terms-error))

   (spacer #:height 16)

   (button
    #:on-click validate-and-submit
    (text #:value "Create Account"))))

(define (success-screen)
  (column
   #:fill-max-size 1
   #:padding 32
   #:vertical-arrangement 'center
   #:horizontal-alignment 'center
   #:spacing 24

   (icon #:name "check" #:size 64 #:tint 'green)

   (text #:value "Account Created!"
         #:style 'headline-large
         #:color 'green)

   (text #:value "Welcome to Moonstone"
         #:style 'body-large
         #:color 'gray)

   (column
    #:spacing 8
    #:horizontal-alignment 'center
    (text #:value name #:style 'title-medium)
    (text #:value email #:style 'body-medium #:color 'gray))

   (spacer #:height 16)

   (button
    #:style 'outlined
    #:on-click reset-form
    (text #:value "Start Over"))))

(define (app)
  (scaffold
   (box
    #:fill-max-size 1
    #:content-alignment 'center
    (card
     #:style 'elevated
     #:shape 'rounded-large
     (if (state-ref submitted)
         (success-screen)
         (registration-form))))))

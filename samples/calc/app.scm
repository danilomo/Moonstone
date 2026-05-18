(define display-value (state "0"))
(define current-op (state #f))
(define first-num (state 0))
(define clear-next (state #f))

(define (append-digit d)
  (if (state-ref clear-next)
      (begin
        (state-set! display-value (number->string d))
        (state-set! clear-next #f))
      (if (string=? (state-ref display-value) "0")
          (state-set! display-value (number->string d))
          (state-set! display-value
            (string-append (state-ref display-value) (number->string d))))))

(define (set-operation op)
  (state-set! first-num (string->number (state-ref display-value)))
  (state-set! current-op op)
  (state-set! clear-next #t))

(define (calculate)
  (let* ((second-num (string->number (state-ref display-value)))
         (op (state-ref current-op))
         (first (state-ref first-num))
         (result
           (cond
             ((eq? op 'add) (+ first second-num))
             ((eq? op 'sub) (- first second-num))
             ((eq? op 'mul) (* first second-num))
             ((eq? op 'div) (if (= second-num 0) 0 (/ first second-num)))
             (else second-num))))
    (state-set! display-value (number->string result))
    (state-set! current-op #f)
    (state-set! clear-next #t)))

(define (clear-all)
  (state-set! display-value "0")
  (state-set! current-op #f)
  (state-set! first-num 0)
  (state-set! clear-next #f))

(define (calc-button label action)
  (box #:modifier (list (list 'weight 1)) #:fill-max-width #t
    (button #:on-click action #:style 'tonal #:fill-max-width #t
      (text #:value label #:style 'title-medium))))

(define (op-button label op)
  (box #:modifier (list (list 'weight 1)) #:fill-max-width #t
    (button #:on-click (lambda () (set-operation op)) #:style 'filled #:fill-max-width #t
      (text #:value label #:style 'title-medium))))

(define (digit-button d)
  (calc-button (number->string d) (lambda () (append-digit d))))

(define (button-row . buttons)
  (row #:spacing 8 #:fill-max-width #t
    buttons))

(define (app)
  (column #:fill-max-size 1 #:padding 12 #:spacing 8
    (box #:modifier (list (list 'weight 1)) #:fill-max-width #t
      (surface #:shape 'rounded #:fill-max-size 1
        (box #:fill-max-size 1 #:content-alignment 'bottom-end #:padding 24
          (text #:value display-value #:style 'display-large #:text-align "end"))))
    (button-row
      (calc-button "C" clear-all)
      (calc-button "+/-" (lambda () #f))
      (calc-button "%" (lambda () #f))
      (op-button "/" 'div))
    (button-row
      (digit-button 7)
      (digit-button 8)
      (digit-button 9)
      (op-button "x" 'mul))
    (button-row
      (digit-button 4)
      (digit-button 5)
      (digit-button 6)
      (op-button "-" 'sub))
    (button-row
      (digit-button 1)
      (digit-button 2)
      (digit-button 3)
      (op-button "+" 'add))
    (button-row
      (digit-button 0)
      (calc-button "." (lambda () #f))
      (box #:modifier (list (list 'weight 1)) #:fill-max-width #t
        (button #:on-click calculate #:style 'filled #:fill-max-width #t
          (text #:value "=" #:style 'title-medium))))))

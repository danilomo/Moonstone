(define (make-state current stored op display reset?)
  (list current stored op display reset?))

(define (state-current s) (list-ref s 0))
(define (state-stored s)  (list-ref s 1))
(define (state-op s)      (list-ref s 2))
(define (state-display s) (list-ref s 3))
(define (state-reset? s)  (list-ref s 4))

(define initial-state
  (make-state "0" #f #f "0" #f))

(define (apply-op op a b)
  (cond ((char=? op #\+) (+ a b))
        ((char=? op #\-) (- a b))
        ((char=? op #\*) (* a b))
        ((char=? op #\/) (/ a b))
        (else b)))

(define (calc-input state input)
  (let* ((cur (state-current state))
         (stored (state-stored state))
         (op (state-op state))
         (reset? (state-reset? state)))

    (cond
      ((and (char? input) (char-numeric? input))
       (let ((new-cur (if reset?
                          (string input)
                          (if (string=? cur "0")
                              (string input)
                              (string-append cur (string input))))))
         (make-state new-cur stored op new-cur #f)))

      ((and (char? input)
            (memv input '(#\+ #\- #\* #\/)))
       (let ((num (string->number cur)))
         (if stored
             (let ((result (apply-op op stored num)))
               (make-state
                (number->string result)
                result
                input
                (number->string result)
                #t))
             (make-state cur num input cur #t))))

      ((and (char? input) (char=? input #\=))
       (if (and stored op)
           (let* ((num (string->number cur))
                  (result (apply-op op stored num)))
             (make-state
              (number->string result)
              #f
              #f
              (number->string result)
              #t))
           state))

      ((and (char? input) (char=? input #\C))
       initial-state)

      ((and (char? input) (char=? input #\.))
       (if (string-contains cur ".")
           state
           (let ((new (string-append cur ".")))
             (make-state new stored op new #f))))

      (else state))))

(define (display-string state)
  (state-display state))

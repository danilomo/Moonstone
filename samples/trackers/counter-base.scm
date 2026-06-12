; Generic counter/habit engine — no date, no history.
; Define the following variables before (load-relative "../counter-base.scm"):
;
;   Required:
;     *metrics*     — list of p-map field definitions (see below)
;     *app-title*   — string shown in the top bar
;
; Each entry in *metrics* is a p-map with these keys:
;
;   Required:
;     #:label      "Display Label"
;     #:col-name   #:some-col-name
;     #:type       #:real | #:int | #:string | #:text
;
;   Optional:
;     #:input-type 'number | 'spin | 'slider | 'toggle | 'rating | 'choice
;     #:step       2.5
;     #:min        0
;     #:max        100
;     #:options    '("A" "B" "C")
;
; Every widget interaction auto-saves immediately — no Save button, no history.
; One persistent record is kept; every save replaces it in-place.

(define (build-table-def)
  (append
    (list 'counters
          (list 'id '#:serial))
    (map (lambda (m)
           (list (string->symbol (keyword->string (p-map-get m #:col-name)))
                 (p-map-get m #:type)))
         *metrics*)))

(define-table (build-table-def))

; === State ===

(define save-in-progress (state #f))
(define save-dirty (state #f))

(define metric-input-states
  (map (lambda (m)
         (let ((it (p-map-get m #:input-type 'number)))
           (state (cond
             ((eq? it 'spin)   "0")
             ((eq? it 'slider) (number->string (p-map-get m #:min 0)))
             ((eq? it 'toggle) "0")
             (else "")))))
       *metrics*))

; === Helpers ===

(define (zip2 l1 l2)
  (if (or (null? l1) (null? l2))
      '()
      (cons (list (car l1) (car l2))
            (zip2 (cdr l1) (cdr l2)))))

(define (make-range n)
  (define (loop i acc)
    (if (< i 0) acc (loop (- i 1) (cons i acc))))
  (loop (- n 1) '()))

(define (metric-col-kw m) (p-map-get m #:col-name))
(define (metric-input-type m) (p-map-get m #:input-type 'number))

; === Value handling ===

(define (is-string-type? m)
  (let ((ts (keyword->string (p-map-get m #:type))))
    (or (string=? ts "string") (string=? ts "text"))))

(define (parse-input-value m str)
  (if (string=? str "")
      'null
      (if (is-string-type? m)
          str
          (string->number str))))

(define (format-number n decimals)
  (if (= decimals 0)
      (number->string (exact (round n)))
      (let ((factor (expt 10 decimals)))
        (number->string (/ (round (* n factor)) factor)))))

(define (decimals-for-step step)
  (cond ((>= step 1)    0)
        ((>= step 0.1)  1)
        ((>= step 0.01) 2)
        (else           3)))

(define (metric-decimals m)
  (if (string=? (keyword->string (p-map-get m #:type)) "int") 0 1))

(define (db-val->string val)
  (cond
    ((db-null? val) "")
    ((string? val) val)
    ((= val (floor val)) (number->string (exact val)))
    (else (number->string val))))

(define (load-record-into-states result)
  (for-each
    (lambda (pair)
      (let* ((m    (car pair))
             (in-s (cadr pair))
             (val  (p-map-get result (metric-col-kw m))))
        (state-set! in-s (db-val->string val))))
    (zip2 *metrics* metric-input-states)))

; === Spin helpers ===

(define (spin-step m) (p-map-get m #:step 1))

(define (spin-clamp m val)
  (let* ((lo (p-map-get m #:min -2000000))
         (hi (p-map-get m #:max  2000000)))
    (if (< val lo) lo (if (> val hi) hi val))))

(define (spin-increment! m in-s)
  (let* ((step    (spin-step m))
         (cur     (let ((v (string->number (state-ref in-s)))) (if v v 0)))
         (new-val (spin-clamp m (+ cur step))))
    (state-set! in-s (format-number new-val (decimals-for-step step)))
    (trigger-auto-save!)))

(define (spin-decrement! m in-s)
  (let* ((step    (spin-step m))
         (cur     (let ((v (string->number (state-ref in-s)))) (if v v 0)))
         (new-val (spin-clamp m (- cur step))))
    (state-set! in-s (format-number new-val (decimals-for-step step)))
    (trigger-auto-save!)))

; === Field renderers ===

(define (number-field m in-s)
  (outlined-text-field #:value in-s
    #:on-change (lambda (v) (begin (state-set! in-s v) (trigger-auto-save!)))
    #:label (p-map-get m #:label) #:keyboard-type 'number #:fill-max-width #t))

(define (spin-field m in-s)
  (column #:spacing 4
    (text #:value (p-map-get m #:label) #:style 'label-medium)
    (row #:spacing 8 #:vertical-alignment 'center
      (button #:style 'outlined
        #:on-click (lambda () (spin-decrement! m in-s))
        (text #:value "−"))
      (text #:value in-s #:style 'title-large)
      (button #:style 'outlined
        #:on-click (lambda () (spin-increment! m in-s))
        (text #:value "+")))))

(define (slider-field m in-s)
  (let* ((mn  (p-map-get m #:min 0))
         (mx  (p-map-get m #:max 100))
         (dec (metric-decimals m))
         (cur (let ((v (string->number (state-ref in-s)))) (if v v mn))))
    (column #:spacing 4
      (text #:value (p-map-get m #:label) #:style 'label-medium)
      (row #:spacing 8 #:vertical-alignment 'center #:fill-max-width #t
        (text #:value (format-number mn dec) #:style 'label-medium)
        (slider #:value cur
                #:min mn #:max mx
                #:modifier '(("weight" 1.0))
                #:on-change (lambda (v) (begin (state-set! in-s (format-number v dec)) (trigger-auto-save!))))
        (text #:value (format-number mx dec) #:style 'label-medium))
      (text #:value in-s #:style 'label-medium))))

(define (toggle-field m in-s)
  (switch
    #:checked (if (string=? (state-ref in-s) "1") 1 0)
    #:on-change (lambda (v) (begin (state-set! in-s (if (= v 1) "1" "0")) (trigger-auto-save!)))
    (text #:value (p-map-get m #:label) #:style 'label-medium)))

(define (make-star-button in-s star-val filled)
  (button #:style 'text
    #:on-click (lambda () (begin (state-set! in-s (number->string star-val)) (trigger-auto-save!)))
    (icon #:name (if filled "star" "star_border")
          #:tint (if filled "#F57C00" "gray")
          #:size 28)))

(define (rating-field m in-s)
  (let* ((max-stars (p-map-get m #:max 5))
         (cur       (let ((v (string->number (state-ref in-s)))) (if v v 0))))
    (column #:spacing 4
      (text #:value (p-map-get m #:label) #:style 'label-medium)
      (apply row
        (append
          (list #:spacing 2)
          (map (lambda (i)
                 (make-star-button in-s (+ i 1) (< i cur)))
               (make-range max-stars)))))))

(define (choice-field m in-s)
  (let ((options (p-map-get m #:options)))
    (column #:spacing 4
      (text #:value (p-map-get m #:label) #:style 'label-medium)
      (apply column
        (append
          (list #:spacing 2)
          (map (lambda (opt)
                 (radio-button
                   #:selected in-s
                   #:value opt
                   #:on-select (lambda () (begin (state-set! in-s opt) (trigger-auto-save!)))
                   (text #:value opt)))
               options))))))

(define (metric-field-components m in-s)
  (let ((it (metric-input-type m)))
    (cond
      ((eq? it 'spin)    (spin-field   m in-s))
      ((eq? it 'slider)  (slider-field m in-s))
      ((eq? it 'toggle)  (toggle-field m in-s))
      ((eq? it 'rating)  (rating-field m in-s))
      ((eq? it 'choice)  (choice-field m in-s))
      (else              (number-field m in-s)))))

; === Save logic ===

(define (build-values-pmap)
  (let loop ((ms *metrics*) (is metric-input-states) (acc (p-map)))
    (if (null? ms)
        acc
        (loop (cdr ms) (cdr is)
              (p-map-assoc acc
                           (metric-col-kw (car ms))
                           (parse-input-value (car ms) (state-ref (car is))))))))

(define (finish-save)
  (state-set! save-in-progress #f)
  (if (state-ref save-dirty)
      (begin (state-set! save-dirty #f) (do-save))
      #f))

(define (do-save)
  (state-set! save-in-progress #t)
  (state-set! save-dirty #f)
  (let ((values-pmap (build-values-pmap)))
    (query-table-single
      '(#:from counters #:order-by (id #:desc))
      (lambda (existing error)
        (if (not error)
            (if existing
                (let ((rid (p-map-get existing #:id)))
                  (db-transaction
                    (lambda (tx)
                      (tx-delete tx counters #:where `(= id ,rid))
                      (tx-insert tx counters #:values values-pmap)
                      #t)
                    (lambda (success err)
                      (if (not err) (finish-save)))))
                (db-insert counters
                  #:values values-pmap
                  (lambda (id err)
                    (if (not err) (finish-save))))))))))

(define (trigger-auto-save!)
  (if (state-ref save-in-progress)
      (state-set! save-dirty #t)
      (do-save)))

; === Init & App ===

(define (init-app)
  (query-table-single
    '(#:from counters #:order-by (id #:desc))
    (lambda (result error)
      (if (and (not error) result)
          (load-record-into-states result)))))

(define (app)
  (scaffold
    #:on-start init-app
    #:top-bar (top-app-bar #:title *app-title* #:style 'center-aligned)
    (apply lazy-column
      (append
        (list #:padding 16 #:spacing 16 #:fill-max-size #t)
        (map (lambda (pair)
               (list-item #:key (keyword->string (metric-col-kw (car pair)))
                 (metric-field-components (car pair) (cadr pair))))
             (zip2 *metrics* metric-input-states))))))

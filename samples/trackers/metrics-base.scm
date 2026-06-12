; Generic tracker engine.
; Define the following variables before (load-relative "../metrics-base.scm"):
;
;   Required:
;     *metrics*            — list of p-map field definitions (see below)
;     *app-title*          — string shown in the top bar
;     *default-from-last*  — #t to pre-fill new entries from the previous record,
;                            #f for blank form each time
;
;   Optional:
;     *auto-save*          — #t to save on every value change, no buttons needed;
;                            #f for the standard manual-save flow (default)
;
; Each entry in *metrics* is a p-map with these keys:
;
;   Required:
;     #:label      "Display Label"      — shown in form and history
;     #:col-name   #:some-col-name      — keyword naming the DB column
;     #:type       #:real               — ORM type: #:real #:int #:string #:text
;
;   Optional:
;     #:required   #t                   — field must have a value (default: optional)
;     #:input-type 'number              — rendering/input widget (see below)
;     #:step       2.5                  — spin: increment amount (default 1)
;     #:min        0                    — spin/slider: lower bound
;     #:max        100                  — spin/slider: upper bound
;     #:options    '("A" "B" "C")       — choice: list of selectable string values
;
; Input types:
;   'number   (default)  — text field, numeric keyboard
;   'spin                — [-] [value] [+] buttons with configurable step
;   'slider              — continuous slider between min/max
;   'toggle              — on/off switch (stored as 0/1, use #:type #:int)
;   'rating              — 1-N star rating (default 5 stars, configure with #:max)
;   'choice              — radio-button group, options from #:options list
;
; === DB Schema ===

(define (build-table-def)
  (append
    (list 'entries
          (list 'id '#:serial)
          (list 'date '#:string '#:not-null '#:unique))
    (map (lambda (m)
           (list (string->symbol (keyword->string (p-map-get m #:col-name)))
                 (p-map-get m #:type)))
         *metrics*)
    (list (list 'notes '#:text))))

(define-table (build-table-def))

; === State ===

(define current-tab (state 0))
(define notes-input (state ""))
(define view-mode (state 'entry))
(define today-date (state ""))
(define entries-list (state '()))
(define current-entry-id (state 0))
(define editing-date (state ""))
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
(define metric-error-states   (map (lambda (m) (state "")) *metrics*))
(define metric-default-states (map (lambda (m) (state "")) *metrics*))

; === Helpers ===

(define (zip2 l1 l2)
  (if (or (null? l1) (null? l2))
      '()
      (cons (list (car l1) (car l2))
            (zip2 (cdr l1) (cdr l2)))))

(define (zip3 l1 l2 l3)
  (if (or (null? l1) (null? l2) (null? l3))
      '()
      (cons (list (car l1) (car l2) (car l3))
            (zip3 (cdr l1) (cdr l2) (cdr l3)))))

(define (zip4 l1 l2 l3 l4)
  (if (or (null? l1) (null? l2) (null? l3) (null? l4))
      '()
      (cons (list (car l1) (car l2) (car l3) (car l4))
            (zip4 (cdr l1) (cdr l2) (cdr l3) (cdr l4)))))

; Generate list (0 1 2 ... n-1)
(define (make-range n)
  (define (loop i acc)
    (if (< i 0) acc (loop (- i 1) (cons i acc))))
  (loop (- n 1) '()))

(define (metric-col-kw m) (p-map-get m #:col-name))

(define (metric-input-type m)
  (p-map-get m #:input-type 'number))

; === Validation ===

(define (constrained-input? m)
  (let ((it (metric-input-type m)))
    (or (eq? it 'toggle)
        (eq? it 'choice)
        (eq? it 'rating)
        (eq? it 'slider)
        (eq? it 'spin))))

(define (is-valid-number? str)
  (if (string=? str "") #t (not (not (string->number str)))))

(define (is-positive-or-zero? str)
  (if (string=? str "")
      #t
      (let ((n (string->number str)))
        (if n (>= n 0) #f))))

(define (validate-form)
  (for-each (lambda (es) (state-set! es "")) metric-error-states)
  (let ((valid #t))
    (for-each
      (lambda (quad)
        (let* ((m    (car quad))
               (in-s (cadr quad))
               (er-s (caddr quad))
               (req  (p-map-get m #:required #f))
               (str  (state-ref in-s)))
          (cond
            ((and req (string=? str ""))
             (begin (state-set! er-s (string-append (p-map-get m #:label) " is required"))
                    (set! valid #f)))
            ((and (not (constrained-input? m))
                  (not (string=? str ""))
                  (not (is-valid-number? str)))
             (begin (state-set! er-s "Must be a valid number")
                    (set! valid #f))))))
      (zip4 *metrics* metric-input-states metric-error-states metric-default-states))
    valid))

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

; Format a number cleanly: whole-number floats become integers ("11.0" -> "11")
(define (format-number n decimals)
  (if (= decimals 0)
      (number->string (exact (round n)))
      (let ((factor (expt 10 decimals)))
        (number->string (/ (round (* n factor)) factor)))))

; Decimal places implied by a step size (0.1 -> 1, 2.5 -> 1, 1 -> 0, 5 -> 0)
(define (decimals-for-step step)
  (cond ((>= step 1)    0)
        ((>= step 0.1)  1)
        ((>= step 0.01) 2)
        (else           3)))

; Decimal places for display based on metric type (#:int -> 0, else -> 1)
(define (metric-decimals m)
  (if (string=? (keyword->string (p-map-get m #:type)) "int") 0 1))

; Convert a DB value to an input-state string
(define (db-val->string val)
  (cond
    ((db-null? val) "")
    ((string? val) val)
    ((= val (floor val)) (number->string (exact val)))
    (else (number->string val))))

(define (load-entry-into-states result)
  (for-each
    (lambda (pair)
      (let* ((m    (car pair))
             (in-s (cadr pair))
             (val  (p-map-get result (metric-col-kw m))))
        (state-set! in-s (db-val->string val))))
    (zip2 *metrics* metric-input-states))
  (let ((n (p-map-get result #:notes)))
    (state-set! notes-input (if (db-null? n) "" n))))

(define (load-entry-into-default-states result)
  (for-each
    (lambda (pair)
      (let* ((m     (car pair))
             (def-s (cadr pair))
             (val   (p-map-get result (metric-col-kw m))))
        (state-set! def-s (db-val->string val))))
    (zip2 *metrics* metric-default-states)))

; Reset all input states to their blank/zero starting values
(define (reset-inputs!)
  (for-each
    (lambda (pair)
      (let* ((m  (car pair))
             (s  (cadr pair))
             (it (metric-input-type m)))
        (state-set! s
          (cond
            ((eq? it 'spin)   "0")
            ((eq? it 'slider) (number->string (p-map-get m #:min 0)))
            ((eq? it 'toggle) "0")
            (else "")))))
    (zip2 *metrics* metric-input-states))
  (for-each (lambda (s) (state-set! s "")) metric-default-states)
  (state-set! notes-input ""))

(define (reset-errors!)
  (for-each (lambda (s) (state-set! s "")) metric-error-states))

; === Spin helpers ===

(define (spin-step m)
  (p-map-get m #:step 1))

(define (spin-clamp m val)
  (let* ((lo (p-map-get m #:min -2000000))
         (hi (p-map-get m #:max  2000000)))
    (if (< val lo) lo (if (> val hi) hi val))))

(define (spin-increment! m in-s er-s)
  (let* ((step    (spin-step m))
         (cur     (let ((v (string->number (state-ref in-s)))) (if v v 0)))
         (new-val (spin-clamp m (+ cur step))))
    (state-set! in-s (format-number new-val (decimals-for-step step)))
    (state-set! er-s "")
    (trigger-auto-save!)))

(define (spin-decrement! m in-s er-s)
  (let* ((step    (spin-step m))
         (cur     (let ((v (string->number (state-ref in-s)))) (if v v 0)))
         (new-val (spin-clamp m (- cur step))))
    (state-set! in-s (format-number new-val (decimals-for-step step)))
    (state-set! er-s "")
    (trigger-auto-save!)))

; === Modification indicator ===

(define (is-modified? in-s default-s)
  (and *default-from-last*
       (not (string=? (state-ref default-s) ""))
       (not (string=? (state-ref in-s) (state-ref default-s)))))

; Label row: "My Label" [● if modified]
(define (metric-field-label m in-s default-s)
  (let ((label    (p-map-get m #:label))
        (modified (is-modified? in-s default-s)))
    (row #:spacing 6 #:vertical-alignment 'center
      (text #:value label #:style 'label-medium)
      (if modified
          (text #:value "●" #:color "#E65100" #:style 'label-small)
          (spacer #:width 0)))))

; === Field renderers — each returns a single column UIElement ===

(define (number-field m in-s er-s default-s)
  (let* ((base-label (p-map-get m #:label))
         (modified   (is-modified? in-s default-s))
         (label      (if modified (string-append base-label " ●") base-label)))
    (column #:spacing 4
      (outlined-text-field #:value in-s
        #:on-change (lambda (v) (begin (state-set! in-s v) (state-set! er-s "") (trigger-auto-save!)))
        #:label label #:keyboard-type 'number #:fill-max-width #t)
      (if (not (string=? (state-ref er-s) ""))
          (text #:value (state-ref er-s) #:color "red" #:style 'label-small)
          (spacer #:height 0)))))

(define (spin-field m in-s er-s default-s)
  (column #:spacing 4
    (metric-field-label m in-s default-s)
    (row #:spacing 8 #:vertical-alignment 'center
      (button #:style 'outlined
        #:on-click (lambda () (spin-decrement! m in-s er-s))
        (text #:value "−"))
      (text #:value in-s #:style 'title-large)
      (button #:style 'outlined
        #:on-click (lambda () (spin-increment! m in-s er-s))
        (text #:value "+")))
    (if (not (string=? (state-ref er-s) ""))
        (text #:value er-s #:color "red" #:style 'label-small)
        (spacer #:height 0))))

(define (slider-field m in-s er-s default-s)
  (let* ((mn  (p-map-get m #:min 0))
         (mx  (p-map-get m #:max 100))
         (dec (metric-decimals m))
         (cur (let ((v (string->number (state-ref in-s)))) (if v v mn))))
    (column #:spacing 4
      (metric-field-label m in-s default-s)
      (row #:spacing 8 #:vertical-alignment 'center #:fill-max-width #t
        (text #:value (format-number mn dec) #:style 'label-medium)
        (slider #:value cur
                #:min mn #:max mx
                #:modifier '(("weight" 1.0))
                #:on-change (lambda (v) (begin (state-set! in-s (format-number v dec)) (trigger-auto-save!))))
        (text #:value (format-number mx dec) #:style 'label-medium))
      (text #:value in-s #:style 'label-medium))))

(define (toggle-field m in-s default-s)
  (let ((checked (string=? (state-ref in-s) "1")))
    (column #:spacing 0
      (switch
        #:checked (if checked 1 0)
        #:on-change (lambda (v) (begin (state-set! in-s (if (= v 1) "1" "0")) (trigger-auto-save!)))
        (metric-field-label m in-s default-s)))))

(define (make-star-button in-s star-val filled)
  (button #:style 'text
    #:on-click (lambda () (begin (state-set! in-s (number->string star-val)) (trigger-auto-save!)))
    (icon #:name (if filled "star" "star_border")
          #:tint (if filled "#F57C00" "gray")
          #:size 28)))

(define (rating-field m in-s default-s)
  (let* ((max-stars (p-map-get m #:max 5))
         (cur       (let ((v (string->number (state-ref in-s)))) (if v v 0))))
    (column #:spacing 4
      (metric-field-label m in-s default-s)
      (apply row
        (append
          (list #:spacing 2)
          (map (lambda (i)
                 (make-star-button in-s (+ i 1) (< i cur)))
               (make-range max-stars)))))))

(define (choice-field m in-s default-s)
  (let ((options (p-map-get m #:options)))
    (column #:spacing 4
      (metric-field-label m in-s default-s)
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

; Returns a single column UIElement for the given metric.
(define (metric-field-components m in-s er-s default-s)
  (let ((it (metric-input-type m)))
    (cond
      ((eq? it 'spin)    (spin-field   m in-s er-s default-s))
      ((eq? it 'slider)  (slider-field m in-s er-s default-s))
      ((eq? it 'toggle)  (toggle-field m in-s default-s))
      ((eq? it 'rating)  (rating-field m in-s default-s))
      ((eq? it 'choice)  (choice-field m in-s default-s))
      (else              (number-field m in-s er-s default-s)))))

; === App actions ===

(define (get-target-date)
  (if (string=? (state-ref editing-date) "")
      (state-ref today-date)
      (state-ref editing-date)))

(define (finish-save)
  (if *auto-save*
      (begin
        (state-set! save-in-progress #f)
        (if (state-ref save-dirty)
            (begin (state-set! save-dirty #f) (save-entry-auto))
            (load-history)))
      (begin
        (if (string=? (state-ref editing-date) "")
            (state-set! view-mode 'saved)
            (state-set! current-tab 1))
        (state-set! editing-date "")
        (load-history))))

(define (load-history)
  (query-table
    '(#:from entries #:order-by (date #:desc))
    (lambda (results error)
      (if (not error)
          (state-set! entries-list results)))))

(define (do-insert values-pmap on-success)
  (db-insert entries
    #:values values-pmap
    (lambda (id err)
      (if (not err)
          (begin
            (state-set! current-entry-id id)
            (on-success))))))

(define (do-replace entry-id values-pmap on-success)
  (let ((new-id-cell (list 0)))
    (db-transaction
      (lambda (tx)
        (tx-delete tx entries #:where `(= id ,entry-id))
        (set-car! new-id-cell (tx-insert tx entries #:values values-pmap))
        #t)
      (lambda (success err)
        (if (not err)
            (begin
              (state-set! current-entry-id (car new-id-cell))
              (on-success)))))))

(define (build-values-pmap target-date)
  (let loop ((ms *metrics*) (is metric-input-states)
             (acc (p-map #:date target-date #:notes (state-ref notes-input))))
    (if (null? ms)
        acc
        (loop (cdr ms) (cdr is)
              (p-map-assoc acc
                           (metric-col-kw (car ms))
                           (parse-input-value (car ms) (state-ref (car is))))))))

(define (save-entry-with-pmap target-date values-pmap)
  (query-table-single
    '(#:from entries #:where (= date ?date) #:params (date))
    #:date target-date
    (lambda (result error)
      (if (not error)
          (if (not result)
              (do-insert values-pmap finish-save)
              (do-replace (p-map-get result #:id) values-pmap finish-save))))))

(define (save-entry)
  (if (validate-form)
      (let ((d (get-target-date)))
        (save-entry-with-pmap d (build-values-pmap d)))))

(define (save-entry-auto)
  (state-set! save-in-progress #t)
  (state-set! save-dirty #f)
  (let ((d (get-target-date)))
    (save-entry-with-pmap d (build-values-pmap d))))

(define (trigger-auto-save!)
  (if *auto-save*
      (if (state-ref save-in-progress)
          (state-set! save-dirty #t)
          (save-entry-auto))))

(define (load-previous-as-defaults)
  (reset-inputs!)
  (query-table
    '(#:from entries #:order-by (date #:desc))
    (lambda (results error)
      (if (and (not error) (not (null? results)))
          (begin
            (load-entry-into-states (car results))
            (load-entry-into-default-states (car results))))
      (state-set! view-mode 'entry))))

(define (init-app)
  (let ((today (current-date-string)))
    (state-set! today-date today)
    (query-table-single
      '(#:from entries #:where (= date ?date) #:params (date))
      #:date today
      (lambda (result error)
        (if (not error)
            (if result
                (begin
                  (state-set! current-entry-id (p-map-get result #:id))
                  (load-entry-into-states result)
                  (state-set! view-mode (if *auto-save* 'edit 'saved)))
                (if *default-from-last*
                    (load-previous-as-defaults)
                    (begin
                      (reset-inputs!)
                      (state-set! view-mode 'entry)))))))
    (load-history)))

(define (start-edit)
  (state-set! view-mode 'edit))

(define (new-entry)
  (reset-inputs!)
  (reset-errors!)
  (state-set! view-mode 'entry)
  (state-set! current-entry-id 0)
  (state-set! editing-date ""))

(define (edit-historical-entry entry)
  (state-set! editing-date (p-map-get entry #:date))
  (state-set! current-entry-id (p-map-get entry #:id))
  (load-entry-into-states entry)
  (reset-errors!)
  (state-set! view-mode 'edit)
  (state-set! current-tab 0))

(define (cancel-edit)
  (state-set! editing-date "")
  (state-set! current-tab 1))

; === UI Components ===

; Returns a list of list-item elements with stable keys — for use in lazy-column.
(define (entry-form-items button-text)
  (append
    (map (lambda (quad)
           (list-item #:key (keyword->string (metric-col-kw (car quad)))
             (metric-field-components
               (car quad) (cadr quad) (caddr quad) (cadddr quad))))
         (zip4 *metrics* metric-input-states metric-error-states metric-default-states))
    (list
      (list-item #:key "notes"
        (outlined-text-field #:value notes-input
          #:on-change (lambda (v) (begin (state-set! notes-input v) (trigger-auto-save!)))
          #:label "Notes (optional)" #:max-lines 3 #:fill-max-width #t))
      (list-item #:key "save-buttons"
        (cond
          (*auto-save*
           (if (not (string=? (state-ref editing-date) ""))
               (button #:style 'outlined #:on-click cancel-edit
                 (text #:value "Done"))
               (spacer #:height 0)))
          ((not (string=? (state-ref editing-date) ""))
           (row #:spacing 12 #:fill-max-width #t
             (button #:style 'outlined #:on-click cancel-edit
               (text #:value "Cancel"))
             (button #:style 'filled #:on-click save-entry
               (text #:value button-text))))
          (else
           (button #:style 'filled #:on-click save-entry
             (text #:value button-text))))))))

; Returns a list of list-item elements with stable keys — for use in lazy-column.
(define (saved-view-items)
  (list
    (list-item #:key "saved-header"
      (column #:horizontal-alignment 'center #:fill-max-width #t #:spacing 8
        (icon #:name "check_circle" #:size 64 #:tint "#4CAF50")
        (text #:value "Entry Saved!" #:style 'headline-small)
        (text #:value (date->display-string (state-ref today-date))
              #:style 'body-medium #:color "gray")))
    (list-item #:key "saved-summary"
      (card #:padding 16 #:fill-max-width #t
        (apply column
          (append
            (list #:spacing 8)
            (map (lambda (pair)
                   (let* ((m   (car pair))
                          (s   (cadr pair))
                          (val (state-ref s)))
                     (if (not (string=? val ""))
                         (row #:spacing 8
                           (text #:value (string-append (p-map-get m #:label) ":")
                                 #:style 'label-large)
                           (text #:value val #:style 'body-medium))
                         (spacer #:height 0))))
                 (zip2 *metrics* metric-input-states))
            (list
              (if (not (string=? (state-ref notes-input) ""))
                  (column #:spacing 4
                    (text #:value "Notes:" #:style 'label-large)
                    (text #:value (state-ref notes-input) #:style 'body-medium))
                  (spacer #:height 0)))))))
    (list-item #:key "saved-actions"
      (row #:spacing 12 #:fill-max-width #t
        (button #:style 'outlined #:on-click start-edit (text #:value "Edit"))
        (button #:style 'filled #:on-click new-entry (text #:value "New Entry"))))))

(define (home-screen)
  (let ((mode    (state-ref view-mode))
        (today   (state-ref today-date))
        (editing (state-ref editing-date)))
    (apply lazy-column
      (append
        (list #:padding 16 #:spacing 16 #:fill-max-size #t)
        (list
          (list-item #:key "date-card"
            (card #:padding 16 #:fill-max-width #t
              (column #:spacing 8
                (text #:value (if (string=? editing "") "Today's Date" "Editing Entry")
                      #:style 'label-large #:color "gray")
                (text #:value (if (string=? editing "")
                                  (if (string=? today "") "Loading..." (date->display-string today))
                                  (date->display-string editing))
                      #:style 'title-large)))))
        (if (and (eq? mode 'saved) (not *auto-save*))
            (saved-view-items)
            (entry-form-items (if (eq? mode 'edit) "Update Entry" "Save Entry")))))))

(define (render-entry-card entry)
  (let ((entry-date (p-map-get entry #:date)))
    (card #:padding 16 #:fill-max-width #t
      (column #:spacing 12
        (row #:spacing 8 #:vertical-alignment 'center #:fill-max-width #t
          (text #:value (date->display-string entry-date)
                #:style 'title-small)
          (spacer #:modifier '(("weight" 1.0)))
          (button #:style 'outlined
            #:on-click (lambda () (edit-historical-entry entry))
            (text #:value "Edit")))
        (apply row
          (append
            (list #:spacing 12 #:wrap #t)
            (map (lambda (m)
                   (let ((val (p-map-get entry (metric-col-kw m))))
                     (if (not (db-null? val))
                         (text #:value (string-append (p-map-get m #:label) ": " (db-val->string val))
                               #:style 'body-small)
                         (spacer #:width 0))))
                 *metrics*)))
        (if (not (db-null? (p-map-get entry #:notes)))
            (text #:value (p-map-get entry #:notes)
                  #:style 'body-small #:color "gray" #:max-lines 2)
            (spacer #:height 0))))))

(define (history-screen)
  (let ((entries (state-ref entries-list)))
    (if (null? entries)
        (column #:spacing 16 #:padding 32
                #:horizontal-alignment 'center #:vertical-alignment 'center
                #:fill-max-size #t
          (icon #:name "calendar_today" #:size 64 #:tint "gray")
          (text #:value "No entries yet" #:style 'title-medium #:color "gray")
          (text #:value "Start tracking!" #:style 'body-medium #:color "gray"))
        (column #:fill-max-size #t
          (column #:padding 16
            (text #:value (string-append "Total Entries: " (number->string (length entries)))
                  #:style 'label-large #:color "gray"))
          (dynamic-list #:items entries-list #:render-item render-entry-card
                        #:spacing 12 #:padding 16
                        #:modifier '(("weight" 1.0)))))))

(define (app)
  (scaffold
    #:on-start init-app
    #:top-bar (top-app-bar #:title *app-title* #:style 'center-aligned)
    #:bottom-bar (bottom-navigation #:selected current-tab
      (nav-item #:icon "home" #:label "Today" #:value 0
        #:on-select (lambda () (state-set! current-tab 0)))
      (nav-item #:icon "list" #:label "History" #:value 1
        #:on-select (lambda () (begin (load-history) (state-set! current-tab 1)))))
    (switch-view #:selected current-tab
      (view #:value 0 (home-screen))
      (view #:value 1 (history-screen)))))

; Generic daily-metrics engine.
; Reads *metrics* (defined in the loading file) and builds everything dynamically.
;
; Each entry in *metrics* is a p-map with:
;   #:label    - Display label for the field (string)
;   #:col-name - DB column as keyword, e.g. #:weight, #:body-fat
;   #:type     - ORM column type keyword, e.g. #:real, #:int, #:string
;   #:required - (optional) #t if the field must be filled in

; === Default app title ===
(define *app-title* "Daily Metrics")

; === DB Schema: build table definition from *metrics* ===

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

; Parallel lists of states, one per metric
(define metric-input-states (map (lambda (m) (state "")) *metrics*))
(define metric-error-states (map (lambda (m) (state "")) *metrics*))

; === Helpers ===

; Zip three lists into a list of (a b c) triples
(define (zip3 l1 l2 l3)
  (if (or (null? l1) (null? l2) (null? l3))
      '()
      (cons (list (car l1) (car l2) (car l3))
            (zip3 (cdr l1) (cdr l2) (cdr l3)))))

; Return the col-name keyword for a metric (used as DB key)
(define (metric-col-kw m) (p-map-get m #:col-name))

; === Validation ===

(define (is-valid-number? str)
  (if (string=? str "") #t (not (not (string->number str)))))

(define (is-positive-number? str)
  (if (string=? str "")
      #t
      (let ((n (string->number str)))
        (if n (> n 0) #f))))

(define (validate-form)
  (for-each (lambda (es) (state-set! es "")) metric-error-states)
  (let ((valid #t))
    (for-each
      (lambda (triple)
        (let* ((m     (car triple))
               (in-s  (cadr triple))
               (er-s  (caddr triple))
               (req   (p-map-get m #:required))
               (str   (state-ref in-s)))
          (cond
            ((and req (string=? str ""))
             (begin (state-set! er-s (string-append (p-map-get m #:label) " is required"))
                    (set! valid #f)))
            ((and (not (string=? str "")) (not (is-valid-number? str)))
             (begin (state-set! er-s "Must be a valid number")
                    (set! valid #f)))
            ((and (not (string=? str "")) (not (is-positive-number? str)))
             (begin (state-set! er-s "Must be positive")
                    (set! valid #f))))))
      (zip3 *metrics* metric-input-states metric-error-states))
    valid))

; === Data helpers ===

; Empty string → 'null atom (stored as SQL NULL); non-empty → parse as number
(define (parse-input-number str)
  (if (string=? str "") 'null (string->number str)))

; Build the p-map for INSERT/UPDATE using current input states
(define (build-values-pmap target-date)
  (let loop ((ms *metrics*) (is metric-input-states)
             (acc (p-map #:date target-date #:notes (state-ref notes-input))))
    (if (null? ms)
        acc
        (loop (cdr ms) (cdr is)
              (p-map-assoc acc
                           (metric-col-kw (car ms))
                           (parse-input-number (state-ref (car is))))))))

; Populate input states from a DB result row
(define (load-entry-into-states result)
  (for-each
    (lambda (triple)
      (let* ((m    (car triple))
             (in-s (cadr triple))
             (val  (p-map-get result (metric-col-kw m))))
        (state-set! in-s (if (db-null? val) "" (number->string val)))))
    (zip3 *metrics* metric-input-states metric-error-states))
  (let ((n (p-map-get result #:notes)))
    (state-set! notes-input (if (db-null? n) "" n))))

; Clear all metric inputs and notes
(define (reset-inputs!)
  (for-each (lambda (s) (state-set! s "")) metric-input-states)
  (state-set! notes-input ""))

; Clear all error messages
(define (reset-errors!)
  (for-each (lambda (s) (state-set! s "")) metric-error-states))

; === App actions ===

(define (get-target-date)
  (if (string=? (state-ref editing-date) "")
      (state-ref today-date)
      (state-ref editing-date)))

(define (finish-save)
  (if (string=? (state-ref editing-date) "")
      (state-set! view-mode 'saved)
      (state-set! current-tab 1))
  (state-set! editing-date "")
  (load-history))

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

; Delete and re-insert atomically so UPDATE gets proper NULL handling
; and avoids the WHERE-param binding limitation in db-update.
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

(define (save-entry)
  (if (validate-form)
      (let* ((target-date  (get-target-date))
             (values-pmap  (build-values-pmap target-date)))
        (query-table-single
          '(#:from entries #:where (= date ?date) #:params (date))
          #:date target-date
          (lambda (result error)
            (if (not error)
                (if (not result)
                    (do-insert values-pmap finish-save)
                    (do-replace (p-map-get result #:id) values-pmap finish-save))))))))

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
                  (state-set! view-mode 'saved))
                (state-set! view-mode 'entry)))))
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

; Returns a list of (input-field, error-display) for one metric
(define (metric-field-components m in-s er-s)
  (list
    (outlined-text-field #:value in-s
      #:on-change (lambda (v) (begin (state-set! in-s v) (state-set! er-s "")))
      #:label (p-map-get m #:label) #:keyboard-type 'number #:fill-max-width #t)
    (if (not (string=? (state-ref er-s) ""))
        (text #:value (state-ref er-s) #:color 'error #:size 'small)
        (spacer #:height 0))))

(define (entry-form button-text)
  (apply column
    (append
      (list #:spacing 16 #:padding 16 #:fill-max-width #t)
      (apply append
        (map (lambda (triple)
               (metric-field-components (car triple) (cadr triple) (caddr triple)))
             (zip3 *metrics* metric-input-states metric-error-states)))
      (list
        (outlined-text-field #:value notes-input
          #:on-change (lambda (v) (state-set! notes-input v))
          #:label "Notes (optional)" #:max-lines 3 #:fill-max-width #t)
        (spacer #:height 16)
        (if (not (string=? (state-ref editing-date) ""))
            (row #:spacing 12 #:fill-max-width #t
              (button #:style 'outlined #:on-click cancel-edit
                (text #:value "Cancel"))
              (button #:style 'filled #:on-click save-entry
                (text #:value button-text)))
            (button #:style 'filled #:on-click save-entry
              (text #:value button-text)))))))

(define (saved-view)
  (column #:spacing 24 #:padding 24 #:horizontal-alignment 'center #:fill-max-width #t
    (icon #:name "check_circle" #:size 64 #:tint 'primary)
    (text #:value "Metrics Saved!" #:size 'large #:weight 'bold)
    (text #:value (date->display-string (state-ref today-date)) #:size 'medium #:color 'secondary)
    (card #:padding 16 #:fill-max-width #t
      (apply column
        (append
          (list #:spacing 8)
          (map (lambda (triple)
                 (let* ((m   (car triple))
                        (s   (cadr triple))
                        (val (state-ref s)))
                   (if (not (string=? val ""))
                       (row #:spacing 8
                         (text #:value (string-append (p-map-get m #:label) ":") #:weight 'bold)
                         (text #:value val))
                       (spacer #:height 0))))
               (zip3 *metrics* metric-input-states metric-error-states))
          (list
            (if (not (string=? (state-ref notes-input) ""))
                (column #:spacing 4
                  (text #:value "Notes:" #:weight 'bold)
                  (text #:value (state-ref notes-input)))
                (spacer #:height 0))))))
    (row #:spacing 12 #:fill-max-width #t
      (button #:style 'outlined #:on-click start-edit (text #:value "Edit"))
      (button #:style 'filled #:on-click new-entry (text #:value "New Entry")))))

(define (home-screen)
  (let ((mode    (state-ref view-mode))
        (today   (state-ref today-date))
        (editing (state-ref editing-date)))
    (column
      (column #:padding 16 #:spacing 16 #:fill-max-width #t
        (card #:padding 16
          (column #:spacing 8
            (text #:value (if (string=? editing "") "Today's Date" "Editing Entry")
                  #:size 'small #:color 'secondary)
            (text #:value (if (string=? editing "")
                              (if (string=? today "") "Loading..." (date->display-string today))
                              (date->display-string editing))
                  #:size 'large #:weight 'bold)))
        (if (eq? mode 'saved)
            (saved-view)
            (entry-form (if (eq? mode 'edit) "Update Entry" "Save Today's Metrics")))))))

(define (render-entry-card entry)
  (let ((entry-date (p-map-get entry #:date)))
    (card #:padding 16 #:fill-max-width #t
      (column #:spacing 12
        (row #:spacing 8 #:vertical-alignment 'center #:fill-max-width #t
          (text #:value (date->display-string entry-date) #:size 'medium #:weight 'bold)
          (spacer #:modifier-weight 1.0)
          (button #:style 'outlined #:on-click (lambda () (edit-historical-entry entry))
            (text #:value "Edit")))
        (apply row
          (append
            (list #:spacing 16 #:wrap #t)
            (map (lambda (m)
                   (let ((val (p-map-get entry (metric-col-kw m))))
                     (if (not (db-null? val))
                         (text #:value (string-append (p-map-get m #:label) ": " (number->string val))
                               #:size 'small)
                         (spacer #:width 0))))
                 *metrics*)))
        (if (not (db-null? (p-map-get entry #:notes)))
            (text #:value (p-map-get entry #:notes) #:size 'small #:color 'secondary #:max-lines 2)
            (spacer #:height 0))))))

(define (history-screen)
  (let ((entries (state-ref entries-list)))
    (if (null? entries)
        (column #:spacing 16 #:padding 32 #:horizontal-alignment 'center
                #:vertical-alignment 'center #:fill-max-size #t
          (icon #:name "calendar_today" #:size 64 #:tint 'secondary)
          (text #:value "No entries yet" #:size 'large #:color 'secondary)
          (text #:value "Start tracking your daily metrics!" #:size 'small #:color 'secondary))
        (column
          (column #:spacing 16 #:padding 16 #:fill-max-width #t
            (text #:value (string-append "Total Entries: " (number->string (length entries)))
                  #:size 'small #:color 'secondary)
            (dynamic-list #:items entries-list #:render-item render-entry-card #:spacing 12))))))

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

(db-table entries
  (id #:serial)
  (date #:string #:not-null #:unique)
  (weight #:real)
  (body-fat #:real)
  (muscle-mass #:real)
  (notes #:text))

(db-query-single find-today-entry
  #:from entries
  #:where (= date ?date)
  #:params (date))

(db-query all-entries-sorted
  #:from entries
  #:order-by (date #:desc))

(define current-tab (state 0))
(define weight-input (state ""))
(define body-fat-input (state ""))
(define muscle-mass-input (state ""))
(define notes-input (state ""))
(define weight-error (state ""))
(define body-fat-error (state ""))
(define muscle-mass-error (state ""))
(define view-mode (state 'entry))
(define today-date (state ""))
(define entries-list (state '()))
(define current-entry-id (state 0))
(define editing-date (state ""))

(define (is-valid-number? str)
  (if (string=? str "")
      #t
      (not (not (string->number str)))))

(define (is-positive-number? str)
  (if (string=? str "")
      #t
      (let ((num (string->number str)))
        (if num (> num 0) #f))))

(define (validate-form)
  (let ((weight-str (state-ref weight-input))
        (body-fat-str (state-ref body-fat-input))
        (muscle-str (state-ref muscle-mass-input)))
    (state-set! weight-error "")
    (state-set! body-fat-error "")
    (state-set! muscle-mass-error "")
    (let ((valid #t))
      (if (string=? weight-str "")
          (begin
            (state-set! weight-error "Weight is required")
            (set! valid #f))
          (if (not (is-valid-number? weight-str))
              (begin
                (state-set! weight-error "Must be a valid number")
                (set! valid #f))
              (if (not (is-positive-number? weight-str))
                  (begin
                    (state-set! weight-error "Must be positive")
                    (set! valid #f)))))
      (if (and (not (string=? body-fat-str "")) (not (is-valid-number? body-fat-str)))
          (begin
            (state-set! body-fat-error "Must be a valid number")
            (set! valid #f)))
      (if (and (not (string=? muscle-str "")) (not (is-valid-number? muscle-str)))
          (begin
            (state-set! muscle-mass-error "Must be a valid number")
            (set! valid #f)))
      valid)))

(define (parse-input-number str)
  (if (string=? str "") #f (string->number str)))

(define (save-entry)
  (if (validate-form)
      (let ((target-date (if (string=? (state-ref editing-date) "")
                             (state-ref today-date)
                             (state-ref editing-date)))
            (weight-val (parse-input-number (state-ref weight-input)))
            (body-fat-val (parse-input-number (state-ref body-fat-input)))
            (muscle-val (parse-input-number (state-ref muscle-mass-input)))
            (notes-val (state-ref notes-input)))
        (find-today-entry
          (lambda (result error)
            (if (not error)
                (if (not result)
                    (db-insert entries
                      #:values (p-map #:date target-date #:weight weight-val
                                      #:body-fat body-fat-val #:muscle-mass muscle-val
                                      #:notes notes-val)
                      (lambda (id err)
                        (if (not err)
                            (begin
                              (state-set! current-entry-id id)
                              (if (string=? (state-ref editing-date) "")
                                  (state-set! view-mode 'saved)
                                  (state-set! current-tab 1))
                              (state-set! editing-date "")
                              (load-history)))))
                    (let ((entry-id (p-map-get result #:id)))
                      (db-update entries
                        #:set (p-map #:weight weight-val #:body-fat body-fat-val
                                     #:muscle-mass muscle-val #:notes notes-val)
                        #:where (= id ?id)
                        #:id entry-id
                        (lambda (count err)
                          (if (not err)
                              (begin
                                (state-set! current-entry-id entry-id)
                                (if (string=? (state-ref editing-date) "")
                                    (state-set! view-mode 'saved)
                                    (state-set! current-tab 1))
                                (state-set! editing-date "")
                                (load-history)))))))))
          #:date target-date))))

(define (load-history)
  (all-entries-sorted
    (lambda (results error)
      (if (not error)
          (state-set! entries-list results)))))

(define (init-app)
  (let ((today (current-date-string)))
    (state-set! today-date today)
    (find-today-entry
      (lambda (result error)
        (if (not error)
            (if result
                (begin
                  (state-set! current-entry-id (p-map-get result #:id))
                  (state-set! weight-input
                    (let ((w (p-map-get result #:weight)))
                      (if (db-null? w) "" (number->string w))))
                  (state-set! body-fat-input
                    (let ((bf (p-map-get result #:body-fat)))
                      (if (db-null? bf) "" (number->string bf))))
                  (state-set! muscle-mass-input
                    (let ((mm (p-map-get result #:muscle-mass)))
                      (if (db-null? mm) "" (number->string mm))))
                  (state-set! notes-input
                    (let ((n (p-map-get result #:notes)))
                      (if (db-null? n) "" n)))
                  (state-set! view-mode 'saved))
                (state-set! view-mode 'entry))))
      #:date today)
    (load-history)))

(define (start-edit)
  (state-set! view-mode 'edit))

(define (new-entry)
  (state-set! weight-input "")
  (state-set! body-fat-input "")
  (state-set! muscle-mass-input "")
  (state-set! notes-input "")
  (state-set! weight-error "")
  (state-set! body-fat-error "")
  (state-set! muscle-mass-error "")
  (state-set! view-mode 'entry)
  (state-set! current-entry-id 0)
  (state-set! editing-date ""))

(define (edit-historical-entry entry)
  (let ((entry-date (p-map-get entry #:date))
        (weight (p-map-get entry #:weight))
        (body-fat (p-map-get entry #:body-fat))
        (muscle-mass (p-map-get entry #:muscle-mass))
        (notes (p-map-get entry #:notes)))
    (state-set! editing-date entry-date)
    (state-set! current-entry-id (p-map-get entry #:id))
    (state-set! weight-input
      (if (db-null? weight) "" (number->string weight)))
    (state-set! body-fat-input
      (if (db-null? body-fat) "" (number->string body-fat)))
    (state-set! muscle-mass-input
      (if (db-null? muscle-mass) "" (number->string muscle-mass)))
    (state-set! notes-input
      (if (db-null? notes) "" notes))
    (state-set! weight-error "")
    (state-set! body-fat-error "")
    (state-set! muscle-mass-error "")
    (state-set! view-mode 'edit)
    (state-set! current-tab 0)))

(define (cancel-edit)
  (state-set! editing-date "")
  (state-set! current-tab 1))

(define (entry-form button-text)
  (column #:spacing 16 #:padding 16 #:fill-max-width #t
    (outlined-text-field #:value weight-input
      #:on-change (lambda (v) (begin (state-set! weight-input v) (state-set! weight-error "")))
      #:label "Weight (kg)" #:keyboard-type 'number #:fill-max-width #t)
    (if (not (string=? (state-ref weight-error) ""))
        (text #:value (state-ref weight-error) #:color 'error #:size 'small)
        (spacer #:height 0))
    (outlined-text-field #:value body-fat-input
      #:on-change (lambda (v) (begin (state-set! body-fat-input v) (state-set! body-fat-error "")))
      #:label "Body Fat (%)" #:keyboard-type 'number #:fill-max-width #t)
    (if (not (string=? (state-ref body-fat-error) ""))
        (text #:value (state-ref body-fat-error) #:color 'error #:size 'small)
        (spacer #:height 0))
    (outlined-text-field #:value muscle-mass-input
      #:on-change (lambda (v) (begin (state-set! muscle-mass-input v) (state-set! muscle-mass-error "")))
      #:label "Muscle Mass (kg)" #:keyboard-type 'number #:fill-max-width #t)
    (if (not (string=? (state-ref muscle-mass-error) ""))
        (text #:value (state-ref muscle-mass-error) #:color 'error #:size 'small)
        (spacer #:height 0))
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
          (text #:value button-text)))))

(define (saved-view)
  (column #:spacing 24 #:padding 24 #:horizontal-alignment 'center #:fill-max-width #t
    (icon #:name "check_circle" #:size 64 #:tint 'primary)
    (text #:value "Metrics Saved!" #:size 'large #:weight 'bold)
    (text #:value (date->display-string (state-ref today-date)) #:size 'medium #:color 'secondary)
    (card #:padding 16 #:fill-max-width #t
      (column #:spacing 8
        (if (not (string=? (state-ref weight-input) ""))
            (row #:spacing 8
              (text #:value "Weight:" #:weight 'bold)
              (text #:value (string-append (state-ref weight-input) " kg")))
            (spacer #:height 0))
        (if (not (string=? (state-ref body-fat-input) ""))
            (row #:spacing 8
              (text #:value "Body Fat:" #:weight 'bold)
              (text #:value (string-append (state-ref body-fat-input) "%")))
            (spacer #:height 0))
        (if (not (string=? (state-ref muscle-mass-input) ""))
            (row #:spacing 8
              (text #:value "Muscle Mass:" #:weight 'bold)
              (text #:value (string-append (state-ref muscle-mass-input) " kg")))
            (spacer #:height 0))
        (if (not (string=? (state-ref notes-input) ""))
            (column #:spacing 4
              (text #:value "Notes:" #:weight 'bold)
              (text #:value (state-ref notes-input)))
            (spacer #:height 0))))
    (row #:spacing 12 #:fill-max-width #t
      (button #:style 'outlined #:on-click start-edit (text #:value "Edit"))
      (button #:style 'filled #:on-click new-entry (text #:value "New Entry")))))

(define (home-screen)
  (let ((mode (state-ref view-mode))
        (today (state-ref today-date))
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
  (let ((entry-date (p-map-get entry #:date))
        (weight (p-map-get entry #:weight))
        (body-fat (p-map-get entry #:body-fat))
        (muscle-mass (p-map-get entry #:muscle-mass))
        (notes (p-map-get entry #:notes)))
    (card #:padding 16 #:fill-max-width #t
      (column #:spacing 12
        (row #:spacing 8 #:vertical-alignment 'center #:fill-max-width #t
          (text #:value (date->display-string entry-date) #:size 'medium #:weight 'bold)
          (spacer #:modifier-weight 1.0)
          (button #:style 'outlined #:on-click (lambda () (edit-historical-entry entry))
            (text #:value "Edit")))
        (row #:spacing 16 #:wrap #t
          (if (not (db-null? weight))
              (text #:value (string-append "Weight: " (number->string weight) " kg") #:size 'small)
              (spacer #:width 0))
          (if (not (db-null? body-fat))
              (text #:value (string-append "Fat: " (number->string body-fat) "%") #:size 'small)
              (spacer #:width 0))
          (if (not (db-null? muscle-mass))
              (text #:value (string-append "Muscle: " (number->string muscle-mass) " kg") #:size 'small)
              (spacer #:width 0)))
        (if (not (db-null? notes))
            (text #:value notes #:size 'small #:color 'secondary #:max-lines 2)
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
    #:top-bar (top-app-bar #:title "Daily Metrics" #:style 'center-aligned)
    #:bottom-bar (bottom-navigation #:selected current-tab
      (nav-item #:icon "home" #:label "Today" #:value 0
        #:on-select (lambda () (state-set! current-tab 0)))
      (nav-item #:icon "list" #:label "History" #:value 1
        #:on-select (lambda () (begin (load-history) (state-set! current-tab 1)))))
    (switch-view #:selected current-tab
      (view #:value 0 (home-screen))
      (view #:value 1 (history-screen)))))

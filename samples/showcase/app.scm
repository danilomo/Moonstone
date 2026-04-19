(define current-tab (state 0))
(define counter (state 0))
(define slider-value (state 50))
(define switch-on (state #f))
(define input-text (state ""))
(define show-dialog (state #f))
(define tasks (state (list)))
(define task-input (state ""))

(define task-count (derived (lambda () (length (state-ref tasks)))))
(define completed-count (derived (lambda ()
  (length (filter (lambda (t) (cdr t)) (state-ref tasks))))))

(define (home-tab)
  (lazy-column #:padding 24 #:spacing 20
    (column #:horizontal-alignment 'center #:fill-max-width #t
      (text #:value "Moonstone" #:style 'display-medium)
      (text #:value "Scheme-based Declarative UI" #:style 'title-medium #:color "gray"))

    (divider)

    (card #:style 'elevated #:padding 20 #:fill-max-width #t
      (column #:spacing 12
        (row #:vertical-alignment 'center #:spacing 12
          (icon #:name "code" #:size 32 #:tint "blue")
          (text #:value "Write UI in Scheme" #:style 'title-medium))
        (text #:value "Build native apps using familiar Lisp syntax with reactive state management.")))

    (card #:style 'elevated #:padding 20 #:fill-max-width #t
      (column #:spacing 12
        (row #:vertical-alignment 'center #:spacing 12
          (icon #:name "devices" #:size 32 #:tint "green")
          (text #:value "Cross-Platform" #:style 'title-medium))
        (text #:value "Desktop (Windows, macOS, Linux) and Android from a single codebase.")))

    (card #:style 'elevated #:padding 20 #:fill-max-width #t
      (column #:spacing 12
        (row #:vertical-alignment 'center #:spacing 12
          (icon #:name "bolt" #:size 32 #:tint "orange")
          (text #:value "Hot Reload" #:style 'title-medium))
        (text #:value "See changes instantly without restarting the application.")))

    (card #:style 'elevated #:padding 20 #:fill-max-width #t
      (column #:spacing 12
        (row #:vertical-alignment 'center #:spacing 12
          (icon #:name "storage" #:size 32 #:tint "purple")
          (text #:value "Built-in Database" #:style 'title-medium))
        (text #:value "SQLite ORM with transactions, migrations, and cross-platform support.")))))

(define (components-tab)
  (lazy-column #:padding 16 #:spacing 16
    (text #:value "Component Gallery" #:style 'headline-medium)

    (card #:style 'outlined #:padding 16 #:fill-max-width #t
      (column #:spacing 16
        (text #:value "Counter Demo" #:style 'title-medium)
        (row #:vertical-alignment 'center #:spacing 16
          (button #:style 'filled-tonal
                  #:on-click (lambda () (state-update! counter (lambda (n) (- n 1))))
            (icon #:name "remove"))
          (text #:value counter #:style 'display-small)
          (button #:style 'filled-tonal
                  #:on-click (lambda () (state-update! counter (lambda (n) (+ n 1))))
            (icon #:name "add"))
          (button #:style 'outlined
                  #:on-click (lambda () (state-set! counter 0))
            (text #:value "Reset")))))

    (card #:style 'outlined #:padding 16 #:fill-max-width #t
      (column #:spacing 12
        (text #:value "Slider" #:style 'title-medium)
        (text #:value (string-append "Value: " (number->string (state-ref slider-value))))
        (slider #:value slider-value #:min 0 #:max 100
                #:on-change (lambda (v) (state-set! slider-value v))
                #:fill-max-width #t)
        (progress-indicator #:style 'linear
                           #:value (/ (state-ref slider-value) 100.0)
                           #:fill-max-width #t)))

    (card #:style 'outlined #:padding 16 #:fill-max-width #t
      (column #:spacing 12
        (text #:value "Input Controls" #:style 'title-medium)
        (outlined-text-field #:value input-text #:label "Type something..."
                            #:on-change (lambda (v) (state-set! input-text v))
                            #:fill-max-width #t)
        (if (> (string-length (state-ref input-text)) 0)
            (text #:value (string-append "You typed: " (state-ref input-text)) #:color "gray")
            (text #:value ""))
        (row #:vertical-alignment 'center #:spacing 8
          (switch #:checked switch-on
                  #:on-change (lambda (v) (state-set! switch-on v)))
          (text #:value (if (state-ref switch-on) "Enabled" "Disabled")))))

    (card #:style 'outlined #:padding 16 #:fill-max-width #t
      (column #:spacing 12
        (text #:value "Chips & Badges" #:style 'title-medium)
        (row #:spacing 8
          (chip #:style 'assist #:label "Assist")
          (chip #:style 'suggestion #:label "Suggestion")
          (chip #:style 'filter #:label "Filter"))
        (row #:spacing 16
          (badge #:count 5 (icon #:name "mail" #:size 28))
          (badge #:count 99 (icon #:name "notifications" #:size 28))
          (badge (icon #:name "chat" #:size 28)))))

    (card #:style 'outlined #:padding 16 #:fill-max-width #t
      (column #:spacing 12
        (text #:value "Buttons" #:style 'title-medium)
        (row #:spacing 8
          (button #:on-click (lambda () (state-set! show-dialog #t))
            (text #:value "Filled"))
          (button #:style 'outlined #:on-click (lambda () #f)
            (text #:value "Outlined"))
          (button #:style 'text #:on-click (lambda () #f)
            (text #:value "Text")))
        (row #:spacing 12
          (fab #:style 'small #:on-click (lambda () #f)
            (icon #:name "add"))
          (fab #:on-click (lambda () #f)
            (icon #:name "edit"))
          (fab #:style 'extended #:label "Create" #:on-click (lambda () #f)
            (icon #:name "add")))))))

(define (add-task)
  (let ((text (state-ref task-input)))
    (if (> (string-length text) 0)
        (begin
          (state-update! tasks (lambda (lst) (cons (cons text #f) lst)))
          (state-set! task-input ""))
        #f)))

(define (toggle-task index)
  (state-update! tasks
    (lambda (lst)
      (let loop ((l lst) (i 0) (result (list)))
        (if (null? l)
            (reverse result)
            (if (= i index)
                (loop (cdr l) (+ i 1) (cons (cons (car (car l)) (not (cdr (car l)))) result))
                (loop (cdr l) (+ i 1) (cons (car l) result))))))))

(define (delete-task index)
  (state-update! tasks
    (lambda (lst)
      (let loop ((l lst) (i 0) (result (list)))
        (if (null? l)
            (reverse result)
            (if (= i index)
                (loop (cdr l) (+ i 1) result)
                (loop (cdr l) (+ i 1) (cons (car l) result))))))))

(define (data-tab)
  (column #:padding 16 #:spacing 16 #:fill-max-size #t
    (text #:value "Task Manager" #:style 'headline-medium)

    (card #:style 'filled #:padding 16 #:fill-max-width #t
      (row #:horizontal-arrangement 'space-between #:fill-max-width #t
        (column
          (text #:value "Tasks" #:style 'title-small #:color "gray")
          (text #:value task-count #:style 'headline-large))
        (column
          (text #:value "Completed" #:style 'title-small #:color "gray")
          (text #:value completed-count #:style 'headline-large))))

    (row #:spacing 8 #:fill-max-width #t
      (box #:modifier (list (list 'weight 1))
        (outlined-text-field #:value task-input #:label "New task..."
                            #:on-change (lambda (v) (state-set! task-input v))
                            #:fill-max-width #t))
      (button #:on-click add-task
        (icon #:name "add")))

    (if (= (state-ref task-count) 0)
        (column #:padding 32 #:horizontal-alignment 'center #:fill-max-width #t
          (icon #:name "check-circle" #:size 64 #:tint "gray")
          (text #:value "No tasks yet" #:style 'title-medium #:color "gray")
          (text #:value "Add a task above to get started" #:color "gray"))
        (lazy-column #:spacing 8
          (dynamic-list #:items tasks
            (lambda (task index)
              (card #:style 'outlined #:padding 12 #:fill-max-width #t
                (row #:vertical-alignment 'center #:spacing 12
                  (checkbox #:checked (cdr task)
                           #:on-change (lambda (v) (toggle-task index)))
                  (box #:modifier (list (list 'weight 1))
                    (text #:value (car task)
                          #:style (if (cdr task) 'body-medium 'body-large)
                          #:color (if (cdr task) "gray" "black")))
                  (button #:style 'text #:on-click (lambda () (delete-task index))
                    (icon #:name "delete" #:tint "red"))))))))))

(define (settings-tab)
  (lazy-column #:padding 16 #:spacing 16
    (text #:value "Settings" #:style 'headline-medium)

    (card #:style 'outlined #:fill-max-width #t
      (column
        (list-item
          (row #:vertical-alignment 'center #:spacing 16 #:padding 16
            (icon #:name "dark-mode" #:size 24)
            (column #:modifier (list (list 'weight 1))
              (text #:value "Dark Mode" #:style 'body-large)
              (text #:value "Enable dark theme" #:style 'body-small #:color "gray"))
            (switch #:checked (state #f))))
        (divider)
        (list-item
          (row #:vertical-alignment 'center #:spacing 16 #:padding 16
            (icon #:name "notifications" #:size 24)
            (column #:modifier (list (list 'weight 1))
              (text #:value "Notifications" #:style 'body-large)
              (text #:value "Enable push notifications" #:style 'body-small #:color "gray"))
            (switch #:checked (state #t))))
        (divider)
        (list-item
          (row #:vertical-alignment 'center #:spacing 16 #:padding 16
            (icon #:name "language" #:size 24)
            (column #:modifier (list (list 'weight 1))
              (text #:value "Language" #:style 'body-large)
              (text #:value "English" #:style 'body-small #:color "gray"))
            (icon #:name "chevron-right" #:size 24 #:tint "gray")))))

    (card #:style 'outlined #:fill-max-width #t
      (column
        (list-item
          (row #:vertical-alignment 'center #:spacing 16 #:padding 16
            (icon #:name "info" #:size 24)
            (column #:modifier (list (list 'weight 1))
              (text #:value "About" #:style 'body-large)
              (text #:value "Version 0.1.0" #:style 'body-small #:color "gray"))
            (icon #:name "chevron-right" #:size 24 #:tint "gray")))
        (divider)
        (list-item
          (row #:vertical-alignment 'center #:spacing 16 #:padding 16
            (icon #:name "code" #:size 24)
            (column #:modifier (list (list 'weight 1))
              (text #:value "Source Code" #:style 'body-large)
              (text #:value "View on GitHub" #:style 'body-small #:color "gray"))
            (icon #:name "chevron-right" #:size 24 #:tint "gray")))
        (divider)
        (list-item
          (row #:vertical-alignment 'center #:spacing 16 #:padding 16
            (icon #:name "description" #:size 24)
            (column #:modifier (list (list 'weight 1))
              (text #:value "Documentation" #:style 'body-large)
              (text #:value "Read the docs" #:style 'body-small #:color "gray"))
            (icon #:name "chevron-right" #:size 24 #:tint "gray")))))))

(define (app)
  (box
    (scaffold
      #:top-bar (top-app-bar #:title "Moonstone Showcase" #:style 'center-aligned)
      #:bottom-bar (bottom-navigation #:selected current-tab
        (nav-item #:icon "home" #:label "Home" #:value 0
                  #:on-select (lambda () (state-set! current-tab 0)))
        (nav-item #:icon "widgets" #:label "Components" #:value 1
                  #:on-select (lambda () (state-set! current-tab 1)))
        (nav-item #:icon "checklist" #:label "Data" #:value 2
                  #:on-select (lambda () (state-set! current-tab 2)))
        (nav-item #:icon "settings" #:label "Settings" #:value 3
                  #:on-select (lambda () (state-set! current-tab 3))))
      (switch-view #:selected current-tab
        (view #:value 0 (home-tab))
        (view #:value 1 (components-tab))
        (view #:value 2 (data-tab))
        (view #:value 3 (settings-tab))))

    (if (state-ref show-dialog)
        (alert-dialog
          #:title "Dialog Example"
          #:text "This is a modal dialog demonstrating Moonstone's alert-dialog component."
          #:confirm-button "OK"
          #:dismiss-button "Cancel"
          #:on-confirm (lambda () (state-set! show-dialog #f))
          #:on-dismiss (lambda () (state-set! show-dialog #f)))
        (box))))

(define current-tab (state 0))
(define slider-value (state 50))
(define progress-value (state 0.3))
(define chip-selected (state #f))
(define notification-count (state 3))

(define (cards-section)
  (column #:spacing 16 #:padding 16
    (text #:value "Cards" #:style 'title-large)
    (text #:value "Cards contain content and actions about a single subject.")

    (row #:spacing 12
      (card #:style 'filled #:padding 16
        (column #:spacing 8
          (text #:value "Filled Card" #:style 'title-medium)
          (text #:value "Default card style with subtle background.")))

      (card #:style 'elevated #:padding 16
        (column #:spacing 8
          (text #:value "Elevated Card" #:style 'title-medium)
          (text #:value "Card with shadow elevation."))))

    (card #:style 'outlined #:padding 16 #:fill-max-width #t
      (column #:spacing 8
        (text #:value "Outlined Card" #:style 'title-medium)
        (text #:value "Card with border outline, no elevation.")))))

(define (sliders-section)
  (column #:spacing 16 #:padding 16
    (text #:value "Slider" #:style 'title-large)
    (text #:value "Sliders allow users to select a value from a range.")

    (column #:spacing 8
      (text #:value (string-append "Value: " (number->string (state-ref slider-value))))
      (slider #:value slider-value
              #:min 0
              #:max 100
              #:on-change (lambda (v) (state-set! slider-value v))
              #:fill-max-width #t))

    (divider)

    (column #:spacing 8
      (text #:value "Slider with steps (10 intervals)")
      (slider #:value slider-value
              #:min 0
              #:max 100
              #:steps 11
              #:on-change (lambda (v) (state-set! slider-value v))
              #:fill-max-width #t))))

(define (progress-section)
  (column #:spacing 16 #:padding 16
    (text #:value "Progress Indicators" #:style 'title-large)
    (text #:value "Progress indicators show the status of ongoing processes.")

    (row #:spacing 24 #:vertical-alignment 'center
      (column #:spacing 8 #:horizontal-alignment 'center
        (text #:value "Circular")
        (progress-indicator #:style 'circular))

      (column #:spacing 8 #:horizontal-alignment 'center
        (text #:value "Determinate")
        (progress-indicator #:style 'circular #:value progress-value)))

    (divider)

    (column #:spacing 8
      (text #:value "Linear Indeterminate")
      (progress-indicator #:style 'linear #:fill-max-width #t))

    (column #:spacing 8
      (text #:value "Linear Determinate (30%)")
      (progress-indicator #:style 'linear #:value progress-value #:fill-max-width #t))

    (row #:spacing 8
      (button #:on-click (lambda ()
                (state-update! progress-value
                  (lambda (v) (if (< v 1.0) (+ v 0.1) 0.0))))
        (text #:value "Increase"))
      (button #:style 'outlined #:on-click (lambda () (state-set! progress-value 0.0))
        (text #:value "Reset")))))

(define (chips-section)
  (column #:spacing 16 #:padding 16
    (text #:value "Chips" #:style 'title-large)
    (text #:value "Chips help users enter information, make selections, and filter content.")

    (row #:spacing 8
      (chip #:style 'assist #:label "Assist Chip")
      (chip #:style 'suggestion #:label "Suggestion"))

    (divider)

    (text #:value "Filter Chips (toggleable)" #:style 'title-small)
    (row #:spacing 8
      (chip #:style 'filter
            #:label "Active"
            #:selected chip-selected
            #:on-select (lambda (v) (state-set! chip-selected (> v 0)))))

    (text #:value (if (state-ref chip-selected) "Filter is ON" "Filter is OFF"))))

(define (dividers-section)
  (column #:spacing 16 #:padding 16
    (text #:value "Dividers" #:style 'title-large)
    (text #:value "Dividers separate content into clear groups.")

    (surface #:padding 16 #:shape 'rounded #:elevation 1
      (column #:spacing 8
        (text #:value "Item 1")
        (divider)
        (text #:value "Item 2")
        (divider #:thickness 2 #:color "blue")
        (text #:value "Item 3 (thick blue divider above)")))))

(define (fab-section)
  (column #:spacing 16 #:padding 16
    (text #:value "Floating Action Buttons" #:style 'title-large)
    (text #:value "FABs represent the most important action on a screen.")

    (row #:spacing 16 #:vertical-alignment 'center
      (column #:spacing 4 #:horizontal-alignment 'center
        (fab #:on-click (lambda () (toast "FAB clicked"))
          (icon #:name "add"))
        (text #:value "Standard"))

      (column #:spacing 4 #:horizontal-alignment 'center
        (fab #:style 'small #:on-click (lambda () (toast "Small FAB"))
          (icon #:name "edit"))
        (text #:value "Small"))

      (column #:spacing 4 #:horizontal-alignment 'center
        (fab #:style 'large #:on-click (lambda () (toast "Large FAB"))
          (icon #:name "share"))
        (text #:value "Large")))

    (fab #:style 'extended
         #:label "Create New"
         #:on-click (lambda () (toast "Extended FAB"))
      (icon #:name "add"))))

(define (badge-section)
  (column #:spacing 16 #:padding 16
    (text #:value "Badges" #:style 'title-large)
    (text #:value "Badges show notifications, counts, or status.")

    (row #:spacing 24 #:vertical-alignment 'center
      (column #:spacing 4 #:horizontal-alignment 'center
        (badge (icon #:name "notifications" #:size 32))
        (text #:value "Dot badge"))

      (column #:spacing 4 #:horizontal-alignment 'center
        (badge #:count notification-count (icon #:name "mail" #:size 32))
        (text #:value "Count badge"))

      (column #:spacing 4 #:horizontal-alignment 'center
        (badge #:count 150 #:max-count 99 (icon #:name "chat" #:size 32))
        (text #:value "Max count")))

    (row #:spacing 8
      (button #:on-click (lambda () (state-update! notification-count (lambda (n) (+ n 1))))
        (text #:value "+1"))
      (button #:style 'outlined #:on-click (lambda () (state-set! notification-count 0))
        (text #:value "Clear")))))

(define (image-section)
  (column #:spacing 16 #:padding 16
    (text #:value "Image Placeholders" #:style 'title-large)
    (text #:value "Image components with various shapes and sizes.")

    (row #:spacing 16 #:vertical-alignment 'center
      (image #:placeholder "100x100" #:size 100 #:shape 'rectangle)
      (image #:placeholder "Circle" #:size 80 #:shape 'circle)
      (image #:placeholder "Rounded" #:width 120 #:height 80 #:shape 'rounded-large))))

(define (render-tab)
  (switch-view #:selected current-tab
    (view #:value 0 (cards-section))
    (view #:value 1 (sliders-section))
    (view #:value 2 (progress-section))
    (view #:value 3 (chips-section))
    (view #:value 4 (dividers-section))
    (view #:value 5 (fab-section))
    (view #:value 6 (badge-section))
    (view #:value 7 (image-section))))

(define (app)
  (scaffold
    #:top-bar (top-app-bar #:title "New Components Showcase" #:style 'center-aligned)
    #:bottom-bar (bottom-navigation #:selected current-tab
      (nav-item #:icon "view-agenda" #:label "Cards" #:value 0
                #:on-select (lambda () (state-set! current-tab 0)))
      (nav-item #:icon "tune" #:label "Slider" #:value 1
                #:on-select (lambda () (state-set! current-tab 1)))
      (nav-item #:icon "sync" #:label "Progress" #:value 2
                #:on-select (lambda () (state-set! current-tab 2)))
      (nav-item #:icon "label" #:label "Chips" #:value 3
                #:on-select (lambda () (state-set! current-tab 3))))
    (column #:fill-max-size #t
      (row #:padding 8 #:horizontal-arrangement 'center #:spacing 4
        (chip #:style 'filter #:label "Dividers"
              #:selected (state 0)
              #:on-click (lambda () (state-set! current-tab 4)))
        (chip #:style 'filter #:label "FAB"
              #:selected (state 0)
              #:on-click (lambda () (state-set! current-tab 5)))
        (chip #:style 'filter #:label "Badge"
              #:selected (state 0)
              #:on-click (lambda () (state-set! current-tab 6)))
        (chip #:style 'filter #:label "Image"
              #:selected (state 0)
              #:on-click (lambda () (state-set! current-tab 7))))
      (render-tab))))

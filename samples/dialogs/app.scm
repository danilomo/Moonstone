(define show-dialog (state #f))
(define show-sheet (state #f))
(define show-snackbar (state #f))
(define action-result (state ""))

(define (app)
  (column #:fill-max-size 1 #:padding 24 #:spacing 16
    (text #:value "Dialogs Demo" #:style 'headline-medium)

    (text #:value "Click buttons to show different dialog types:" #:style 'body-large)

    (spacer #:height 8)

    (button #:fill-max-width #t
            #:on-click (lambda () (state-set! show-dialog #t))
      (row #:spacing 8 #:vertical-alignment 'center
        (icon #:name "warning" #:size 20)
        (text #:value "Show Alert Dialog")))

    (button #:fill-max-width #t #:style 'tonal
            #:on-click (lambda () (state-set! show-sheet #t))
      (row #:spacing 8 #:vertical-alignment 'center
        (icon #:name "keyboard-arrow-up" #:size 20)
        (text #:value "Show Bottom Sheet")))

    (button #:fill-max-width #t #:style 'outlined
            #:on-click (lambda () (state-set! show-snackbar #t))
      (row #:spacing 8 #:vertical-alignment 'center
        (icon #:name "notifications" #:size 20)
        (text #:value "Show Snackbar")))

    (spacer #:height 16)

    (if (not (string=? (state-ref action-result) ""))
        (card #:style 'outlined #:padding 12 #:fill-max-width #t
          (text #:value (state-ref action-result)))
        (spacer #:height 0))

    (alert-dialog #:visible show-dialog
      #:title "Confirm Action"
      #:text "Are you sure you want to proceed with this action?"
      #:icon (icon #:name "warning" #:tint "red")
      #:confirm-button (button #:on-click (lambda ()
          (println "AlertDialog: Confirm button clicked")
          (state-set! action-result "Dialog: Confirmed!")
          (state-set! show-dialog #f))
        (text #:value "Confirm"))
      #:dismiss-button (button #:on-click (lambda ()
          (println "AlertDialog: Cancel button clicked")
          (state-set! action-result "Dialog: Cancelled")
          (state-set! show-dialog #f)) #:style 'text
        (text #:value "Cancel"))
      #:on-dismiss (lambda () (println "AlertDialog: on-dismiss called") (state-set! show-dialog #f)))

    (bottom-sheet #:visible show-sheet #:on-dismiss (lambda () (println "BottomSheet: on-dismiss called") (state-set! show-sheet #f))
      (column #:padding 24 #:spacing 16
        (text #:value "Bottom Sheet Content" #:style 'headline-small)
        (text #:value "This is a modal bottom sheet that slides up from the bottom of the screen.")
        (spacer #:height 8)
        (button #:on-click (lambda ()
            (println "BottomSheet: Option 1 clicked")
            (state-set! action-result "Sheet: Option 1 selected")
            (state-set! show-sheet #f))
          (text #:value "Option 1"))
        (button #:on-click (lambda ()
            (println "BottomSheet: Option 2 clicked")
            (state-set! action-result "Sheet: Option 2 selected")
            (state-set! show-sheet #f)) #:style 'outlined
          (text #:value "Option 2"))
        (spacer #:height 24)))

    (snackbar #:visible show-snackbar
      #:message "This is a snackbar notification"
      #:action-label "Undo"
      #:on-action (lambda ()
        (println "Snackbar: Undo action clicked")
        (state-set! action-result "Snackbar: Undo clicked")
        (state-set! show-snackbar #f))
      #:on-dismiss (lambda ()
        (println "Snackbar: on-dismiss called")
        (state-set! show-snackbar #f)))))

; Gamepad button tester - press physical buttons to highlight them green

(define active-button (state ""))

(on-key-down
  (lambda (key)
    (state-set! active-button key)))

(define (btn-color name)
  (if (string=? (state-ref active-button) name)
    "#1DB954"
    "#C0392B"))

(define (gamepad-btn name label)
  (surface
    #:color (btn-color name)
    #:shape 'circle
    #:width 56
    #:height 56
    (box
      #:fill-max-size #t
      #:content-alignment 'center
      (text #:value label #:color 'white #:style 'body-large))))

(define (app)
  (surface
    #:fill-max-size #t
    #:color "#1A1A2E"
    (column
      #:fill-max-size #t
      #:padding 24
      #:spacing 16
      #:horizontal-alignment 'center
      #:vertical-arrangement 'center

      (text #:value "Gamepad Tester" #:style 'headline-medium #:color 'white)

      (text
        #:value (if (string=? (state-ref active-button) "")
                  "Press a button..."
                  (string-append "Pressed: " (state-ref active-button)))
        #:color "#AAAAAA")

      (spacer #:height 8)

      ; Triggers
      (row
        #:fill-max-width #t
        #:horizontal-arrangement 'space-between
        (gamepad-btn "l2" "L2")
        (gamepad-btn "r2" "R2"))

      ; Shoulders
      (row
        #:fill-max-width #t
        #:horizontal-arrangement 'space-between
        (gamepad-btn "l1" "L1")
        (gamepad-btn "r1" "R1"))

      (spacer #:height 8)

      ; Main button area: dpad | center | face buttons
      (row
        #:spacing 24
        #:vertical-alignment 'center

        ; D-pad
        (column
          #:spacing 4
          #:horizontal-alignment 'center
          (gamepad-btn "dpad-up" "↑")
          (row #:spacing 4
            (gamepad-btn "dpad-left" "←")
            (spacer #:width 56)
            (gamepad-btn "dpad-right" "→"))
          (gamepad-btn "dpad-down" "↓"))

        ; Select / Start
        (column
          #:spacing 8
          #:horizontal-alignment 'center
          (gamepad-btn "select" "SEL")
          (gamepad-btn "start" "STA"))

        ; Face buttons (SNES layout: Y top-left, X left, A right, B bottom)
        (column
          #:spacing 4
          #:horizontal-alignment 'center
          (gamepad-btn "y" "Y")
          (row #:spacing 4
            (gamepad-btn "x" "X")
            (spacer #:width 56)
            (gamepad-btn "b" "B"))
          (gamepad-btn "a" "A"))))))

; Tetris for Moonstone
; Controls: dpad-left/right to move, dpad-up to hard drop, dpad-down to soft drop
;           a: rotate clockwise  |  b: rotate counter-clockwise
;           start: pause / restart after game over

; ===== CONSTANTS =====

(define COLS 10)
(define ROWS 20)
(define CELL 24)       ; dp per cell
(define BORDER 2)      ; gap around each cell (creates grid effect)
(define CELL-INNER (- CELL BORDER))

; ===== PIECE DATA =====
; Each piece: (color rot0 rot1 rot2 rot3)
; Each rotation: list of (col-offset row-offset) pairs within 4x4 bounding box

(define PIECES
  (list
    (list "#00BCD4"   ; 0: I - cyan
      (list (list 0 1) (list 1 1) (list 2 1) (list 3 1))
      (list (list 2 0) (list 2 1) (list 2 2) (list 2 3))
      (list (list 0 2) (list 1 2) (list 2 2) (list 3 2))
      (list (list 1 0) (list 1 1) (list 1 2) (list 1 3)))
    (list "#FFD700"   ; 1: O - yellow
      (list (list 1 0) (list 2 0) (list 1 1) (list 2 1))
      (list (list 1 0) (list 2 0) (list 1 1) (list 2 1))
      (list (list 1 0) (list 2 0) (list 1 1) (list 2 1))
      (list (list 1 0) (list 2 0) (list 1 1) (list 2 1)))
    (list "#AB47BC"   ; 2: T - purple
      (list (list 1 0) (list 0 1) (list 1 1) (list 2 1))
      (list (list 1 0) (list 1 1) (list 2 1) (list 1 2))
      (list (list 0 1) (list 1 1) (list 2 1) (list 1 2))
      (list (list 1 0) (list 0 1) (list 1 1) (list 1 2)))
    (list "#66BB6A"   ; 3: S - green
      (list (list 1 0) (list 2 0) (list 0 1) (list 1 1))
      (list (list 1 0) (list 1 1) (list 2 1) (list 2 2))
      (list (list 1 1) (list 2 1) (list 0 2) (list 1 2))
      (list (list 0 0) (list 0 1) (list 1 1) (list 1 2)))
    (list "#EF5350"   ; 4: Z - red
      (list (list 0 0) (list 1 0) (list 1 1) (list 2 1))
      (list (list 2 0) (list 1 1) (list 2 1) (list 1 2))
      (list (list 0 1) (list 1 1) (list 1 2) (list 2 2))
      (list (list 1 0) (list 0 1) (list 1 1) (list 0 2)))
    (list "#42A5F5"   ; 5: J - blue
      (list (list 0 0) (list 0 1) (list 1 1) (list 2 1))
      (list (list 1 0) (list 2 0) (list 1 1) (list 1 2))
      (list (list 0 1) (list 1 1) (list 2 1) (list 2 2))
      (list (list 1 0) (list 1 1) (list 0 2) (list 1 2)))
    (list "#FFA726"   ; 6: L - orange
      (list (list 2 0) (list 0 1) (list 1 1) (list 2 1))
      (list (list 1 0) (list 1 1) (list 1 2) (list 2 2))
      (list (list 0 1) (list 1 1) (list 2 1) (list 0 2))
      (list (list 0 0) (list 1 0) (list 1 1) (list 1 2)))))

(define (piece-color type)
  (car (list-ref PIECES type)))

(define (piece-shape type rot)
  (list-ref (list-ref PIECES type) (+ rot 1)))

; ===== PSEUDO-RANDOM NUMBER GENERATOR =====

(define rng 54321)

(define (rand-int! n)
  ; LCG with all values within 32-bit int range
  (set! rng (modulo (+ (* 75 rng) 74) 65537))
  (modulo rng n))

; ===== BOARD (mutable via set!) =====
; A list of ROWS rows, each row is a list of COLS cells (#f or a color string)

(define (make-empty-row)
  (let loop ((c 0))
    (if (= c COLS) '()
        (cons #f (loop (+ c 1))))))

(define (make-empty-board)
  (let loop ((r 0))
    (if (= r ROWS) '()
        (cons (make-empty-row) (loop (+ r 1))))))

; Functional list update: returns new list with element at idx replaced by val
(define (list-update lst idx val)
  (let loop ((rest lst) (i 0) (acc '()))
    (cond
      ((null? rest) (reverse acc))
      ((= i idx)   (loop (cdr rest) (+ i 1) (cons val acc)))
      (else         (loop (cdr rest) (+ i 1) (cons (car rest) acc))))))

(define board (make-empty-board))

(define (board-ref col row)
  (list-ref (list-ref board row) col))

(define (board-set! col row val)
  (set! board (list-update board row
                (list-update (list-ref board row) col val))))

; ===== MUTABLE GAME STATE =====

(define cur-type 0)
(define cur-x 3)
(define cur-y 0)
(define cur-rot 0)
(define next-type 1)
(define score 0)
(define level 1)
(define lines 0)
(define game-over #f)
(define paused #f)
(define tick-count 0)
(define soft-drop #f)          ; #t while soft-drop is active
(define soft-drop-left 0)      ; ticks remaining before soft-drop expires
(define SOFT-DROP-TICKS 3)     ; each dpad-down press keeps boost for 3 ticks (~150ms)

; Single reactive trigger: increment to force one re-render
(define render-tick (state 0))

(define (refresh!)
  (state-set! render-tick (+ (state-ref render-tick) 1)))

; ===== COLLISION DETECTION =====

(define (in-bounds? col row)
  (and (>= col 0) (< col COLS) (>= row 0) (< row ROWS)))

(define (cell-free? col row)
  (if (in-bounds? col row)
    (not (board-ref col row))
    #f))

(define (can-place? type rot px py)
  (let loop ((cells (piece-shape type rot)))
    (if (null? cells)
      #t
      (let* ((cell (car cells))
             (cx   (+ px (car cell)))
             (cy   (+ py (cadr cell))))
        (if (cell-free? cx cy)
          (loop (cdr cells))
          #f)))))

; ===== PIECE PLACEMENT =====

(define (place-piece!)
  (let ((color (piece-color cur-type))
        (cells (piece-shape cur-type cur-rot)))
    (for-each
      (lambda (cell)
        (board-set! (+ cur-x (car cell)) (+ cur-y (cadr cell)) color))
      cells)))

; ===== LINE CLEARING =====

(define (row-full? row)
  (let loop ((cells row))
    (if (null? cells)
      #t
      (if (car cells)
        (loop (cdr cells))
        #f))))

(define (clear-lines!)
  (let* ((remaining (filter (lambda (row) (not (row-full? row))) board))
         (cleared   (- ROWS (length remaining)))
         (new-top   (let loop ((n cleared))
                      (if (= n 0) '()
                          (cons (make-empty-row) (loop (- n 1)))))))
    (set! board (append new-top remaining))
    cleared))

; ===== SCORING =====

(define LINE-SCORES (list 0 100 300 500 800))

(define (add-score! cleared)
  (when (> cleared 0)
    (set! score (+ score (* level (list-ref LINE-SCORES (min cleared 4)))))
    (set! lines (+ lines cleared))
    (set! level (+ 1 (quotient lines 10)))))

; ===== PIECE SPAWN & GRAVITY =====

(define (gravity-ticks)
  (max 1 (- 14 (* level 1))))

(define (spawn-piece!)
  (set! cur-type next-type)
  (set! next-type (rand-int! 7))
  (set! cur-x 3)
  (set! cur-y 0)
  (set! cur-rot 0)
  (set! tick-count 0)
  (when (not (can-place? cur-type cur-rot cur-x cur-y))
    (set! game-over #t)))

(define (lock-piece!)
  (place-piece!)
  (let ((cleared (clear-lines!)))
    (add-score! cleared))
  (set! soft-drop #f)
  (set! soft-drop-left 0)
  (spawn-piece!))

(define (try-move-down!)
  (if (can-place? cur-type cur-rot cur-x (+ cur-y 1))
    (set! cur-y (+ cur-y 1))
    (lock-piece!)))

(define (hard-drop!)
  (let loop ()
    (when (can-place? cur-type cur-rot cur-x (+ cur-y 1))
      (set! cur-y (+ cur-y 1))
      (loop)))
  (lock-piece!))

(define (try-rotate!)
  (let ((new-rot (modulo (+ cur-rot 1) 4)))
    (when (can-place? cur-type new-rot cur-x cur-y)
      (set! cur-rot new-rot))))

(define (try-rotate-ccw!)
  (let ((new-rot (modulo (- cur-rot 1) 4)))
    (when (can-place? cur-type new-rot cur-x cur-y)
      (set! cur-rot new-rot))))

; ===== GAME RESET =====

(define (reset-game!)
  (set! board (make-empty-board))
  (set! score 0)
  (set! level 1)
  (set! lines 0)
  (set! game-over #f)
  (set! paused #f)
  (set! tick-count 0)
  (set! soft-drop #f)
  (set! soft-drop-left 0)
  (set! cur-type (rand-int! 7))
  (set! next-type (rand-int! 7))
  (set! cur-x 3)
  (set! cur-y 0)
  (set! cur-rot 0))

; Initialize first game
(reset-game!)

; ===== GAME TICK =====

(define (effective-gravity)
  (if soft-drop 1 (gravity-ticks)))

(define (tick!)
  (when (and (not game-over) (not paused))
    ; Age soft-drop: cancel it after SOFT-DROP-TICKS ticks without a new press
    (when soft-drop
      (set! soft-drop-left (- soft-drop-left 1))
      (when (<= soft-drop-left 0)
        (set! soft-drop #f)))
    ; Apply gravity
    (set! tick-count (+ tick-count 1))
    (when (>= tick-count (effective-gravity))
      (set! tick-count 0)
      (try-move-down!)))
  (refresh!))

; ===== INPUT HANDLING =====

(on-key-down
  (lambda (key)
    (cond
      ((string=? key "dpad-left")
       (when (and (not game-over) (not paused))
         (when (can-place? cur-type cur-rot (- cur-x 1) cur-y)
           (set! cur-x (- cur-x 1))
           (refresh!))))

      ((string=? key "dpad-right")
       (when (and (not game-over) (not paused))
         (when (can-place? cur-type cur-rot (+ cur-x 1) cur-y)
           (set! cur-x (+ cur-x 1))
           (refresh!))))

      ; Soft drop: activates speed boost, falls one step immediately for responsiveness
      ((string=? key "dpad-down")
       (when (and (not game-over) (not paused))
         (set! soft-drop #t)
         (set! soft-drop-left SOFT-DROP-TICKS)
         (try-move-down!)
         (set! tick-count 0)
         (refresh!)))

      ; Hard drop: instantly slam piece to the bottom
      ((string=? key "dpad-up")
       (when (and (not game-over) (not paused))
         (hard-drop!)
         (refresh!)))

      ; A: rotate clockwise
      ((string=? key "a")
       (when (and (not game-over) (not paused))
         (try-rotate!)
         (refresh!)))

      ; B: rotate counter-clockwise
      ((string=? key "b")
       (when (and (not game-over) (not paused))
         (try-rotate-ccw!)
         (refresh!)))

      ((string=? key "start")
       (if game-over
         (begin (reset-game!) (refresh!))
         (begin
           (set! paused (not paused))
           (refresh!)))))))

; ===== RENDERING HELPERS =====

(define (make-cell-rect col row color)
  (rect
    #:x      (+ (* col CELL) BORDER)
    #:y      (+ (* row CELL) BORDER)
    #:width  CELL-INNER
    #:height CELL-INNER
    #:color  color))

; Inner helper for board-rects: collects rects for one row
; Defined as a top-level function to avoid nested named-let issues in KleinLisp
(define (row-rects cells c r acc)
  (if (null? cells)
    acc
    (row-rects (cdr cells) (+ c 1) r
      (if (car cells)
        (cons (make-cell-rect c r (car cells)) acc)
        acc))))

; All occupied board cells as a list of rect elements
(define (board-rects)
  (let loop ((rows board) (r 0) (acc '()))
    (if (null? rows)
      acc
      (loop (cdr rows) (+ r 1)
            (row-rects (car rows) 0 r acc)))))

; Current active piece as rect elements
(define (piece-rects)
  (map
    (lambda (cell)
      (make-cell-rect (+ cur-x (car cell)) (+ cur-y (cadr cell))
                      (piece-color cur-type)))
    (piece-shape cur-type cur-rot)))

; Ghost piece (dark shadow showing landing position)
(define (find-ghost-y)
  (let loop ((y cur-y))
    (if (can-place? cur-type cur-rot cur-x (+ y 1))
      (loop (+ y 1))
      y)))

(define (ghost-rects)
  (let ((gy (find-ghost-y)))
    (if (= gy cur-y)
      '()
      (map
        (lambda (cell)
          (rect
            #:x      (+ (* (+ cur-x (car cell)) CELL) BORDER)
            #:y      (+ (* (+ gy  (cadr cell)) CELL) BORDER)
            #:width  CELL-INNER
            #:height CELL-INNER
            #:color  "#2A2A2A"))
        (piece-shape cur-type cur-rot)))))

; Next piece preview (4x4 canvas)
(define (next-preview-rects)
  (map
    (lambda (cell)
      (rect
        #:x      (+ (* (car cell) CELL) BORDER)
        #:y      (+ (* (cadr cell) CELL) BORDER)
        #:width  CELL-INNER
        #:height CELL-INNER
        #:color  (piece-color next-type)))
    (piece-shape next-type 0)))

; ===== APP =====

(define (app)
  (surface
    #:fill-max-size #t
    #:color "#0D0D0D"
    (row
      #:fill-max-size #t
      #:horizontal-arrangement 'center
      #:vertical-alignment 'center
      #:spacing 16

      ; Main game board
      (column
        #:spacing 0
        (game-canvas
          #:width  (* COLS CELL)
          #:height (* ROWS CELL)
          #:background (if game-over "#1A0000" "#111111")
          #:on-tick       tick!
          #:tick-interval 50
          (board-rects)
          (if (not game-over) (ghost-rects) '())
          (if (not game-over) (piece-rects) '())))

      ; Side panel
      (column
        #:spacing 10
        #:width 140

        (text #:value "TETRIS"
              #:style 'headline-small
              #:color 'white)

        (spacer #:height 4)

        (text #:value "SCORE"
              #:color "#AAAAAA")
        (text #:value (number->string score)
              #:style 'headline-medium
              #:color "#00BCD4")

        (text #:value "LEVEL"
              #:color "#AAAAAA")
        (text #:value (number->string level)
              #:style 'title-large
              #:color 'white)

        (text #:value "LINES"
              #:color "#AAAAAA")
        (text #:value (number->string lines)
              #:style 'title-medium
              #:color 'white)

        (spacer #:height 8)

        (text #:value "NEXT"
              #:color "#AAAAAA")
        (game-canvas
          #:width  (* 4 CELL)
          #:height (* 4 CELL)
          #:background "#111111"
          (next-preview-rects))

        (spacer #:height 12)

        (if game-over
          (column
            #:spacing 8
            (text #:value "GAME OVER"
                  #:style 'headline-small
                  #:color "#EF5350")
            (text #:value (string-append "Score: " (number->string score))
                  #:color "#AAAAAA")
            (spacer #:height 4)
            (text #:value "START"
                  #:color "#66BB6A")
            (text #:value "to restart"
                  #:color "#888888"))
          (if paused
            (text #:value "PAUSED"
                  #:style 'title-medium
                  #:color "#FFD700")
            (column
              #:spacing 4
              (text #:value "A  rotate ↻" #:color "#555555")
              (text #:value "B  rotate ↺" #:color "#555555")
              (text #:value "↓  soft drop" #:color "#555555")
              (text #:value "↑  hard drop" #:color "#555555")
              (text #:value "START pause" #:color "#555555"))))))))

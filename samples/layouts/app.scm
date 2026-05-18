(define (app)
  (scaffold
    #:top-bar (top-app-bar #:title "Layouts" #:style 'center-aligned)
    (lazy-column #:padding 16 #:spacing 20

      ; ─── Column ───────────────────────────────────────────────
      (text #:value "Column" #:style 'title-medium)
      (text #:value "Items stacked vertically with spacing"
            #:style 'body-small #:color "gray")
      (card #:style 'outlined #:fill-max-width #t #:padding 12
        (column #:spacing 8 #:fill-max-width #t
          (card #:style 'filled #:padding 12 #:fill-max-width #t
            (text #:value "First item" #:style 'label-large #:text-align "center"))
          (card #:style 'filled #:padding 12 #:fill-max-width #t
            (text #:value "Second item" #:style 'label-large #:text-align "center"))
          (card #:style 'filled #:padding 12 #:fill-max-width #t
            (text #:value "Third item" #:style 'label-large #:text-align "center"))))

      ; ─── Row ──────────────────────────────────────────────────
      (text #:value "Row" #:style 'title-medium)
      (text #:value "Items arranged horizontally with spacing"
            #:style 'body-small #:color "gray")
      (card #:style 'outlined #:fill-max-width #t #:padding 12
        (row #:spacing 8 #:fill-max-width #t #:horizontal-arrangement 'space-evenly
          (card #:style 'filled #:padding 12
            (text #:value "Left" #:style 'label-large))
          (card #:style 'filled #:padding 12
            (text #:value "Center" #:style 'label-large))
          (card #:style 'filled #:padding 12
            (text #:value "Right" #:style 'label-large))))

      ; ─── Weight ───────────────────────────────────────────────
      (text #:value "Weight" #:style 'title-medium)
      (text #:value "Proportional sizing: 1x — 2x — 1x"
            #:style 'body-small #:color "gray")
      (card #:style 'outlined #:fill-max-width #t #:padding 12
        (row #:fill-max-width #t #:spacing 8
          (box #:modifier (list (list 'weight 1)) #:fill-max-width #t
            (card #:style 'filled #:padding 12 #:fill-max-width #t
              (text #:value "1x" #:style 'label-large #:text-align "center")))
          (box #:modifier (list (list 'weight 2)) #:fill-max-width #t
            (card #:style 'elevated #:padding 12 #:fill-max-width #t
              (text #:value "2x" #:style 'label-large #:text-align "center")))
          (box #:modifier (list (list 'weight 1)) #:fill-max-width #t
            (card #:style 'filled #:padding 12 #:fill-max-width #t
              (text #:value "1x" #:style 'label-large #:text-align "center")))))

      ; ─── Box alignment ────────────────────────────────────────
      (text #:value "Box Alignment" #:style 'title-medium)
      (text #:value "Content pinned at different positions"
            #:style 'body-small #:color "gray")
      (row #:spacing 8 #:fill-max-width #t
        (box #:modifier (list (list 'weight 1)) #:fill-max-width #t
          (card #:style 'outlined #:fill-max-width #t #:height 88 #:padding 4
            (box #:fill-max-size 1 #:content-alignment 'top-start
              (text #:value "top-start" #:style 'label-small #:color "gray"))))
        (box #:modifier (list (list 'weight 1)) #:fill-max-width #t
          (card #:style 'outlined #:fill-max-width #t #:height 88 #:padding 4
            (box #:fill-max-size 1 #:content-alignment 'center
              (text #:value "center" #:style 'label-small #:color "gray"))))
        (box #:modifier (list (list 'weight 1)) #:fill-max-width #t
          (card #:style 'outlined #:fill-max-width #t #:height 88 #:padding 4
            (box #:fill-max-size 1 #:content-alignment 'bottom-end
              (text #:value "bottom-end" #:style 'label-small #:color "gray")))))

      ; ─── Arrangements ─────────────────────────────────────────
      (text #:value "Arrangements" #:style 'title-medium)
      (text #:value "Horizontal distribution strategies"
            #:style 'body-small #:color "gray")
      (card #:style 'outlined #:fill-max-width #t #:padding 16
        (column #:spacing 16 #:fill-max-width #t
          (column #:spacing 6 #:fill-max-width #t
            (text #:value "space-between" #:style 'label-small #:color "gray")
            (row #:fill-max-width #t #:horizontal-arrangement 'space-between
              (card #:style 'filled #:padding 10 (text #:value "A" #:style 'label-large))
              (card #:style 'filled #:padding 10 (text #:value "B" #:style 'label-large))
              (card #:style 'filled #:padding 10 (text #:value "C" #:style 'label-large))))
          (divider)
          (column #:spacing 6 #:fill-max-width #t
            (text #:value "space-evenly" #:style 'label-small #:color "gray")
            (row #:fill-max-width #t #:horizontal-arrangement 'space-evenly
              (card #:style 'filled #:padding 10 (text #:value "A" #:style 'label-large))
              (card #:style 'filled #:padding 10 (text #:value "B" #:style 'label-large))
              (card #:style 'filled #:padding 10 (text #:value "C" #:style 'label-large))))
          (divider)
          (column #:spacing 6 #:fill-max-width #t
            (text #:value "space-around" #:style 'label-small #:color "gray")
            (row #:fill-max-width #t #:horizontal-arrangement 'space-around
              (card #:style 'filled #:padding 10 (text #:value "A" #:style 'label-large))
              (card #:style 'filled #:padding 10 (text #:value "B" #:style 'label-large))
              (card #:style 'filled #:padding 10 (text #:value "C" #:style 'label-large))))))

      ; ─── Nested ───────────────────────────────────────────────
      (text #:value "Nested" #:style 'title-medium)
      (text #:value "Rows inside columns with mixed weights"
            #:style 'body-small #:color "gray")
      (card #:style 'outlined #:fill-max-width #t #:padding 12
        (column #:spacing 8 #:fill-max-width #t
          (row #:spacing 8 #:fill-max-width #t
            (box #:modifier (list (list 'weight 1)) #:fill-max-width #t
              (card #:style 'filled #:padding 10 #:fill-max-width #t
                (text #:value "A" #:style 'label-large #:text-align "center")))
            (box #:modifier (list (list 'weight 1)) #:fill-max-width #t
              (card #:style 'filled #:padding 10 #:fill-max-width #t
                (text #:value "B" #:style 'label-large #:text-align "center")))
            (box #:modifier (list (list 'weight 1)) #:fill-max-width #t
              (card #:style 'filled #:padding 10 #:fill-max-width #t
                (text #:value "C" #:style 'label-large #:text-align "center"))))
          (row #:spacing 8 #:fill-max-width #t
            (box #:modifier (list (list 'weight 2)) #:fill-max-width #t
              (card #:style 'elevated #:padding 10 #:fill-max-width #t
                (text #:value "Wide (2x)" #:style 'label-large #:text-align "center")))
            (box #:modifier (list (list 'weight 1)) #:fill-max-width #t
              (card #:style 'elevated #:padding 10 #:fill-max-width #t
                (text #:value "1x" #:style 'label-large #:text-align "center")))))))))

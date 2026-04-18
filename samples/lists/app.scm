(define (app)
  (column #:fill-max-size 1 #:padding 16 #:spacing 16
    (text #:value "Lazy Lists Demo" #:style 'headline-medium)

    (text #:value "Vertical List (LazyColumn)" #:style 'title-medium)
    (surface #:shape 'rounded #:elevation 2
      (lazy-column #:spacing 8 #:padding 8 #:height 200 #:fill-max-width 1
        (list-item #:key "item-1"
          (surface #:color 'gray #:shape 'rounded #:padding 12 #:fill-max-width 1
            (text #:value "Item 1")))
        (list-item #:key "item-2"
          (surface #:color 'gray #:shape 'rounded #:padding 12 #:fill-max-width 1
            (text #:value "Item 2")))
        (list-item #:key "item-3"
          (surface #:color 'gray #:shape 'rounded #:padding 12 #:fill-max-width 1
            (text #:value "Item 3")))
        (list-item #:key "item-4"
          (surface #:color 'gray #:shape 'rounded #:padding 12 #:fill-max-width 1
            (text #:value "Item 4")))
        (list-item #:key "item-5"
          (surface #:color 'gray #:shape 'rounded #:padding 12 #:fill-max-width 1
            (text #:value "Item 5")))
        (list-item #:key "item-6"
          (surface #:color 'gray #:shape 'rounded #:padding 12 #:fill-max-width 1
            (text #:value "Item 6")))
        (list-item #:key "item-7"
          (surface #:color 'gray #:shape 'rounded #:padding 12 #:fill-max-width 1
            (text #:value "Item 7")))
        (list-item #:key "item-8"
          (surface #:color 'gray #:shape 'rounded #:padding 12 #:fill-max-width 1
            (text #:value "Item 8")))))

    (spacer #:height 16)

    (text #:value "Horizontal List (LazyRow)" #:style 'title-medium)
    (lazy-row #:spacing 12 #:padding 8
      (list-item #:key "card-a"
        (surface #:color 'blue #:shape 'rounded #:width 120 #:height 80
          (box #:fill-max-size 1 #:content-alignment 'center
            (text #:value "Card A" #:color 'white))))
      (list-item #:key "card-b"
        (surface #:color 'green #:shape 'rounded #:width 120 #:height 80
          (box #:fill-max-size 1 #:content-alignment 'center
            (text #:value "Card B" #:color 'white))))
      (list-item #:key "card-c"
        (surface #:color 'red #:shape 'rounded #:width 120 #:height 80
          (box #:fill-max-size 1 #:content-alignment 'center
            (text #:value "Card C" #:color 'white))))
      (list-item #:key "card-d"
        (surface #:color 'magenta #:shape 'rounded #:width 120 #:height 80
          (box #:fill-max-size 1 #:content-alignment 'center
            (text #:value "Card D" #:color 'white))))
      (list-item #:key "card-e"
        (surface #:color 'cyan #:shape 'rounded #:width 120 #:height 80
          (box #:fill-max-size 1 #:content-alignment 'center
            (text #:value "Card E")))))))

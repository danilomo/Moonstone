(define (app)
  (column #:spacing 16 #:padding 16
    (text #:value "Testing New Components" #:style 'headline-medium)

    (card #:padding 16
      (column #:spacing 8
        (text #:value "Card Component" #:style 'title-medium)
        (text #:value "This is a simple filled card.")))

    (divider)

    (text #:value "Slider test:")
    (slider #:value (state 50) #:min 0 #:max 100 #:fill-max-width #t)

    (divider)

    (text #:value "Progress indicators:")
    (row #:spacing 16
      (progress-indicator #:style 'circular)
      (progress-indicator #:style 'circular #:value 0.5))

    (progress-indicator #:style 'linear #:fill-max-width #t)

    (divider)

    (text #:value "Chips:")
    (row #:spacing 8
      (chip #:style 'assist #:label "Assist")
      (chip #:style 'suggestion #:label "Suggestion")
      (chip #:style 'filter #:label "Filter" #:selected (state 0)))

    (divider)

    (text #:value "FABs:")
    (row #:spacing 16
      (fab (icon #:name "add"))
      (fab #:style 'small (icon #:name "edit"))
      (fab #:style 'extended #:label "Create" (icon #:name "add")))

    (divider)

    (text #:value "Badges:")
    (row #:spacing 16
      (badge (icon #:name "notifications" #:size 32))
      (badge #:count 5 (icon #:name "mail" #:size 32)))

    (divider)

    (text #:value "Image placeholders:")
    (row #:spacing 16
      (image #:placeholder "Rect" #:size 60 #:shape 'rectangle)
      (image #:placeholder "Circle" #:size 60 #:shape 'circle))))

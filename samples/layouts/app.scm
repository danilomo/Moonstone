(define (app)
  (surface
   #:fill-max-size 1
   #:color 'white
   (column
    #:fill-max-size 1
    #:padding 16
    #:spacing 16
    #:horizontal-alignment 'center

    (text #:value "Moonstone - Layout Demo"
          #:style 'headline-medium
          #:color 'blue)

    (spacer #:height 8)

    (text #:value "Column Layout"
          #:style 'title-medium)

    (surface
     #:color 'gray
     #:shape 'rounded
     #:padding 8
     (column
      #:spacing 8
      #:fill-max-width 1
      (text #:value "Item 1")
      (text #:value "Item 2")
      (text #:value "Item 3")))

    (spacer #:height 16)

    (text #:value "Row Layout"
          #:style 'title-medium)

    (surface
     #:color 'gray
     #:shape 'rounded
     #:padding 8
     (row
      #:spacing 16
      #:horizontal-arrangement 'space-evenly
      #:fill-max-width 1
      (text #:value "Left" #:color 'red)
      (text #:value "Center" #:color 'green)
      (text #:value "Right" #:color 'blue)))

    (spacer #:height 16)

    (text #:value "Nested Layout"
          #:style 'title-medium)

    (surface
     #:color 'gray
     #:shape 'rounded
     #:padding 12
     (column
      #:spacing 8
      (row
       #:spacing 8
       #:vertical-alignment 'center
       (text #:value "Row 1:" #:font-weight 'bold)
       (text #:value "A")
       (text #:value "B")
       (text #:value "C"))
      (row
       #:spacing 8
       #:vertical-alignment 'center
       (text #:value "Row 2:" #:font-weight 'bold)
       (text #:value "X")
       (text #:value "Y")
       (text #:value "Z")))))))

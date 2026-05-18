(define (list-row key icon-name title subtitle)
  (list-item #:key key
    (column #:fill-max-width #t
      (row #:fill-max-width #t #:spacing 16 #:vertical-alignment 'center #:padding 16
        (icon #:name icon-name #:size 24 #:tint "gray")
        (column #:spacing 2
          (text #:value title #:style 'body-large)
          (text #:value subtitle #:style 'body-small #:color "gray")))
      (divider))))

(define (h-card key icon-name label tint)
  (list-item #:key key
    (card #:style 'outlined #:padding 0 #:width 140 #:height 104
      (box #:fill-max-size 1 #:content-alignment 'center
        (column #:spacing 10 #:horizontal-alignment 'center
          (icon #:name icon-name #:size 36 #:tint tint)
          (text #:value label #:style 'label-large))))))

(define (app)
  (scaffold
    #:top-bar (top-app-bar #:title "Lazy Lists" #:style 'center-aligned)
    (column #:fill-max-size 1 #:padding 16 #:spacing 8

      (text #:value "Vertical List (LazyColumn)" #:style 'title-medium)
      (text #:value "Scroll down to see all 8 items"
            #:style 'body-small #:color "gray")

      (card #:style 'outlined #:fill-max-width #t
            #:modifier (list (list 'weight 1))
        (lazy-column #:fill-max-size 1
          (list-row "r1" "email"         "Messages"     "3 unread conversations")
          (list-row "r2" "notifications" "Notifications" "You are all caught up")
          (list-row "r3" "star"          "Favorites"    "8 saved items")
          (list-row "r4" "history"       "Recent"       "Viewed 2 hours ago")
          (list-row "r5" "person"        "Profile"      "Update your information")
          (list-row "r6" "dark-mode"     "Appearance"   "Dark theme enabled")
          (list-row "r7" "info"          "About"        "Version 1.0.0")
          (list-row "r8" "help"          "Help & Support" "FAQs and contact us")))

      (text #:value "Horizontal List (LazyRow)" #:style 'title-medium)

      (lazy-row #:spacing 12 #:padding 4
        (h-card "c1" "email"         "Messages"  "#1565C0")
        (h-card "c2" "notifications" "Alerts"    "#E65100")
        (h-card "c3" "star"          "Favorites" "#F9A825")
        (h-card "c4" "person"        "Profile"   "#2E7D32")
        (h-card "c5" "settings"      "Settings"  "#6A1B9A")
        (h-card "c6" "info"          "About"     "#00695C")))))

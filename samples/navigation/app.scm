(define current-tab (state 0))

(define (home-content)
  (column #:fill-max-size 1 #:padding 24 #:spacing 12
    (text #:value "Home" #:style 'headline-large)
    (text #:value "Welcome to the navigation demo!" #:style 'body-large)
    (text #:value "Use the bottom navigation to switch between tabs.")
    (spacer #:height 16)
    (surface #:color 'blue #:shape 'rounded #:padding 16
      (text #:value "This is the home view content" #:color 'white))))

(define (search-content)
  (column #:fill-max-size 1 #:padding 24 #:spacing 12
    (text #:value "Search" #:style 'headline-large)
    (text #:value "Search functionality would go here." #:style 'body-large)
    (spacer #:height 16)
    (row #:spacing 16 #:vertical-alignment 'center
      (icon #:name "search" #:size 48 #:tint "gray")
      (text #:value "Type to search..." #:color "gray"))))

(define (profile-content)
  (column #:fill-max-size 1 #:padding 24 #:spacing 12
    (text #:value "Profile" #:style 'headline-large)
    (spacer #:height 8)
    (row #:spacing 16 #:vertical-alignment 'center
      (icon #:name "account-circle" #:size 64 #:tint "blue")
      (column #:spacing 4
        (text #:value "John Doe" #:style 'title-medium)
        (text #:value "john@example.com" #:color "gray")))
    (spacer #:height 16)
    (surface #:color 'gray #:shape 'rounded #:padding 12
      (text #:value "Profile settings and preferences would appear here."))))

(define (app)
  (scaffold
    #:top-bar (top-app-bar #:title "Navigation Demo" #:style 'center-aligned)
    #:bottom-bar (bottom-navigation #:selected current-tab
      (nav-item #:icon "home" #:label "Home" #:value 0
        #:on-select (lambda () (state-set! current-tab 0)))
      (nav-item #:icon "search" #:label "Search" #:value 1
        #:on-select (lambda () (state-set! current-tab 1)))
      (nav-item #:icon "person" #:label "Profile" #:value 2
        #:on-select (lambda () (state-set! current-tab 2))))
    (switch-view #:selected current-tab
      (view #:value 0 (home-content))
      (view #:value 1 (search-content))
      (view #:value 2 (profile-content)))))

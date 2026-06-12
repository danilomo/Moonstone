; Habit Tracker — persistent counter, no date, no history.
;
; One screen, auto-saves on every change.
; Open the app, update your habits, close — that's it.

(define *app-title* "Habit Tracker")

(define *metrics*
  (list
    (p-map #:label "Water (glasses)"
           #:col-name #:water
           #:type #:int
           #:input-type 'spin
           #:step 1
           #:min 0
           #:max 20)
    (p-map #:label "Exercise"
           #:col-name #:exercise
           #:type #:int
           #:input-type 'toggle)
    (p-map #:label "Reading"
           #:col-name #:reading
           #:type #:int
           #:input-type 'toggle)
    (p-map #:label "Meditation"
           #:col-name #:meditation
           #:type #:int
           #:input-type 'toggle)
    (p-map #:label "Mood"
           #:col-name #:mood
           #:type #:string
           #:input-type 'choice
           #:options '("Great" "Good" "Neutral" "Tired" "Bad"))
    (p-map #:label "Day rating"
           #:col-name #:day-rating
           #:type #:int
           #:input-type 'rating
           #:max 5)))

(load-relative "../counter-base.scm")

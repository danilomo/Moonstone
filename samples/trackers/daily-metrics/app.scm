; Daily Metrics Tracker
;
; Tracks body composition and wellness daily.
; Mix of numeric fields, a slider for wellness, and a mood choice.
;
; Input types demonstrated:
;   #:input-type 'number   — free numeric text field (default)
;   #:input-type 'spin     — +/- buttons with configurable step
;   #:input-type 'slider   — continuous range slider
;   #:input-type 'choice   — radio-button group
;   #:input-type 'rating   — star rating
;   #:input-type 'toggle   — on/off switch

(define *app-title* "Daily Metrics")
(define *default-from-last* #f)

(define *metrics*
  (list
    (p-map #:label "Weight (kg)"
           #:col-name #:weight
           #:type #:real
           #:input-type 'spin
           #:step 0.1
           #:min 0
           #:required #t)
    (p-map #:label "Body fat (%)"
           #:col-name #:body-fat
           #:type #:real
           #:input-type 'number)
    (p-map #:label "Muscle mass (kg)"
           #:col-name #:muscle-mass
           #:type #:real
           #:input-type 'number)
    (p-map #:label "Sleep (hours)"
           #:col-name #:sleep-hours
           #:type #:real
           #:input-type 'slider
           #:min 0
           #:max 12)
    (p-map #:label "Water intake (glasses)"
           #:col-name #:water-glasses
           #:type #:int
           #:input-type 'spin
           #:step 1
           #:min 0
           #:max 20)
    (p-map #:label "Workout done?"
           #:col-name #:workout-done
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

(load-relative "../metrics-base.scm")

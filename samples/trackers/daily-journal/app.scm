; Daily Journal — multi-group daily check-in.
;
; Metrics are organized into three pages, navigated with Previous / Next.
; Every change auto-saves immediately — no Save button needed. Combined with
; the page-resume behavior in metrics-grouped-base.scm, closing the app
; mid-entry and reopening it picks up right where you left off, with all
; values already filled in.
;
; Groups:
;   Morning   — sleep hours, sleep quality, energy level, mood
;   Body      — weight, water intake, workout done?
;   Evening   — focus level, stress level, day rating

(define *app-title* "Daily Journal")
(define *default-from-last* #f)
(define *auto-save* #t)

(define *groups*
  (list
    (p-map
      #:title "Morning"
      #:metrics (list
        (p-map #:label "Sleep (hours)"
               #:col-name #:sleep-hours
               #:type #:real
               #:input-type 'slider
               #:min 0
               #:max 12)
        (p-map #:label "Sleep quality"
               #:col-name #:sleep-quality
               #:type #:int
               #:input-type 'rating
               #:max 5)
        (p-map #:label "Morning energy"
               #:col-name #:morning-energy
               #:type #:int
               #:input-type 'slider
               #:min 1
               #:max 10)
        (p-map #:label "Mood"
               #:col-name #:mood
               #:type #:string
               #:input-type 'choice
               #:options '("Great" "Good" "Neutral" "Tired" "Awful"))))

    (p-map
      #:title "Body"
      #:metrics (list
        (p-map #:label "Weight (kg)"
               #:col-name #:weight
               #:type #:real
               #:input-type 'spin
               #:step 0.1
               #:min 0
               #:required #t)
        (p-map #:label "Water (glasses)"
               #:col-name #:water-glasses
               #:type #:int
               #:input-type 'spin
               #:step 1
               #:min 0
               #:max 20)
        (p-map #:label "Workout done?"
               #:col-name #:workout-done
               #:type #:int
               #:input-type 'toggle)))

    (p-map
      #:title "Evening"
      #:metrics (list
        (p-map #:label "Focus level"
               #:col-name #:focus-level
               #:type #:int
               #:input-type 'slider
               #:min 1
               #:max 10)
        (p-map #:label "Stress level"
               #:col-name #:stress-level
               #:type #:int
               #:input-type 'rating
               #:max 5)
        (p-map #:label "Day rating"
               #:col-name #:day-rating
               #:type #:int
               #:input-type 'rating
               #:max 5
               #:required #t)))))

(load-relative "../metrics-grouped-base.scm")

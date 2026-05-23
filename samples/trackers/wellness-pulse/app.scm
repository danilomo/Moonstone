; Wellness Pulse — auto-saving daily check-in.
;
; Tracks your current state throughout the day.
; Every slider drag, spin tap, star click, toggle, or choice selection
; is saved immediately — no Save button needed.
;
; *auto-save* is enabled. The form always stays open; the record is
; created on the first change and updated on every subsequent one.

(define *app-title* "Wellness Pulse")
(define *default-from-last* #f)
(define *auto-save* #t)

(define *metrics*
  (list
    (p-map #:label "Energy level"
           #:col-name #:energy
           #:type #:int
           #:input-type 'slider
           #:min 1
           #:max 10)
    (p-map #:label "Focus level"
           #:col-name #:focus
           #:type #:int
           #:input-type 'slider
           #:min 1
           #:max 10)
    (p-map #:label "Mood"
           #:col-name #:mood
           #:type #:string
           #:input-type 'choice
           #:options '("Great" "Good" "Neutral" "Tired" "Bad"))
    (p-map #:label "Stress"
           #:col-name #:stress
           #:type #:int
           #:input-type 'rating
           #:max 5)
    (p-map #:label "Water (glasses)"
           #:col-name #:water
           #:type #:int
           #:input-type 'spin
           #:step 1
           #:min 0
           #:max 20)
    (p-map #:label "Headache"
           #:col-name #:headache
           #:type #:int
           #:input-type 'toggle)
    (p-map #:label "Overall day rating"
           #:col-name #:day-rating
           #:type #:int
           #:input-type 'rating
           #:max 5)))

(load-relative "../metrics-base.scm")

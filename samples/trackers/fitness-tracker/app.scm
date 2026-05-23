; Fitness / Weight Training Tracker
;
; Tracks sets for the major compound lifts.
; *default-from-last* = #t pre-fills today's form with your last workout values.
; When you modify a field from the loaded value, a orange ● marker appears
; next to the label so you can see at a glance what you changed.
;
; Workflow:
;   1. Open the app — last session's weights and reps are pre-loaded.
;   2. Increase (or decrease) any value as needed.
;   3. Save. The ● marker shows exactly what changed from last time.

(define *app-title* "Fitness Tracker")
(define *default-from-last* #t)
(define *auto-save* #f)

(define *metrics*
  (list
    ; ── Bench Press ────────────────────────────────────────────────
    (p-map #:label "Bench Press — Weight (kg)"
           #:col-name #:bench-weight
           #:type #:real
           #:input-type 'spin
           #:step 2.5
           #:min 0
           #:required #t)
    (p-map #:label "Bench Press — Reps"
           #:col-name #:bench-reps
           #:type #:int
           #:input-type 'spin
           #:step 1
           #:min 0
           #:required #t)

    ; ── Squat ──────────────────────────────────────────────────────
    (p-map #:label "Squat — Weight (kg)"
           #:col-name #:squat-weight
           #:type #:real
           #:input-type 'spin
           #:step 2.5
           #:min 0)
    (p-map #:label "Squat — Reps"
           #:col-name #:squat-reps
           #:type #:int
           #:input-type 'spin
           #:step 1
           #:min 0)

    ; ── Deadlift ───────────────────────────────────────────────────
    (p-map #:label "Deadlift — Weight (kg)"
           #:col-name #:deadlift-weight
           #:type #:real
           #:input-type 'spin
           #:step 2.5
           #:min 0)
    (p-map #:label "Deadlift — Reps"
           #:col-name #:deadlift-reps
           #:type #:int
           #:input-type 'spin
           #:step 1
           #:min 0)

    ; ── Overhead Press ─────────────────────────────────────────────
    (p-map #:label "OHP — Weight (kg)"
           #:col-name #:ohp-weight
           #:type #:real
           #:input-type 'spin
           #:step 2.5
           #:min 0)
    (p-map #:label "OHP — Reps"
           #:col-name #:ohp-reps
           #:type #:int
           #:input-type 'spin
           #:step 1
           #:min 0)

    ; ── Session quality ────────────────────────────────────────────
    (p-map #:label "Completed all sets?"
           #:col-name #:completed
           #:type #:int
           #:input-type 'toggle)
    (p-map #:label "Workout type"
           #:col-name #:workout-type
           #:type #:string
           #:input-type 'choice
           #:options '("Full body" "Upper" "Lower" "Push" "Pull" "Legs"))
    (p-map #:label "Session rating"
           #:col-name #:session-rating
           #:type #:int
           #:input-type 'rating
           #:max 5)))

(load-relative "../metrics-base.scm")

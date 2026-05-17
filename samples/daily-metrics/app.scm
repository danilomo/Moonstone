; Daily Metrics Tracker
;
; Configure *metrics* — a list of pmaps, one per field to track.
;
; Required keys per entry:
;   #:label    "Display Label"     — shown in form and history
;   #:col-name #:some-col-name     — keyword naming the DB column (use hyphens)
;   #:type     #:real              — ORM type: #:real #:int #:string #:text etc.
;
; Optional keys:
;   #:required #t                  — makes the field mandatory (default: optional)
;
; To override the app title, add after the load:
;   (set! *app-title* "My Custom Tracker")

(define *metrics*
  (list
    (p-map #:label "Weight (kg)"      #:col-name #:weight      #:type #:real #:required #t)
    (p-map #:label "Body fat (%)"     #:col-name #:body-fat    #:type #:real)
    (p-map #:label "Muscle mass (kg)" #:col-name #:muscle-mass #:type #:real)))

(load-relative "metrics-base.scm")

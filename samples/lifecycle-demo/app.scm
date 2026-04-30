; Today's Note — lifecycle hooks demo
;
; This app persists one short note per day using SQLite.
; Its purpose is to demonstrate the three scaffold lifecycle hooks in isolation:
;   #:on-start  → runs ONCE when the scaffold first appears
;   #:on-resume → runs on EVERY recompose (use for refreshing data)
;   #:on-close  → runs when the window closes (use for teardown)

; === Schema ===

(db-table notes
  (id #:serial)
  (date #:string #:not-null #:unique)
  (note #:text #:not-null #:default ""))

; === Query ===

(db-query-single find-note-by-date
  #:from notes
  #:where (= date ?date)
  #:params (date))

; === State ===

(define saved-note (state ""))   ; last note persisted to DB
(define input-text (state ""))   ; text the user is currently typing
(define resume-count (state 0))  ; counts on-resume calls (teaching aid)
(define today (current-date-string))

; === Lifecycle: on-start ===
; Guaranteed to run exactly ONCE per scaffold lifetime (LaunchedEffect(Unit)).
; Ideal for: DB table creation, loading initial data, one-time setup.

(define (init!)
  (println "App starting...")
  (find-note-by-date
    (lambda (row error)
      (if (not error)
          (if row
              (begin
                (state-set! saved-note (p-map-get row #:note ""))
                (state-set! input-text (p-map-get row #:note "")))
              (println "No note for today yet"))))
    #:date today))

; === Lifecycle: on-resume ===
; Runs on every recomposition — even while on-start has already fired.
; This means external DB changes (e.g. another process) will be picked up.
; Keep this lightweight; avoid side effects that should only happen once.

(define (refresh!)
  (state-update! resume-count (lambda (n) (+ n 1)))
  (println (string-append "on-resume fired (call #" (number->string (state-ref resume-count)) ")"))
  (find-note-by-date
    (lambda (row error)
      (if (and (not error) row)
          (state-set! saved-note (p-map-get row #:note ""))))
    #:date today))

; === Lifecycle: on-close ===
; Called when the scaffold is disposed — window close or composable removal.
; Use for: flushing buffers, closing connections, logging session end.

(define (teardown!)
  (println "App closing. Goodbye!"))

; === Save ===
; Upsert: update if today's row exists, insert otherwise.

(define (save-note!)
  (let ((text (state-ref input-text)))
    (find-note-by-date
      (lambda (row error)
        (if (not error)
            (if row
                (db-update notes
                  #:set (p-map #:note text)
                  #:where (= date ?date)
                  #:date today
                  (lambda (count err)
                    (if (not err)
                        (state-set! saved-note text))))
                (db-insert notes
                  #:values (p-map #:date today #:note text)
                  (lambda (id err)
                    (if (not err)
                        (state-set! saved-note text)))))))
      #:date today)))

; === UI ===

(define (app)
  (scaffold
    #:on-start init!       ; once — initialize DB and load today's note
    #:on-resume refresh!   ; every recompose — keep note in sync with DB
    #:on-close teardown!   ; cleanup — log session end
    #:top-bar (top-app-bar #:title "Today's Note")
    (column #:padding 16 #:spacing 12
      (text #:value (string-append "Date: " today))
      (text #:value (string-append "Saved: " (state-ref saved-note)))
      (outlined-text-field
        #:label "Your note for today"
        #:value input-text
        #:on-change (lambda (v) (state-set! input-text v))
        #:fill-max-width #t)
      (button #:on-click save-note!
        (text #:value "Save")))))

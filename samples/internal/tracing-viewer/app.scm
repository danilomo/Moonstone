(define all-spans
  (list
    (p-map "id" "span1" "name" "agent.research_turn" "duration" 7660 "status" "ok" "depth" 0 "parent_id" '())
    (p-map "id" "span2" "name" "agent.plan" "duration" 51 "status" "ok" "depth" 1 "parent_id" "span1")
    (p-map "id" "span3" "name" "llm.chat" "duration" 7572 "status" "ok" "depth" 1 "parent_id" "span1")
    (p-map "id" "span4" "name" "llm.completion" "duration" 2375 "status" "ok" "depth" 2 "parent_id" "span3")
    (p-map "id" "span5" "name" "llm.tool" "duration" 101 "status" "ok" "depth" 2 "parent_id" "span3")
    (p-map "id" "span6" "name" "llm.completion" "duration" 1301 "status" "ok" "depth" 2 "parent_id" "span3")
    (p-map "id" "span7" "name" "llm.tool" "duration" 51 "status" "ok" "depth" 2 "parent_id" "span3")
    (p-map "id" "span8" "name" "llm.completion" "duration" 3738 "status" "ok" "depth" 2 "parent_id" "span3")
    (p-map "id" "span9" "name" "agent.synthesize" "duration" 31 "status" "ok" "depth" 1 "parent_id" "span1")))

(define spans-map
  (fold-left
    (lambda (acc span)
      (let ((id (p-map-get span "id")))
        (p-map-assoc acc id span)))
    (p-map)
    all-spans))

(define (get-span id) (p-map-get spans-map id))

(define selected-span (state (car all-spans)))
(define expanded-set (state (p-set-conj (p-set-conj (p-set) "span1") "span3")))

(define (format-dur ms)
  (if (null? ms)
      "-"
      (if (> ms 1000)
          (string-append (number->string (/ ms 1000)) "s")
          (string-append (number->string ms) "ms"))))

(define (status-color s)
  (cond ((null? s) "#757575")
        ((string=? s "ok") "#4CAF50")
        ((string=? s "error") "#F44336")
        (else "#757575")))

(define (is-expanded? span-id)
  (p-set-contains? (state-ref expanded-set) span-id))

(define (toggle-expand span-id)
  (if (p-set-contains? (state-ref expanded-set) span-id)
      (state-set! expanded-set (p-set-disj (state-ref expanded-set) span-id))
      (state-set! expanded-set (p-set-conj (state-ref expanded-set) span-id))))

(define (has-children? span-id)
  (any (lambda (span)
         (let ((parent (p-map-get span "parent_id" "")))
           (if (string? parent)
               (string=? parent span-id)
               #f)))
       all-spans))

(define (is-visible? span)
  (let ((parent-id (p-map-get span "parent_id")))
    (if (null? parent-id)
        #t
        (if (not (string? parent-id))
            #t
            (if (string=? parent-id "")
                #t
                (if (p-set-contains? (state-ref expanded-set) parent-id)
                    (let ((parent-span (get-span parent-id)))
                      (if (p-map? parent-span)
                          (is-visible? parent-span)
                          #t))
                    #f))))))

(define (span-label name duration status)
  (row #:spacing 12 #:vertical-alignment 'center
       (text #:value name #:style 'body-medium)
       (text #:value (format-dur duration) #:style 'body-small #:color "#666666")
       (surface #:padding-horizontal 8 #:padding-vertical 2 #:shape 12
                #:color (status-color status)
                (text #:value status #:style 'label-small #:color "#FFFFFF"))))

(define (render-span span)
  (let ((id (p-map-get span "id"))
        (name (p-map-get span "name"))
        (duration (p-map-get span "duration"))
        (status (p-map-get span "status"))
        (depth (p-map-get span "depth")))
    (let ((indent (* depth 20))
          (has-kids (has-children? id))
          (expanded (is-expanded? id))
          (visible (is-visible? span)))
      (if visible
          (row
           #:fill-max-width #t
           #:vertical-alignment 'center
           #:padding-horizontal indent
           (if has-kids
               (button #:on-click (lambda () (toggle-expand id)) #:style 'text
                       (text #:value (if expanded "-" "+") #:style 'body-medium))
               (spacer #:width 40))
           (button #:on-click (lambda () (state-set! selected-span span)) #:style 'text
                   (span-label name duration status)))
          (spacer #:height 0)))))

(define (details-panel)
  (let ((span (state-ref selected-span)))
    (let ((id (p-map-get span "id" "-"))
          (name (p-map-get span "name" "-"))
          (duration (p-map-get span "duration"))
          (status (p-map-get span "status" "?"))
          (parent-id (p-map-get span "parent_id")))
      (surface #:fill-max-width #t #:shape 8 #:color "#E8F5E9" #:padding 16
               (column #:fill-max-width #t #:spacing 8
                       (text #:value "Selected Span" #:style 'title-medium)
                       (row #:spacing 8
                            (text #:value "ID:" #:style 'body-medium #:color "#666666")
                            (text #:value id #:style 'body-small))
                       (row #:spacing 8
                            (text #:value "Name:" #:style 'body-medium #:color "#666666")
                            (text #:value name #:style 'body-medium))
                       (row #:spacing 8
                            (text #:value "Duration:" #:style 'body-medium #:color "#666666")
                            (text #:value (format-dur duration) #:style 'body-medium))
                       (row #:spacing 8
                            (text #:value "Status:" #:style 'body-medium #:color "#666666")
                            (surface #:padding-horizontal 8 #:padding-vertical 2 #:shape 12
                                     #:color (status-color status)
                                     (text #:value status #:style 'label-small #:color "#FFFFFF")))
                       (row #:spacing 8
                            (text #:value "Parent:" #:style 'body-medium #:color "#666666")
                            (text #:value (if (null? parent-id) "(root)" parent-id)
                                  #:style 'body-small)))))))

(define (app)
  (scaffold
   #:top-bar (top-app-bar #:title "Trace Viewer" #:style 'center-aligned)
   (column #:fill-max-size #t #:padding 16 #:spacing 16
           (surface #:fill-max-width #t #:shape 8 #:color "#FAFAFA" #:padding 8
                    (column #:fill-max-width #t #:spacing 2
                            (map render-span all-spans)))
           (details-panel))))

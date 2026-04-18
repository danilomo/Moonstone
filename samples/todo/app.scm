(define new-todo-text (state ""))
(define next-id (state 4))
(define completed-count (state 1))
(define total-count (state 3))

(define todos (state (list (list 1 "Buy groceries" #f) (list 2 "Walk the dog" #f) (list 3 "Read a book" #t))))

(define (get-id todo) (car todo))
(define (get-text todo) (cadr todo))
(define (get-done todo) (caddr todo))

(define (update-counts)
  (state-set! completed-count
    (length (filter (lambda (todo) (get-done todo)) (state-ref todos))))
  (state-set! total-count (length (state-ref todos))))

(define (toggle-todo id)
  (state-update! todos
    (lambda (items)
      (map (lambda (todo)
             (if (= (get-id todo) id)
                 (list (get-id todo)
                       (get-text todo)
                       (not (get-done todo)))
                 todo))
           items)))
  (update-counts))

(define (add-todo)
  (if (not (string=? (state-ref new-todo-text) ""))
      (begin
        (state-update! todos
          (lambda (items)
            (append items
                    (list (list (state-ref next-id)
                                (state-ref new-todo-text)
                                #f)))))
        (state-update! next-id (lambda (n) (+ n 1)))
        (state-set! new-todo-text "")
        (update-counts))))

(define (delete-todo id)
  (state-update! todos
    (lambda (items)
      (filter (lambda (todo) (not (= (get-id todo) id))) items)))
  (update-counts))

(define (render-todo todo)
  (surface
   #:fill-max-width 1
   #:shape 'rounded
   #:padding 12
   (row
    #:fill-max-width 1
    #:spacing 12
    #:vertical-alignment 'center
    (checkbox
     #:checked (get-done todo)
     #:on-change (lambda (v) (toggle-todo (get-id todo))))
    (text #:value (get-text todo) #:style 'body-large)
    (spacer #:width 1)
    (icon #:name "delete"
          #:size 20
          #:tint 'gray
          #:on-click (lambda () (delete-todo (get-id todo)))))))

(define (app)
  (scaffold
   #:top-bar (top-app-bar
              #:title "Todo List"
              #:style 'center-aligned)

   (column
    #:fill-max-size 1
    #:padding 16
    #:spacing 16

    (row
     #:fill-max-width 1
     #:spacing 8
     #:vertical-alignment 'center
     (text #:value "Your Tasks" #:style 'title-medium #:color 'gray)
     (spacer #:width 1)
     (text #:value completed-count #:style 'body-medium #:color 'gray)
     (text #:value " / " #:style 'body-medium #:color 'gray)
     (text #:value total-count #:style 'body-medium #:color 'gray)
     (text #:value " done" #:style 'body-medium #:color 'gray))

    (column
     #:fill-max-width 1
     #:spacing 8
     (outlined-text-field
      #:value new-todo-text
      #:label "New task"
      #:placeholder "What needs to be done?"
      #:fill-max-width 1
      #:on-change (lambda (v) (state-set! new-todo-text v)))
     (button
      #:on-click add-todo
      #:fill-max-width 1
      (text #:value "Add Task")))

    (dynamic-list
     #:items todos
     #:render-item render-todo
     #:fill-max-size 1
     #:spacing 8))))

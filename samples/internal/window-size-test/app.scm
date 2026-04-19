;; Window size configuration test
;; These variables are only used on desktop (ignored on Android)
(define *window-width* 600)
(define *window-height* 400)

(define (app)
  (box #:fill-max-size #t #:content-alignment 'center #:padding 32
    (column #:spacing 16 #:horizontal-alignment 'center
      (text #:value "Window Size Test" #:style 'headline-medium)
      (text #:value "This window should be 600x400 pixels")
      (text #:value "(Desktop only - Android ignores these settings)"))))

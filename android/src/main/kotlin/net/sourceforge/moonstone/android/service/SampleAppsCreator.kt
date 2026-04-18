package net.sourceforge.moonstone.android.service

import java.io.File

/**
 * Creates sample KleinLisp apps for demonstration purposes.
 */
object SampleAppsCreator {

    /**
     * Create sample apps in the given root folder.
     *
     * @param rootFolder The folder to create apps in
     * @return Number of apps created (skips existing ones)
     */
    fun createSampleApps(rootFolder: File): Int {
        if (!rootFolder.exists()) {
            rootFolder.mkdirs()
        }

        var created = 0

        // Calculator app
        if (createApp(rootFolder, "Calculator", CALCULATOR_CONFIG, CALCULATOR_CODE)) {
            created++
        }

        // Counter app
        if (createApp(rootFolder, "Counter", COUNTER_CONFIG, COUNTER_CODE)) {
            created++
        }

        // Component Showcase app
        if (createApp(rootFolder, "Showcase", SHOWCASE_CONFIG, SHOWCASE_CODE)) {
            created++
        }

        return created
    }

    private fun createApp(
        rootFolder: File,
        name: String,
        config: String,
        code: String
    ): Boolean {
        val appFolder = File(rootFolder, name)
        val scriptFile = File(appFolder, "app.scm")

        // Skip if already exists
        if (scriptFile.exists()) {
            return false
        }

        appFolder.mkdirs()

        // Write config
        File(appFolder, "app.conf").writeText(config)

        // Write script
        scriptFile.writeText(code)

        return true
    }

    // Calculator app
    private val CALCULATOR_CONFIG = """
[app]
name = Calculator
description = A simple calculator with basic operations
version = 1.0
author = KleinLisp
    """.trimIndent()

    private val CALCULATOR_CODE = """
(define display-value (state "0"))
(define current-op (state #f))
(define first-num (state 0))
(define clear-next (state #f))

(define (append-digit d)
  (if (state-ref clear-next)
      (begin
        (state-set! display-value (number->string d))
        (state-set! clear-next #f))
      (if (string=? (state-ref display-value) "0")
          (state-set! display-value (number->string d))
          (state-set! display-value
            (string-append (state-ref display-value) (number->string d))))))

(define (set-operation op)
  (state-set! first-num (string->number (state-ref display-value)))
  (state-set! current-op op)
  (state-set! clear-next #t))

(define (calculate)
  (define second-num (string->number (state-ref display-value)))
  (define op (state-ref current-op))
  (define result
    (cond
      ((eq? op 'add) (+ (state-ref first-num) second-num))
      ((eq? op 'sub) (- (state-ref first-num) second-num))
      ((eq? op 'mul) (* (state-ref first-num) second-num))
      ((eq? op 'div) (if (= second-num 0) 0 (/ (state-ref first-num) second-num)))
      (else second-num)))
  (state-set! display-value (number->string result))
  (state-set! current-op #f)
  (state-set! clear-next #t))

(define (clear-all)
  (state-set! display-value "0")
  (state-set! current-op #f)
  (state-set! first-num 0)
  (state-set! clear-next #f))

(define (calc-button label action)
  (button #:on-click action #:style 'tonal
    (text #:value label #:style 'title-medium)))

(define (op-button label op)
  (button #:on-click (lambda () (set-operation op)) #:style 'filled
    (text #:value label #:style 'title-medium)))

(define (digit-button d)
  (calc-button (number->string d) (lambda () (append-digit d))))

(define (button-row . buttons)
  (row #:spacing 8 #:horizontal-arrangement 'space-evenly #:fill-max-width #t
    buttons))

(define (app)
  (column #:padding 16 #:spacing 12 #:horizontal-alignment 'center
    (surface #:color 'surface-variant #:shape 'rounded #:padding 24 #:fill-max-width #t
      (text #:value display-value #:style 'display-medium))
    (spacer #:height 8)
    (button-row
      (calc-button "C" clear-all)
      (calc-button "+/-" (lambda () #f))
      (calc-button "%" (lambda () #f))
      (op-button "/" 'div))
    (button-row
      (digit-button 7)
      (digit-button 8)
      (digit-button 9)
      (op-button "x" 'mul))
    (button-row
      (digit-button 4)
      (digit-button 5)
      (digit-button 6)
      (op-button "-" 'sub))
    (button-row
      (digit-button 1)
      (digit-button 2)
      (digit-button 3)
      (op-button "+" 'add))
    (button-row
      (digit-button 0)
      (calc-button "." (lambda () #f))
      (button #:on-click calculate #:style 'filled
        (text #:value "=" #:style 'title-medium)))))
    """.trimIndent()

    // Counter app
    private val COUNTER_CONFIG = """
[app]
name = Counter
description = A simple counter with increment and decrement
version = 1.0
author = KleinLisp
    """.trimIndent()

    private val COUNTER_CODE = """
(define count (state 0))

(define (app)
  (box #:fill-max-size #t #:content-alignment 'center #:padding 32
    (column #:spacing 24 #:horizontal-alignment 'center
      (text #:value "Counter App" #:style 'headline-medium)
      (surface #:color 'primary-container #:shape 'rounded #:padding 32
        (text #:value count #:style 'display-large #:color 'on-primary-container))
      (row #:spacing 16
        (button #:on-click (lambda () (state-update! count (lambda (n) (- n 1))))
          #:style 'outlined
          (row #:padding-horizontal 16 #:vertical-alignment 'center
            (icon #:name "remove" #:size 24)
            (text #:value "Decrease")))
        (button #:on-click (lambda () (state-update! count (lambda (n) (+ n 1))))
          #:style 'filled
          (row #:padding-horizontal 16 #:vertical-alignment 'center
            (icon #:name "add" #:size 24)
            (text #:value "Increase"))))
      (button #:on-click (lambda () (state-set! count 0))
        #:style 'text
        (text #:value "Reset")))))
    """.trimIndent()

    // Component Showcase app
    private val SHOWCASE_CONFIG = """
[app]
name = Showcase
description = Demonstrates Moonstone components
version = 1.0
author = KleinLisp
    """.trimIndent()

    private val SHOWCASE_CODE = """
(define current-tab (state 0))
(define text-input (state ""))
(define switch-on (state #f))
(define checkbox-checked (state #f))

(define (buttons-view)
  (column #:spacing 16 #:padding 16 #:fill-max-width #t
    (text #:value "Button Styles" #:style 'title-large)
    (button #:on-click (lambda () (toast "Filled button clicked!"))
      #:style 'filled
      (text #:value "Filled Button"))
    (button #:on-click (lambda () (toast "Outlined button clicked!"))
      #:style 'outlined
      (text #:value "Outlined Button"))
    (button #:on-click (lambda () (toast "Text button clicked!"))
      #:style 'text
      (text #:value "Text Button"))
    (button #:on-click (lambda () (toast "Elevated button clicked!"))
      #:style 'elevated
      (text #:value "Elevated Button"))
    (button #:on-click (lambda () (toast "Tonal button clicked!"))
      #:style 'tonal
      (text #:value "Tonal Button"))))

(define (inputs-view)
  (column #:spacing 16 #:padding 16 #:fill-max-width #t
    (text #:value "Input Components" #:style 'title-large)
    (text-field #:value text-input
      #:label "Text Field"
      #:on-change (lambda (v) (state-set! text-input v)))
    (outlined-text-field #:value text-input
      #:label "Outlined Text Field"
      #:on-change (lambda (v) (state-set! text-input v)))
    (text #:value (string-append "You typed: " (state-ref text-input))
      #:style 'body-medium)
    (spacer #:height 8)
    (switch #:checked switch-on
      #:on-change (lambda (v) (state-set! switch-on v))
      (text #:value "Toggle Switch"))
    (checkbox #:checked checkbox-checked
      #:on-change (lambda (v) (state-set! checkbox-checked v))
      (text #:value "Checkbox"))))

(define (icons-view)
  (column #:spacing 16 #:padding 16 #:fill-max-width #t
    (text #:value "Material Icons" #:style 'title-large)
    (row #:spacing 16 #:horizontal-arrangement 'center #:fill-max-width #t
      (icon #:name "home" #:size 32 #:on-click (lambda () (toast "Home")))
      (icon #:name "search" #:size 32 #:on-click (lambda () (toast "Search")))
      (icon #:name "settings" #:size 32 #:on-click (lambda () (toast "Settings")))
      (icon #:name "person" #:size 32 #:on-click (lambda () (toast "Person"))))
    (row #:spacing 16 #:horizontal-arrangement 'center #:fill-max-width #t
      (icon #:name "favorite" #:size 32 #:tint "red")
      (icon #:name "star" #:size 32 #:tint "gold")
      (icon #:name "check" #:size 32 #:tint "green")
      (icon #:name "close" #:size 32 #:tint "gray"))
    (row #:spacing 16 #:horizontal-arrangement 'center #:fill-max-width #t
      (icon #:name "add" #:size 32)
      (icon #:name "delete" #:size 32)
      (icon #:name "edit" #:size 32)
      (icon #:name "info" #:size 32))))

(define (app)
  (scaffold
    #:top-bar (top-app-bar #:title "Component Showcase" #:style 'center-aligned)
    #:bottom-bar (bottom-navigation #:selected current-tab
      (nav-item #:icon "touch_app" #:label "Buttons" #:value 0
        #:on-select (lambda () (state-set! current-tab 0)))
      (nav-item #:icon "edit" #:label "Inputs" #:value 1
        #:on-select (lambda () (state-set! current-tab 1)))
      (nav-item #:icon "palette" #:label "Icons" #:value 2
        #:on-select (lambda () (state-set! current-tab 2))))
    (switch-view #:selected current-tab
      (view #:value 0 (buttons-view))
      (view #:value 1 (inputs-view))
      (view #:value 2 (icons-view)))))
    """.trimIndent()
}

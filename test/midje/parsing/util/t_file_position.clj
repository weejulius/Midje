(ns midje.parsing.util.t-file-position
  (:use [midje.parsing.util.file-position]
        [midje sweet test-util])
  (:require [clojure.zip :as zip]
            [midje.parsing.util.recognizing :as recognize]))

(defn this-file [line-number] 
  ["t_file_position.clj" line-number])

;; Throughout this file, file positions are captured outside of
;; facts. That's because facts have their own mechanism for file
;; position, and I want it to be clear that this is just working with
;; the base (utility) function.










(def line-marker-2 24)
(unfinished f)
(let [fake-on-one-line (fake (f 1) => 33)
      multiline-with-position-at-first-token (fake

                                              (f 1)

                                              =>

                                              33)]
  (fact "fake, being a one-level macro, knows its file position as a single line"
    (:position fake-on-one-line) => (this-file (+ 2 line-marker-2))
    (:position multiline-with-position-at-first-token) => (this-file (+ 5 line-marker-2))))

(defmacro result-of-second-form [& forms] (second forms))

(def line-marker-3 (+ line-marker-2 16))
(let [fake (result-of-second-form
            "random garbage"
            (fake (f 1) => 33)
            "more garbage")]
  (fact "Macros within dirt-simple macroexpansions find their correct file position"
    (:position fake) => (this-file (+ 3 line-marker-3))))

(defmacro fake-constructor [& forms]
  `(do
     (fake ~(nth forms 1) => ~(nth forms 3))))


(def line-marker-4 (+ line-marker-3 13))
(let [fake (fake-constructor        
                      "random garbage"
                      (f 1) => 33)] 
  (fact 
    (:position fake) => (this-file (+ 3 line-marker-4))))


;; Macros like the above will need to calculate the file position themselves, but
;; the filename will be valid.
(fact "line-number-known is used when you know the line but not the file"
  (let [position (line-number-known 33)]
    position => ["t_file_position.clj", 33]))

(facts "about determining a line number from forms near an arrow"
  "Typical case is form on left. (f 1) => 5"
  (let [form `( ~(at-line 33 '(f 1)) => 5)
        loc (-> form zip/seq-zip zip/down)]
    loc => recognize/start-of-checking-arrow-sequence?
    (arrow-line-number (zip/right loc)) => 33)

  "When form on the left is has no line, check right: ...a... => (exactly 1)"
  (let [form `( ...a... => ~(at-line 33 '(exactly 1)))
        loc (-> form zip/seq-zip zip/down)]
    loc => recognize/start-of-checking-arrow-sequence?
    (arrow-line-number (zip/right loc)) => 33)

  "If both sides have line numbers, the left takes precedence: (f 1) => (exactly 1)"
  (let [form `( ~(at-line 33 '(f 1)) => ~(at-line 34 '(exactly 1)))
        loc (-> form zip/seq-zip zip/down)]
    loc => recognize/start-of-checking-arrow-sequence?
    (arrow-line-number (zip/right loc)) => 33)

  "If neither side has a line number, look to the left and add 1: (let [a 2] a => b)"
  (let [form `( (let ~(at-line 32 '[a 2]) a => b))
        loc (-> form zip/seq-zip zip/down zip/down zip/right zip/right)]
    loc => recognize/start-of-checking-arrow-sequence?
    (arrow-line-number (zip/right loc)) => 33)

  "Default result is is one plus the fallback line number."
  (set-fallback-line-number-from (at-line 333 '(previous form)))
  (let [form '(1 => 2)
        loc (-> form zip/seq-zip zip/down)]
    loc => recognize/start-of-checking-arrow-sequence?
    (arrow-line-number (zip/right loc)) => 334

    ;; incrementing happens more than once
    (arrow-line-number (zip/right loc)) => 335


    (let [another-form `( ~(at-line 3 '(f 1)) => 5) ]
      (-> another-form zip/seq-zip zip/down zip/right arrow-line-number)
      (arrow-line-number (zip/right loc)) => 4)))

(facts "about finding the arrow-line-number from a form"
  (let [form `( ~(at-line 333 '(f 1)) => 3)]
    (arrow-line-number-from-form form) => 333))

(facts "about compile-time discovery of positions and line numbers from a form"
  (form-position (with-meta '(form) {:line 332}))
  => ["t_file_position.clj" 332])
                   


(defn lineno
  ([tree] (get (meta tree) :line :not-found))
  ([tree n] (get (meta (nth tree n)) :line :not-found)))

(fact "metadata can be copied from one tree to a matching tree"
  (let [line-number-source '(This has
                      (some line numbers)
                      (on it))
        form-source '(The line  
                      (numbers of this)
                      (tree differ))
        result (form-with-copied-line-numbers line-number-source form-source)]

    line-number-source =not=> form-source
    result => form-source
    
    (lineno form-source) =not=> (lineno line-number-source)
    (lineno form-source 2) =not=> (lineno line-number-source 2)
    (lineno form-source 3) =not=> (lineno line-number-source 3)

    (lineno result) => (lineno line-number-source)
    (lineno result 2) => (lineno line-number-source 2)
    (lineno result 3) => (lineno line-number-source 3)))

(fact "The metadata tree might have nodes where the other tree has branches"
  (let [line-number-source '(This
                 ?1
                 (that)
                 ?2)
        form-source '(This
                      (something (deeply (nested)))
                      (that)
                      (something (deeply (nested))))
        result (form-with-copied-line-numbers line-number-source form-source)]

    line-number-source =not=> form-source
    result => form-source

    (lineno line-number-source 1) => :not-found
    (lineno line-number-source 3) => :not-found
    (lineno form-source 1) =not=> nil
    (lineno form-source 3) =not=> nil
    (lineno result 1) => :not-found
    (lineno result 3) => :not-found

    (lineno result) =not=> (lineno form-source)
    (lineno result) => (lineno line-number-source)

    (lineno result 2) =not=> (lineno form-source 2)
    (lineno result 2) => (lineno line-number-source 2)))

(fact "other metadata is left alone"
  (let [line-number-source '(This (that))
        form-source `(This
                      ~(with-meta
                         '(something (deeply (nested)))
                         {:meta :data, :line 33}))
        result (form-with-copied-line-numbers line-number-source form-source)]
    (lineno result 1) => (lineno line-number-source 1)
    (:meta (meta (nth result 1))) => :data))


(fact "adding line number information"
  (let [form-without-line-number (cons 'without '(line))
        form-with-line-number '(with line)]
    (fact "to a form without one updates it"
      (:line (meta (positioned-form form-without-line-number 8888))) => 8888)
    (fact "to a form with one does not"
      (:line (meta (positioned-form form-with-line-number 8888))) =not=> 8888)
    (fact "the source of the line number can be a form"
      (:line (meta (positioned-form form-without-line-number form-with-line-number)))
      => (:line (meta form-with-line-number)))))

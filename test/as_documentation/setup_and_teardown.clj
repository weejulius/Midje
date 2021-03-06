(ns as-documentation.setup-and-teardown
  (:use midje.repl
        midje.test-util))

(def state (atom nil))


                                        ;;; setup

(with-state-changes [(before :facts (reset! state 0))]
  (fact "the state gets set before this fact"
    @state => 0)

  (fact "it also gets set before this fact"
    @state => 0))
    
;;; It's common to put `with-state-changes` inside an outermost fact

(facts swap!
  (with-state-changes [(before :facts (reset! state 0))]
    (fact "uses a function to update the current value"
      (swap! state inc)
      @state => 1)

    (fact "that function can take additional args"
      (swap! state - 33)
      @state => -33)

    (fact "swap returns the new value"
      (swap! state inc) => 1)))

(def log (atom :undefined))

(fact "*all* nested setups are run before *each* fact"
  (with-state-changes [(before :facts (reset! log []))]
    (fact "an outer fact"
      (swap! log conj "this will get overwritten")

      (with-state-changes [(before :facts (swap! log conj "from inner with-state-changes"))]
        (fact
          ;; the outer `before` has just reset the log
          @log => ["from inner with-state-changes"])))))


                                        ;;; teardown

;; Combining setup and teardown. 

(fact "whereas `before` executes outer-to-inner, `after` is the reverse"
  (with-state-changes [(before :facts (reset! log ["outer in"]))
                       (after :facts (swap! log conj "outer out"))]
    (with-state-changes [(before :facts (swap! log conj "  inner in"))
                         (after :facts (swap! log conj "  inner out"))]
      (fact (+ 1 1) => 2)))
  @log => ["outer in"
           "  inner in"
           "  inner out"
           "outer out"])

(fact "teardown is executed even if the enclosed fact throws an exception."
  (try 
    (with-state-changes [(after :facts (reset! log ["caught!"]))]
      (fact
        (throw (new Error))))
  (catch Error ex))
  @log => ["caught!"])
  
    
(fact "teardown is NOT executed when the corresponding `before` throws an exception."
  ;; Use `around` instead.
  (try 
    (with-state-changes [(before :facts (do (reset! log [])
                                            (throw (new Error))))
                         (after :facts (reset! log ["caught!"]))]
      (fact))
  (catch Error ex))
  @log =not=> ["caught!"])
      
                                        ;;; repl-state-changes

;;; The earlier example of testing `swap!` surrounded three facts with
;;; an `with-state-changes`. That prevents you from using only one of
;;; the facts in the repl.  An alternative to awkward editing is to
;;; use `repl-state-changes`. It establishes a namespace-specific background.

(repl-state-changes [(before :facts (reset! state 0))])

;;; Note that the syntax is the same as for `with-state-changes`. Now you
;;; can use a fact that depends on the background:

(fact "uses a function to update the current value"
  (swap! state inc)
  @state => 1)

;;; Note that any change to the global background replaces
;;; everything. For example, the following doesn't add teardown to the
;;; earlier setup. There is no longer any setup, just teardown.

(repl-state-changes [(after :facts (swap! state inc))])

(reset! state 1000)
(fact "the `before` no longer happens"
  @state => 1000)

(fact "... but the `after` did"
  @state => 1001)

;;; Therefore, to "erase" the background, you can do this:

(repl-state-changes [(after :facts identity)])
(fact @state => 1002)
(fact @state => 1002)

(ns midje.checking.t-core
  (:use midje.sweet
        midje.checking.core
        midje.test-util))

(facts "about an extended notion of falsehood"
  (extended-false? false) => truthy
  (extended-false? true) => falsey
  (extended-false? {:intermediate-results 3}) => falsey
  (extended-false? (as-data-laden-falsehood {})) => truthy

  ".. and its inverse"
  (extended-true? false) => falsey
  (extended-true? true) => truthy
  (extended-true? {:intermediate-results 3}) => truthy
  (extended-true? (as-data-laden-falsehood {})) => falsey)

(facts "about data-laden falsehoods"
  (as-data-laden-falsehood [5]) => data-laden-falsehood?
  (meta (as-data-laden-falsehood (with-meta [5] {:foo true}))) => (contains {:foo true}))
  
(facts "user-friendly-falsehood converts extended-falsehood into just false"
  (user-friendly-falsehood false) => false
  (user-friendly-falsehood nil) => nil
  (user-friendly-falsehood (as-data-laden-falsehood {})) => false)

  
  




(facts "about extended equality"
  (extended-= 1 2) => falsey
  (extended-= 1 odd?) => truthy

  (let [checker (fn [expected] (chatty-checker [actual] (> (inc actual) expected)))]
    (extended-= 5 ((checker 5) 4)) => falsey)

  "regexps"
  (extended-= #"a*b+" #"a*b+") => truthy
  (extended-= #"a*b+" #"a*b") => falsey
  (extended-= "BEGIN aab END" #"a*b+") => truthy
  (extended-= "BEGIN bb END" #"ab+") => falsey

  ;; When searching for unordered comparisons, you might get exceptions.
  ;; Count those as false.
  (extended-= nil odd?) => falsey)

(fact "extended equality can be applied to lists"
  (extended-list-= [] []) => truthy
  (extended-list-= [1] [1]) => truthy
  (extended-list-= ['()] [seq?]) => truthy
  (extended-list-= ['() 1] [seq? seq?]) => falsey)

(defrecord AB [a b])
(defrecord AB2 [a b])

(fact "extended equality allows one-way comparison of records to maps"
  "First, regular maps"
  (extended-= {:a 1} {:a 1}) => truthy
  (extended-= {:a 1} {:a 2}) => falsey
  (extended-= {:a 1} {:b 1}) => falsey
  (extended-= {:a 1} {:a 1, :b 1}) => falsey
  (extended-= {:a 1, :b 1} {:a 1}) => falsey

  "And records compared to records"
  (extended-= (AB. 1 2) (AB. 1 2)) => truthy
  (extended-= (AB. 1 2) (AB. 1 3)) => falsey
  (extended-= (AB. 1 2) (AB2. 1 2)) => falsey
  (extended-= (assoc (AB. 1 2) :c 3) (AB. 1 2)) => falsey

  "Maps on the left never match records on the right"
  (extended-= {:a 1, :b 2} (AB. 1 2)) => falsey

  "Maps on the RIGHT can match records on the left"
  (extended-= (AB. 1 2) {:a 1, :b 2}) => truthy
  (extended-= (AB. 1 2) {:a 1, :b 1}) => falsey
  (extended-= (AB. 1 2) {:a 1, :b 2, :c 3}) => falsey
  (extended-= (assoc (AB. 1 2) :c 3) {:a 1, :b 2, :c 3}) => truthy
  )
  
(fact "for functions, you can get information about the failure"
  (first (evaluate-checking-function odd? 3)) => true
  (evaluate-checking-function odd? 4) => [false {}]
  (let [extra ((just 1) [2])]
    (evaluate-checking-function (just 1) [2]) => [false extra])
  (let [error (Error.)]
    (evaluate-checking-function (fn [actual] (throw error)) anything) => [false {:thrown error}]))

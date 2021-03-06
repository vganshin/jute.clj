(ns jute.core-test
  (:refer-clojure :exclude [compile])
  (:require [clojure.test :refer :all]
            [jute.core :refer :all]
            [yaml.core :as yaml]))

(deftest expressions-test
  (is (= false (compile* "$ true && false" {})))
  (is (= true (compile* "$ false || true" {})))
  (is (compile* "$ 42 > 3" {}))
  (is (compile* "$ 42 >= 42" {}))
  (is (compile* "$ 42 != true" {}))

  (is (= ((compile* "$ foo + 42" {}) {:foo 5}) 47))
  (is (= ((compile* "$ foo + 42" {}) {:foo "a"}) "a42")))

(deftest if-directive-test
  (let [suite (yaml/from-file "spec/if_directive.yml" true)]
    (doseq [{:keys [desc scope template result]} (:tests suite)]
      (testing desc
        (is (= result ((compile template) scope)))))))

(deftest let-directive-test
  (let [suite (yaml/from-file "spec/let_directive.yml" true)]
    (doseq [{:keys [desc scope template result]} (:tests suite)]
      (testing desc
        (is (= result ((compile template) scope)))))))

(deftest map-directive-test
  (let [suite (yaml/from-file "spec/map_directive.yml" true)]
    (doseq [{:keys [desc scope template result]} (:tests suite)]
      (testing desc
        (is (= result ((compile template) scope)))))))

(deftest fn-directive-test
  (let [suite (yaml/from-file "spec/fn_directive.yml" true)]
    (doseq [{:keys [desc scope template result]} (:tests suite)]
      (testing desc
        (is (= result ((compile template) scope)))))))

(deftest call-directive-test
  (let [suite (yaml/from-file "spec/call_directive.yml" true)]
    (doseq [{:keys [desc scope template result]} (:tests suite)]
      (testing desc
        (is (= result ((compile template) scope)))))))

(deftest expressions-test
  (let [suite (yaml/from-file "spec/expressions.yml" true)]
    (doseq [{:keys [desc scope template result]} (:tests suite)]
      (testing desc
        (is (= result ((compile template) scope)))))))

(deftest special-options-test
  (let [suite (yaml/from-file "spec/special_options.yml" true)]
    (doseq [{:keys [desc scope template result options]} (:tests suite)]
      (testing desc
        (is (= result ((compile template options) scope)))))))

(deftest special-options-test
  (let [suite (yaml/from-file "spec/switch_directive.yml" true)]
    (doseq [{:keys [desc scope template result]} (:tests suite)]
      (testing desc
        (is (= result ((compile template) scope)))))))

(deftest special-options-test
  (let [suite (yaml/from-file "spec/reduce_directive.yml" true)]
    (doseq [{:keys [desc scope template result]} (:tests suite)]
      (testing desc
        (is (= result ((compile template) scope)))))))

(deftest functions-test
  (let [suite (yaml/from-file "spec/functions_spec.yml" true)]
    (doseq [{:keys [desc scope template result]} (:tests suite)]
      (testing desc
        (is (= result ((compile template) scope)))))))

(deftest documentation-examples-test
  (testing "The first example from README.md"
    (let [template {:type "book"
                    :author "$ book.author.name"
                    :title "$ book.title"
                    :content {:$map "$ book.chapters.*(this.type = \"content\")"
                              :$as "ch"
                              :$body "$ ch.content"}
                    :content2 "$ book.chapters.*(this.type = \"content\").content"}

          scope {:book {:author {:name "M. Soloviev"
                                 :title "PHD"
                                 :gender "m"}
                        :title "Approach to Cockroach"
                        :chapters [{:type "preface"
                                    :content "A preface chapter"}
                                   {:type "content"
                                    :content "Chapter 1"}
                                   {:type "content"
                                    :content "Chapter 2"}
                                   {:type "content"
                                    :content "Chapter 3"}
                                   {:type "afterwords"
                                    :content "Afterwords"}]}}]

      (is (= ((compile template) scope)
             {:type "book"
              :author "M. Soloviev"
              :title "Approach to Cockroach"
              :content ["Chapter 1" "Chapter 2" "Chapter 3"]
              :content2 ["Chapter 1" "Chapter 2" "Chapter 3"]}))))

  (testing "The FizzBuzz challenge"
    (let [t {:$call "joinStr"
             :$args [" "
                     {:$map "$ range(0, 50, 1)"
                      :$as "num"
                      :$body {:$let
                              [{:s ""}
                               {:s {:$if "$ num % 3 = 0"
                                    :$then "$ s + \"Fizz\""
                                    :$else "$ s"}}

                               {:s {:$if "$ num % 5 = 0"
                                    :$then "$ s + \"Buzz\""
                                    :$else "$ s"}}]

                              :$body {:$if "$ s = \"\""
                                      :$then "$ num"
                                      :$else "$ toString(num) + \"-\" + s"}}}]}]
      (is (= "0-FizzBuzz 1 2 3-Fizz 4 5-Buzz 6-Fizz 7 8 9-Fizz 10-Buzz 11 12-Fizz 13 14 15-FizzBuzz 16 17 18-Fizz 19 20-Buzz 21-Fizz 22 23 24-Fizz 25-Buzz 26 27-Fizz 28 29 30-FizzBuzz 31 32 33-Fizz 34 35-Buzz 36-Fizz 37 38 39-Fizz 40-Buzz 41 42-Fizz 43 44 45-FizzBuzz 46 47 48-Fizz 49"
             ((compile t) {}))))))


(deftest compilation-exceptions-test
  (testing "Reports template path where compilation failed"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"While compiling JUTE template at @.foo.0"
                          (compile {:foo ["$ 30c"]})))

    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"While compiling JUTE template at @.\$then"
                          (compile {:$if "$ a"
                                    :$then "$ 42b"
                                    :$else false})))

    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"While compiling JUTE template at @.\$if"
                          (compile {:$if "$ 3 / 0"
                                    :$then 42
                                    :$else false}))))

  (testing "Provides in exception information"
    (try
      (compile {:$if "$ a"
                :$then "$ 42b"
                :$else false})

      (catch clojure.lang.ExceptionInfo e
        (is (= [(keyword "@") :$then] (:path (.getData e))))
        (is (= "@.$then" (:path-str (.getData e))))))))

(deftest evaluation-exceptions-test
  (testing "Reports template path where evaluation failed"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"While evaluating JUTE template at @.\$body"
                          ((compile {:$map "$ a"
                                     :$as "i"
                                     :$body "$ b / 0"}) {:a [1] :b 1}))))

  (testing "Expressions thrown in JUTE functions reports correct path"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"While evaluating JUTE template at @.\$let.f.\$body"
                          ((compile {:$let {:f {:$fn [:a]
                                                :$body "$ a / 0"}}
                                     :$body {:b "$ f(a)"}}) {:a 1})))))

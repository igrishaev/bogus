(ns bogus.core-test
  (:require
   [bogus.core :refer [with-locals
                       wrap-do
                       eval+]]
   [clojure.test :refer [is deftest testing]]))


(deftest test-with-locals

  (let [a 1
        b 2]

    (with-locals [locals1]

      (let [c 3
            d 4]

        (with-locals [locals2]

          (is (= '{a 1, b 2, locals1 {a 1, b 2}, c 3, d 4}
                 locals2)))))))


(deftest test-wrap-do
  (is (= '(do (+ 1 2) (+ 1 3))
         (wrap-do "(+ 1 2) (+ 1 3)"))))


(deftest test-eval+

  (let [result
        (eval+ '{aa 1 bb 2 xx 3}
               '(+ aa bb xx))]

    (is (= 6 result))))

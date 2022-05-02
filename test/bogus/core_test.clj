(ns bogus.core-test
  (:require
   [bogus.core :refer [eval+
                       wrap-do
                       with-locals
                       debug-reader]]
   [clojure.test :refer [is deftest testing]]))


(deftest test-wrap-do
  (is (= "(do (+ 1 2)(+ 1 3))"
         (wrap-do "(+ 1 2)(+ 1 3)")))

  (is (= "(do nil)"
         (wrap-do "nil"))))


(deftest test-with-locals

  (let [a 1
        b 2]

    (with-locals [locals1]

      (let [c 3
            d 4]

        (with-locals [locals2]

          (is (= '{a 1, b 2, locals1 {a 1, b 2}, c 3, d 4}
                 locals2)))))))


(deftest test-eval+

  (let [the-ns
        (find-ns 'bogus.core)

        result
        (eval+ the-ns
               {'aaa (list 1 2 3)
                'bbb (list 4 5 6)}
               '(into aaa bbb))]

    (is (= '(6 5 4 1 2 3)
           result))))


(def GLOBAL_VAR 999)


(deftest test-eval-global-vars

  (let [the-ns
        (find-ns 'bogus.core-test)

        result
        (eval+ the-ns
               {'aaa 1}
               '(+ GLOBAL_VAR aaa))]

    (is (= 1000 result))))


(deftest test-eval-global-vars-missing

  (let [the-ns
        (find-ns 'bogus.core)]

    (is (thrown?
         Exception

         (eval+ the-ns
                {'aaa 1}
                '(+ GLOBAL_VAR aaa))))))


(deftest test-tag-reader

  (is (= '(do (bogus.core/debug {:line 83 :column 25 :form (println 42)})
              (println 42))

         (debug-reader '(println 42)))))


(deftest test-tag-reader-when

  (is (= '(do
            (clojure.core/when (= i 42)
              (bogus.core/debug
               {:line 94 :column 18 :when (= i 42) :form (println 42)}))
            (println 42))

         '#bogus ^{:when (= i 42)} (println 42))))

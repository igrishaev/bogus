(ns bogus.core-test
  (:require
   [bogus.core :refer [eval+
                       wrap-do
                       get-old-sym
                       get-fq-sym
                       with-locals
                       with-globalize]]
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


(def FOO 111)
(def BAR 222)

(deftest test-with-globalize

  (let [the-ns
        (find-ns 'bogus.core-test)]

    (with-globalize the-ns '{FOO 42 BAR 33}

      (is (= 111 @(resolve 'bogus.core-test/__OLD_FOO__)))
      (is (= 222 @(resolve 'bogus.core-test/__OLD_BAR__)))

      (is (= 42 @(resolve 'bogus.core-test/FOO)))
      (is (= 33 @(resolve 'bogus.core-test/BAR)))

      (is (= 75
             (binding [*ns* the-ns]
               (eval '(+ FOO BAR))))))

    (is (nil? (resolve 'bogus.core-test/__OLD_FOO__)))
    (is (nil? (resolve 'bogus.core-test/__OLD_BAR__)))

    (is (= 111 @(resolve 'bogus.core-test/FOO)))
    (is (= 222 @(resolve 'bogus.core-test/BAR)))))


(deftest test-syn-naming

  (is (= '__OLD_my-var__
         (get-old-sym 'my-var)))

  (let [the-ns
        (find-ns 'bogus.core)]

    (is (= 'bogus.core/aaa
           (get-fq-sym the-ns 'aaa)))))


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

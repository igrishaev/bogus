(ns bogus.core-test
  (:require
   [clojure.test :refer [is deftest testing]]
   [bogus.core :refer [with-globals eval+]]))



(def A 1)
(def B 2)
(def C 3)


(deftest test-with-globals-ok

  (let [A 100
        B 200]

    (let [result
          (with-globals
            (+ A B C))]

      (is (= 303 result)))))


(deftest test-with-globals-locals-map

  (let [A 100
        B 200]

    (with-globals

      (let [locals
            @(resolve '__locals__)]

        (is (= '{A 100, B 200} locals))))))


(deftest test-with-globals-old-symbols

  (let [A 100
        B 200]

    (with-globals

      (let [A*
            @(resolve 'A)

            B*
            @(resolve 'B)

            C*
            @(resolve 'C)

            A-old
            @(resolve '__OLD_A__)

            B-old
            @(resolve '__OLD_B__)

            C-old
            (resolve '__OLD_C__)]

        (is (= 100 A*))
        (is (= 200 B*))
        (is (=   3 C*))

        (is (= 1 A-old))
        (is (= 2 B-old))
        (is (nil? C-old)))))

  (is (= A 1))
  (is (= B 2))
  (is (= C 3))

  (is (nil? (resolve '__OLD_A__)))
  (is (nil? (resolve '__OLD_B__))))


(deftest test-with-globals-nested

  (let [A 100
        B 200]

    (let [foo :a
          bar :b

          func
          (fn [x]

            (with-globals

              (let [locals
                    @(resolve '__locals__)]

                (is (= '{A 100, B 200, foo :a, bar :b, x 42}
                       locals)))))]

      (func 42))))


;; eval+

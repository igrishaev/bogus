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


#_
(deftest test-with-globals-old-symbols

  (let [A 100
        B 200]

    (with-globals

      (is (= 100 A))
      (is (= 200 B))
      (is (=   3 C))

      (is (= 1 @(resolve '__OLD_A__)))
      (is (= 2 @(resolve '__OLD_B__)))
      (is (nil? (resolve '__OLD_C__))))

    (is (= A 100))
    (is (= B 200))

    (is (nil? (resolve '__OLD_A__)))
    (is (nil? (resolve '__OLD_B__))))

  (is (= A 1))
  (is (= B 2))
  (is (= C 3)))


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


(deftest test-with-globals-locals-levels

  (let [p 1
        q 2]

    (with-globals

      (let [locals1
            @(resolve '__locals__)]

        (is (= '{p 1, q 2} locals1))

        (let [x 3
              y 4]

          (with-globals

            (let [locals2
                  @(resolve '__locals__)]

              (is (= '{p 1, q 2, locals1 {p 1, q 2}, x 3, y 4}
                     locals2))))

          ;;
          ;; ideally, the locals from level 1 must be still available
          ;;
          (let [locals1
                (resolve '__locals__)]

            (is (nil? locals1))))))))


(deftest test-eval+

  (let [a 10
        b 20]

    (is (= 30 (eval+ (+ a b))))))

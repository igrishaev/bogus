(ns bogus.core-test
  (:require
   [bogus.core :refer [with-locals
                       with-globalize]]
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


(deftest test-with-globalize

  (intern *ns* 'foo 111)
  (intern *ns* 'bar 222)

  (with-globalize *ns* '{foo 42 bar 33}

    (is (= 111 @(resolve '__OLD_foo__)))
    (is (= 222 @(resolve '__OLD_bar__)))

    (is (= 42 @(resolve 'foo)))
    (is (= 33 @(resolve 'bar)))
    (is (= 75 (eval '(+ foo bar)))))

  (is (nil? (resolve '__OLD_foo__)))
  (is (nil? (resolve '__OLD_bar__)))

  (is (= 111 @(resolve 'foo)))
  (is (= 222 @(resolve 'bar)))

  (ns-unmap *ns* 'foo)
  (ns-unmap *ns* 'bar))

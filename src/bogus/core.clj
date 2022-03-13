(ns bogus.core
  (:require
   [clojure.main :as main]
   [clojure.pprint :as pprint]
   [clojure.inspector :as inspector]))


(defmacro with-globals [& body]

  (let [syms
        (vec (keys &env))

        ;; syms
        ;; '[a b c]

        sym-pairs
        (for [sym syms]
          [sym (symbol (format "__OLD_%s__" sym))])]

    `(do

       (intern ~'*ns* '~'__locals__
               ~(into {} (for [sym syms]
                           [(list 'quote sym) sym])))

       ~@(for [[sym sym-old] sym-pairs]
           `(do
              (when-let [sym-var# (resolve '~sym)]
                (intern ~'*ns* '~sym-old @sym-var#))
              (intern ~'*ns* '~sym ~sym)))

       (try

         ~@body

         (finally

           (ns-unmap ~'*ns* '~'__locals__)

           ~@(for [[sym sym-old] (reverse sym-pairs)]
               `(do
                  (ns-unmap ~'*ns* '~sym)
                  (when-let [sym-var# (resolve '~sym-old)]
                    (intern ~'*ns* '~sym @sym-var#)
                    (ns-unmap ~'*ns* '~sym-old)))))))))


(defmacro eval+ [& body]
  `(with-globals
     (eval ~(list* 'do ~@body))))


(defn repl-eval [form]
  (case form

    (?help ?locals ?dump ?inspect)
    form

    ;; else
    (eval form)))


(defn repl-print [val]

  (let [locals
        (some-> '__locals__ resolve deref)]

    (case val

      ?help
      (do
        (println)
        (println "Help:")
        (println "------------------")
        (println "?help    show this message")
        (println "?locals  show local variables")
        (println "?dump    save local variables to the 'dump.edn' file")
        (println "?exit    quit debugging"))

      ?locals
      (do
        (println)
        (println "Locals:")
        (println "------------------")
        (pprint/pprint locals))

      ?inspect
      (inspector/inspect-tree locals)

      ?dump
      (do
        (spit "dump.edn"
              (with-out-str
                (pprint/pprint locals)))
        (println "Saved to the file 'dump.edn'"))


      ;; else
      (pprint/pprint val))))


(defn repl-read [request-prompt request-exit]
  (let [result
        (main/repl-read request-prompt request-exit)]

    (if (= result '?exit)
      request-exit
      result)))


(def repl-defaults
  {:eval repl-eval
   :print repl-print
   :read repl-read})


(defmacro debug [& repl-opt]
  `(with-globals
     (apply main/repl
            (apply concat (merge ~repl-defaults ~repl-opt)))))


(defn debug-reader [form]
  `(do (debug) ~form))

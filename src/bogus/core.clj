(ns bogus.core
  (:require
   [clojure.main :as main]
   [clojure.pprint :as pprint]
   [clojure.inspector :as inspector]
   [clojure.stacktrace :as trace]))


(def dump-file "dump.edn")


(defmacro with-globals [& body]

  (let [syms
        (vec (keys &env))

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
     (eval '(do ~@body))))


(defn repl-eval [form]
  (case form

    (!h !help !l !locals !d !dump !i !inspect)
    form

    ;; else
    (eval form)))


(def help-message
  (with-out-str
    (println)
    (println         "Help:")
    (println         "------------------")
    (println         "!h, !help         show this message")
    (println         "!l, !locals       show local variables")
    (println (format "!d, !dump         save local variables to the '%s' file" dump-file))
    (println         "!q, !quit, !exit  quit debugging")))


(defn repl-print [val]

  (let [locals
        (some-> '__locals__ resolve deref)]

    (case val

      (!h !help)
      (do
        (println help-message))

      (!l !locals)
      (do
        (println)
        (println "Locals:")
        (println "------------------")
        (pprint/pprint locals))

      (!i !inspect)
      (inspector/inspect-tree locals)

      (!d !dump)
      (do
        (spit dump-file
              (with-out-str
                (pprint/pprint locals)))
        (println (format "Saved to the file '%s'" dump-file))
        (println))


      ;; else
      (do
        (pprint/pprint val)))))


(defn repl-read [request-prompt request-exit]
  (let [result
        (main/repl-read request-prompt request-exit)]

    (if (contains? '#{!q !quit !exit} result)
      request-exit
      result)))


(defn make-repl-prompt [& [options]]
  (let [title
        (get options :title "debug")]
    (fn []
      (printf "%s (%s)=> " (ns-name *ns*) title))))


(defn repl-init []
  (println help-message))


(defn ex-chain [e]
  (take-while some?
    (iterate ex-cause e)))


(defn ex-print
  [^Throwable e]
  (let [indent "  "]
    (doseq [e (ex-chain e)]
      (println (-> e
                   class
                   .getCanonicalName))
      (print indent)
      (println (ex-message e))
      (when-let [data (ex-data e)]
        (print indent)
        (clojure.pprint/pprint data)))))


(defn make-repl-caught [& [options]]

  (let [stack-trace?
        (get options :stack-trace? false)]

    (fn [^Throwable e]

      (println)

      (let [indent "  "]

        (doseq [e (ex-chain e)]
          (println (-> e
                       class
                       .getCanonicalName))
          (print indent)
          (println (ex-message e))
          (when-let [data (ex-data e)]
            (print indent)
            (clojure.pprint/pprint data))))

      (when stack-trace?
        (doseq [el (.getStackTrace e)]
          (println (trace/print-trace-element el)))))))


(defn make-repl-defaults [& [options]]
  {:init   repl-init
   :eval   repl-eval
   :print  repl-print
   :read   repl-read
   :prompt (make-repl-prompt options)
   :caught (make-repl-caught options)})


(defmacro debug [& [options]]
  `(with-globals
     (apply main/repl
            (apply concat (make-repl-defaults ~options)))))


(defn debug-reader [form]
  (let [options (meta form)]
    `(do (debug ~options) ~form)))


#_
(let [a 1]
  (debug {:title "test1"})
  (+ a 2))

#_
(let [a 1]
  #bg/debug ^{:title "test2" :stack-trace? true}
  (+ a 2))

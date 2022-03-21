
(def LOCALS '{foo 111 bar 222 baz 333})


(defn foo [-form]
  (walk/prewalk

   (fn [form]

     (cond

       (and (coll? form)
            (= 'let (first form)))

       (let [[_ binding & body]
             form

             syms
             (take-nth 2 binding)

             vals
             (take-nth 2 (rest binding))

             syms*
             (for [sym syms]
               (with-meta sym {:skip true}))

             binding*
             (vec (interleave syms* vals))]

         (list* 'let binding* body))

       (symbol? form)
       (if (-> form meta :skip)
         form
         (get LOCALS form form))

       :else
       form))

   -form))


(defmacro get-locals []
  `(hash-map
    ~@(reduce
       (fn [result sym]
         (conj result (list 'quote sym) sym))
       []
       (keys &env))))

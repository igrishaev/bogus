(ns bogus.core
  (:require
   [clojure.string :as str]
   [clojure.pprint :as pprint]
   [clojure.inspector :as inspector]
   [clojure.stacktrace :as trace])
  (:import
   (javax.swing.tree TreeModel)
   (javax.swing JFrame
                JTree
                JLabel
                JTextArea
                JScrollPane
                JButton)
   (java.awt.event ActionListener
                   WindowListener
                   KeyListener
                   KeyEvent)))


(def br "\r\n")


(defn throwable? [e]
  (instance? Throwable e))


(defmacro with-locals [[bind] & body]
  `(let [~bind ~(into {} (for [sym (keys &env)]
                           [(list 'quote sym) sym]))]
     ~@body))


(defn wrap-do [input]
  (format "(do %s)" input))


(def ^:dynamic *locals* nil)


(defn eval+ [the-ns locals form]

  (binding [*locals* locals
            *ns* the-ns]

    (eval `(let ~(reduce
                  (fn [result sym]
                    (conj result sym `(get *locals* '~sym)))
                  []
                  (keys locals))
             ~form))))


(defn show-gui [the-ns locals & [options]]

  (let [latch
        (promise)

        {:keys [form]}
        options

        lab-input
        (new JLabel "Input. Eval all forms or selected only. Press Enter + Shift/Control to eval")

        lab-output
        (new JLabel "Output")

        lab-locals
        (new JLabel "Locals")

        frame
        (new JFrame (format "Bogus debugger: %s" (ns-name the-ns)))

        area-input
        (new JTextArea)

        area-output
        (new JTextArea)

        area-locals
        (new JTextArea)

        scroll-input
        (new JScrollPane area-input)

        scroll-output
        (new JScrollPane area-output)

        scroll-locals
        (new JScrollPane area-locals)

        fn-close
        (fn []
          (deliver latch true))

        fn-eval
        (fn []

          (let [input
                (str/trim
                 (or (.getSelectedText area-input)
                     (.getText area-input)))]

            (when-not (str/blank? input)
              (let [result
                    (try
                      (let [form
                            (read-string (wrap-do input))]
                        (eval+ the-ns locals form))
                      (catch Throwable e
                        e))

                    output
                    (with-out-str
                      (if (throwable? result)
                        (trace/print-stack-trace result)
                        (pprint/pprint result)))]

                (.setText area-output output)))))

        frame-listener
        (reify WindowListener

          (windowActivated [this e])

          (windowClosed [this e])

          (windowClosing [this e]
            (fn-close))

          (windowDeactivated [this e])

          (windowDeiconified [this e])

          (windowIconified [this e])

          (windowOpened [this e]))]

    (.addWindowListener frame frame-listener)

    (.addKeyListener area-input
                     (proxy [KeyListener] []
                       (keyReleased [e])
                       (keyTyped [e])
                       (keyPressed [^KeyEvent e]
                         (when (and
                                (or (.isShiftDown e)
                                    (.isControlDown e))
                                (= (.getKeyCode e) KeyEvent/VK_ENTER))
                           (fn-eval)))))

    (when form
      (.setText area-input
                (with-out-str
                  (pprint/pprint form))))

    (.setBounds scroll-input  20  25 460 155)
    (.setBounds scroll-output 20 205 460 175)

    (.setEditable area-output false)
    (.setEditable area-locals false)

    (.setLabelFor lab-input area-input)
    (.setLabelFor lab-output area-output)
    (.setLabelFor lab-locals area-locals)

    (.setBounds lab-input  20   5 500 20)
    (.setBounds lab-output 20 185 100 20)
    (.setBounds lab-locals 20 385 100 20)

    (.add frame lab-input)
    (.add frame lab-output)
    (.add frame lab-locals)

    (.add frame scroll-input)
    (.add frame scroll-output)

    (let [scroll-locals
          (new JScrollPane (new JTree ^TreeModel (inspector/tree-model locals)))]
      (.setBounds scroll-locals 20 405 460 350)
      (.add frame scroll-locals))

    (.setSize frame 500 800)
    (.setLayout frame nil)
    (.setVisible frame true)

    ;; (.toFront frame)
    ;; (.requestFocus frame)
    ;; (.setState frame JFrame/NORMAL)

    ;; trigger the focus
    (.setAlwaysOnTop frame true)
    (.setAlwaysOnTop frame false)

    latch))

#_
(show-gui *ns* {'foo 42} {:form '(+ 1 2 3)})


(defmacro debug [& [{:as options
                     :keys [form]}]]
  (let [the-ns *ns*

        options
        (dissoc options :form)]

    `(with-locals [locals#]
       @(show-gui ~the-ns locals#
                  (assoc ~options :form (quote ~form))))))


(defn debug-reader [form]
  (let [options
        (-> (meta form)
            (assoc :form form))

        {when-clause :when}
        options]

    (if when-clause

      `(do
         (when ~when-clause
           (debug ~options))
         ~form)

      `(do (debug ~options) ~form))))


#_
(do

  (let [a 1
        b 2]
    (let [c 3]
      (with-locals [locals]
        locals)))


  (let [xx 22
        yy 33
        zz (list 1 2 3)]
    (debug))


  (eval+ *ns* {'list (list 1 2 3)} 'list)

  ;; prevent syntax error for lein local profile

  ;; (doseq [x (range 9)]
  ;;   #bg/debug ^{:when (= x 3)}
  ;;   (println x))

  ;; (loop [items [1 2 3 4 5]]
  ;;   #bg/debug
  ;;   (when-let [item (first items)]
  ;;     (println item)
  ;;     (recur (rest items))))

  )

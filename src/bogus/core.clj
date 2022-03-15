(ns bogus.core
  (:require
   [clojure.string :as str]
   [clojure.pprint :as pprint]
   [clojure.inspector :as inspector]
   [clojure.stacktrace :as trace])
  (:import
   (javax.swing JFrame
                JLabel
                JDialog
                JTextArea
                JPanel
                JScrollPane
                JButton)
   (java.awt.event ActionListener
                   WindowListener)))


(defn throwable? [e]
  (instance? Throwable e))


(defn get-old-sym [sym]
  (symbol (format "__OLD_%s__" sym)))


(defn globalize [locals]

  (doseq [[sym value] locals]

    (let [sym-old
          (get-old-sym sym)]

      (when-let [sym-var (resolve sym)]
        (intern *ns* sym-old @sym-var))

      (intern *ns* sym value))))


(defn de-globalize [locals]

  (doseq [[sym value] locals]

    (let [sym-old
          (get-old-sym sym)]

      (ns-unmap *ns* sym)

      (when-let [sym-var (resolve sym-old)]
        (intern *ns* sym @sym-var)
        (ns-unmap *ns* sym-old)))))


(defmacro with-globalize [locals & body]
  `(do
     (globalize ~locals)
     (try
       ~@body
       (finally
         (de-globalize ~locals)))))


(defmacro with-locals [[bind] & body]
  `(let [~bind ~(into {} (for [sym (keys &env)]
                           [(list 'quote sym) sym]))]

     ~@body))


(defn show-gui [locals name-space]

  (let [latch
        (promise)

        lab-input
        (new JLabel "Input")

        lab-output
        (new JLabel "Output")

        lab-log
        (new JLabel "Log")

        frame
        (new JFrame)

        btn-eval
        (new JButton "Eval")

        btn-locals
        (new JButton "Locals")

        btn-inspect
        (new JButton "Inspect")

        area-input
        (new JTextArea)

        area-output
        (new JTextArea)

        area-log
        (new JTextArea)

        scroll-input
        (new JScrollPane area-input)

        scroll-output
        (new JScrollPane area-output)

        scroll-log
        (new JScrollPane area-log)

        fn-close
        (fn []
          (deliver latch true))

        fn-eval
        (fn []

          (let [input
                (.getText area-input)]

            (when-not (str/blank? input)
              (let [result
                    (binding [*ns* name-space]
                      (with-globalize locals
                        (try
                          (eval (read-string input))
                          (catch Throwable e
                            e))))

                    output
                    (with-out-str
                      (if (throwable? result)
                        (trace/print-stack-trace result)
                        (pprint/pprint result)))]

                (.setText area-output output)

                (.append area-log input)
                (.append area-log "\r\n")
                (.append area-log output)
                (.append area-log "\r\n")

                (.setCaretPosition area-log (.. area-log getDocument getLength))))))

        fn-locals
        (fn []
          (let [output
                (with-out-str
                  (pprint/pprint locals))]

            (.setText area-output output)))

        fn-inspect
        (fn []
          (inspector/inspect-tree locals))

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

    (.addActionListener btn-eval
                        (reify ActionListener
                          (actionPerformed [this e]
                            (fn-eval))))

    (.addActionListener btn-locals
                        (reify ActionListener
                          (actionPerformed [this e]
                            (fn-locals))))

    (.addActionListener btn-inspect
                        (reify ActionListener
                          (actionPerformed [this e]
                            (fn-inspect))))

    (.setBounds btn-eval     20 130 100 50)
    (.setBounds btn-locals  130 130 100 50)
    (.setBounds btn-inspect 240 130 100 50)

    (.setBounds scroll-input  20  25 460 100)
    (.setBounds scroll-output 20 205 460 175)
    (.setBounds scroll-log    20 405 460 350)

    (.setEditable area-output false)
    (.setEditable area-log false)

    (.setLabelFor lab-input area-input)
    (.setLabelFor lab-output area-output)
    (.setLabelFor lab-log area-log)

    (.setBounds lab-input  20   5 100 20)
    (.setBounds lab-output 20 185 100 20)
    (.setBounds lab-log    20 385 100 20)

    (.add frame lab-input)
    (.add frame lab-output)
    (.add frame lab-log)

    (.add frame btn-eval)
    (.add frame btn-locals)
    (.add frame btn-inspect)

    (.add frame scroll-input)
    (.add frame scroll-output)
    (.add frame scroll-log)

    (.setSize frame 500 800)
    (.setLayout frame nil)
    (.setVisible frame true)

    latch))


#_
(show-gui {} *ns*)


(defmacro debug [& [options]]
  (let [the-ns *ns*]
    `(with-locals [locals#]
       @(show-gui locals# ~the-ns))))


(defn debug-reader [form]
  (let [options (meta form)]
    `(do (debug ~options) ~form)))

#_
(let [a 1
      b 2]
  (let [c 3]
    (with-locals [locals]
      (println locals))))

#_
(let [xx 22
      yy 33]
  (debug))

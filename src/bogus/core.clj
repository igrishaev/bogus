(ns bogus.core
  (:require
   [clojure.string :as str]
   [clojure.pprint :as pprint]
   [clojure.inspector :as inspector]
   [clojure.stacktrace :as trace])
  (:import
   (javax.swing JFrame
                JLabel
                JTextArea
                JScrollPane
                JButton)
   (java.awt.event ActionListener
                   WindowListener)))


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



(defn show-gui [the-ns locals]

  (let [latch
        (promise)

        lab-input
        (new JLabel "Input (eval all forms or selected)")

        lab-output
        (new JLabel "Output")

        lab-log
        (new JLabel "Log")

        frame
        (new JFrame (format "Bogus debugger: %s" (ns-name the-ns)))

        btn-eval
        (new JButton "Eval")

        btn-locals
        (new JButton "Locals")

        btn-inspect
        (new JButton "Inspect")

        btn-clear
        (new JButton "Clear")

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

        fn-clear
        (fn []
          (.setText area-input "")
          (.setText area-output "")
          (.setText area-log ""))

        fn-eval
        (fn []

          (let [input
                (or (.getSelectedText area-input)
                    (.getText area-input))]

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

                (.setText area-output output)

                (.append area-log input)
                (.append area-log br)
                (.append area-log output)
                (.append area-log br)

                (.setCaretPosition area-log (.. area-log getDocument getLength))))))

        fn-locals
        (fn []
          (let [output
                (with-out-str
                  (pprint/pprint locals))]

            (.setText area-output ";; locals")
            (.append area-output br)
            (.append area-output br)
            (.append area-output output)))

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

          (windowOpened [this e]
            (fn-locals)))]

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

    (.addActionListener btn-clear
                        (reify ActionListener
                          (actionPerformed [this e]
                            (fn-clear))))

    (.setBounds btn-eval     20 130 100 50)
    (.setBounds btn-locals  130 130 100 50)
    (.setBounds btn-inspect 240 130 100 50)
    (.setBounds btn-clear   350 130 100 50)

    (.setBounds scroll-input  20  25 460 100)
    (.setBounds scroll-output 20 205 460 175)
    (.setBounds scroll-log    20 405 460 350)

    (.setEditable area-output false)
    (.setEditable area-log false)

    (.setLabelFor lab-input area-input)
    (.setLabelFor lab-output area-output)
    (.setLabelFor lab-log area-log)

    (.setBounds lab-input  20   5 300 20)
    (.setBounds lab-output 20 185 100 20)
    (.setBounds lab-log    20 385 100 20)

    (.add frame lab-input)
    (.add frame lab-output)
    (.add frame lab-log)

    (.add frame btn-eval)
    (.add frame btn-locals)
    (.add frame btn-inspect)
    (.add frame btn-clear)

    (.add frame scroll-input)
    (.add frame scroll-output)
    (.add frame scroll-log)

    (.setSize frame 500 800)
    (.setLayout frame nil)
    (.setVisible frame true)

    latch))

#_
(show-gui *ns* {'foo 42})


(defmacro debug [& [options]]
  (let [the-ns *ns*]
    `(with-locals [locals#]
       @(show-gui ~the-ns locals#))))


(defn debug-reader [form]
  (let [options (meta form)]
    `(do (debug ~options) ~form)))


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

  )

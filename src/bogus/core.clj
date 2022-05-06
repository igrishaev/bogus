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

(def HELP "
;; Press Enter + Shift/Control to execute the last sexp or selected text.
;; Expressions must be separated with a blank line.
")


(defn throwable? [e]
  (instance? Throwable e))


(defmacro with-locals [[bind] & body]
  `(let [~bind ~(into {} (for [sym (keys &env)]
                           [(list 'quote sym) sym]))]
     ~@body))


(defn prepend-text [text prefix]
  (with-out-str
    (doseq [line (str/split-lines text)]
      (print prefix)
      (println line))))


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


(defn get-text-before-carret [^JTextArea text-area]
  (let [offset (-> text-area .getCaret .getMark)]
    (.getText text-area 0 offset)))


(defn get-last-sexp-from-text [text]

  (let [chunks
        (str/split text #"\n\s*\n")

        pred
        (fn [line]
          (-> line str/trim str/blank?))]

    (last (remove pred chunks))))


(defn get-last-sexp [^JTextArea text-area]
  (-> text-area
      (get-text-before-carret)
      (get-last-sexp-from-text)))


(defn ctrl|shift+enter? [^KeyEvent e]
  (and
   (= (.getKeyCode e) KeyEvent/VK_ENTER)
   (or (.isShiftDown e)
       (.isControlDown e))))

(defn meta+j? [^KeyEvent e]
  (and
   (= (.getKeyCode e) 74) ;; j
   (.isMetaDown e)))


(defn scroll-down [^JTextArea text-area]
  (.. text-area getCaret (setDot Integer/MAX_VALUE)))


(defn show-gui [the-ns locals & [options]]

  (let [latch
        (promise)

        {:keys [form]}
        options

        lab-input
        (new JLabel "Intput")

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

        fn-window-opened
        (fn []
          (.requestFocusInWindow area-input)
          (scroll-down area-input))

        fn-init-input
        (fn [form]
          (.append area-input
                   (with-out-str
                     (pprint/pprint form))))

        fn-eval
        (fn []

          (let [input
                (or (.getSelectedText area-input)
                    (get-last-sexp area-input))]

            (when-not (str/blank? input)
              (let [result
                    (try
                      (let [form
                            (read-string (wrap-do (str/trim input)))]

                        (.setText area-output "")
                        (.append area-output
                                 (prepend-text
                                  (with-out-str
                                    (pprint/pprint form))
                                  "> "))
                        (.append area-output br)
                        (scroll-down area-output)

                        (eval+ the-ns locals form))
                      (catch Throwable e
                        e))

                    output
                    (with-out-str
                      (if (throwable? result)
                        (trace/print-stack-trace result)
                        (pprint/pprint result)))]

                (.append area-output output)))))

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
            (fn-window-opened)))]

    (.addWindowListener frame frame-listener)

    ;; TODO: add Alt+Q to quit

    (.addKeyListener area-input
                     (proxy [KeyListener] []
                       (keyReleased [e])
                       (keyTyped [e])
                       (keyPressed [^KeyEvent e]
                         (when (or
                                (ctrl|shift+enter? e)
                                (meta+j? e))
                           (fn-eval)))))

    ;;
    ;; Prepare input area
    (.append area-input (str/trim HELP))
    (.append area-input br)
    (.append area-input br)

    (when form
      (fn-init-input form))
    ;; end
    ;;

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

    (.setState frame JFrame/NORMAL)
    (.setLocationRelativeTo frame nil)
    (.setVisible frame true)

    (.setAlwaysOnTop frame true)
    (.setAlwaysOnTop frame false)

    (.setFocusable frame true)
    (.requestFocus frame)

    ;; (.toFront frame)

    latch))

#_
(show-gui *ns* {'foo 42}
          {:form '{:a "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                   :b "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                   :c "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                   :d "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                   :e "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                   :f "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                   :g "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                   :h "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                   :i "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                   :j "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                   :k "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                   :l "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                   :m "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}})


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

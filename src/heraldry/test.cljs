(ns heraldry.test
  {:dev/always true}
  (:require
   [clansi.core :as cla]
   [cljs.test :as ct]
   [lambdaisland.deep-diff2 :as ddiff]
   [shadow.test :as st]
   [shadow.test.env :as env]))

(defmethod ct/report [::reporter :begin-test-ns] [m]
  ((get-method ct/report [:cljs.test/default :begin-test-ns]) m))

(defmethod ct/report [::reporter :pass] [m]
  ((get-method ct/report [:cljs.test/default :pass]) m))

(def diff-printer
  (ddiff/printer
   {:sort-keys false
    :width 25
    :map-delimiter ""
    :color-scheme
    (merge {;; syntax elements
            :delimiter [:yellow]
            :tag [:magenta]

            ;; primitive values
            :nil [:black]
            :boolean [:blue]
            :number [:cyan]
            :string [:cyan]
            :character [:magenta]
            :keyword [:blue]
            :symbol nil

            ;; special types
            :function-symbol [:blue]
            :class-delimiter [:blue]
            :class-name [:blue]}
           #:lambdaisland.deep-diff2.printer-impl {:deletion [:red]
                                                   :insertion [:green]
                                                   :other [:yellow]})}))

(defmethod ct/report [::reporter :fail] [{:keys [message expected actual] :as m}]
  (ct/inc-report-counter! :fail)

  (println "\nFAIL in" (ct/testing-vars-str m))
  (when (seq (:testing-contexts (ct/get-current-env)))
    (println (ct/testing-contexts-str)))
  (when message
    (println message))

  (if (-> expected first (= '=))
    (let [[_ actual-value expected-value] (second actual)]
      (print (with-out-str
               (ddiff/pretty-print
                (ddiff/diff expected-value actual-value)
                diff-printer))))
    (do
      (println "expected:" (cla/style expected :green))
      (println "  actual:" (cla/style actual :red)))))

(defmethod ct/report [::reporter :error] [m]
  ((get-method ct/report [:cljs.test/default :error]) m))

(defmethod ct/report [::reporter :summary] [{:keys [test pass fail error]}]
  (let [total (+ pass fail error)]
    (println (cla/style (str "\nRan " test " tests with " total " assertions.")
                        :cyan))
    (println (str
              (cla/style (str pass " passes,")
                         (if (= pass total)
                           :green
                           :yellow))
              " "
              (cla/style (str fail " failures,")
                         (if (pos? fail)
                           :red
                           :green))
              " "
              (cla/style (str error " errors.")
                         (if (pos? error)
                           :red
                           :green))))))

(defn ^:dev/after-load reset-test-data! []
  (-> (env/get-test-data)
      (env/reset-test-data!)))

(defn main []
  (reset-test-data!)
  (st/run-all-tests (ct/empty-env ::reporter) nil))

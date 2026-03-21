(ns heraldicon.reader.blazonry.transform.line
  (:require
   [clojure.string :as str]
   [heraldicon.reader.blazonry.transform.fimbriation :refer [add-fimbriation]]
   [heraldicon.reader.blazonry.transform.shared :refer [ast->hdn transform-first transform-all]]))

(defmethod ast->hdn :line-type [[_ node]]
  (let [raw-type (-> node
                     first
                     name
                     str/lower-case
                     keyword)]
    (get {:rayonny :rayonny-flaming}
         raw-type raw-type)))

(defmethod ast->hdn :line-style-repetitions [[_ node]]
  (let [n (js/parseFloat (second node))
        clamped (max 1 (min 25 n))]
    (/ 100 clamped)))

(defmethod ast->hdn :line [[_ & nodes]]
  (let [line-type (transform-first #{:line-type} nodes)
        line-width (transform-first #{:line-style-repetitions} nodes)]
    (-> nil
        (cond->
          line-type (assoc :type line-type)
          line-width (assoc :width line-width))
        (add-fimbriation nodes))))

(defn add-lines [hdn nodes]
  (let [[line
         opposite-line
         extra-line] (transform-all #{:line} nodes)]
    (cond-> hdn
      line (assoc :line line)
      opposite-line (assoc :opposite-line opposite-line)
      extra-line (assoc :extra-line extra-line))))

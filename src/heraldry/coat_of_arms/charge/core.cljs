(ns heraldry.coat-of-arms.charge.core
  (:require [heraldry.coat-of-arms.charge.other :as charge-other]
            [heraldry.coat-of-arms.charge.type.annulet :as annulet]
            [heraldry.coat-of-arms.charge.type.billet :as billet]
            [heraldry.coat-of-arms.charge.type.crescent :as crescent]
            [heraldry.coat-of-arms.charge.type.escutcheon :as escutcheon]
            [heraldry.coat-of-arms.charge.type.fusil :as fusil]
            [heraldry.coat-of-arms.charge.type.lozenge :as lozenge]
            [heraldry.coat-of-arms.charge.type.mascle :as mascle]
            [heraldry.coat-of-arms.charge.type.roundel :as roundel]
            [heraldry.coat-of-arms.charge.type.rustre :as rustre]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.charge.options :as charge-options]
            [heraldry.frontend.util :as util]
            [clojure.string :as s]))

(def charges
  [#'roundel/render
   #'annulet/render
   #'billet/render
   #'escutcheon/render
   #'lozenge/render
   #'fusil/render
   #'mascle/render
   #'rustre/render
   #'crescent/render])

(def kinds-function-map
  (->> charges
       (map (fn [function]
              [(-> function meta :value) function]))
       (into {})))

(def choices
  (->> charges
       (map (fn [function]
              [(-> function meta :display-name) (-> function meta :value)]))))

(defn render [{:keys [type variant data] :as charge} parent environment context]
  (let [function (get kinds-function-map type)]
    (if (and function
             (not data)
             (not variant))
      [function charge parent environment context]
      [charge-other/render charge parent environment context])))

(defn title [charge]
  (s/join " " [(-> charge :type util/translate-cap-first)
               (when (-> charge :attitude (not= :none))
                 (-> charge :attitude util/translate))
               (when (-> charge :facing #{:none :to-dexter} not)
                 (-> charge :facing util/translate))]))

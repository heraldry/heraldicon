(ns or.coad.field
  (:require
   [or.coad.division :as division]))

(defn render [field context]
  (let [division (:division field)
        ordinaries (:ordinaries field)]
    [:<>
     [division/render division]
     #_(for [[idx ordinary] (map-indexed vector ordinaries)]
         ^{:key idx} [ordinary/render ordinary])]))

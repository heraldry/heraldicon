(ns or.coad.field-content
  (:require [or.coad.division :as division]))

(defn render [content field]
  (let [division   (:division content)
        ordinaries (:ordinaries content)]
    [:<>
     [division/render division field render]
     #_(for [[idx ordinary] (map-indexed vector ordinaries)]
         ^{:key idx} [ordinary/render ordinary])]))

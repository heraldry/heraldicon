(ns or.coad.field-content
  (:require [or.coad.division :as division]
            [or.coad.svg :as svg]
            [or.coad.tincture :as tincture]))

(defn render [content field]
  (let [division   (:division content)
        ordinaries (:ordinaries content)
        tincture   (:tincture content)
        [mx my]    (svg/center (:shape field))]
    [:<>
     (cond
       tincture [:path {:d         (:shape field)
                        :transform (str "translate(" mx "," my ") scale(1.1, 1.1) translate(" (- mx) "," (- my) ")")
                        :fill      (get tincture/tinctures tincture)}]
       division [division/render division field render])
     #_(for [[idx ordinary] (map-indexed vector ordinaries)]
         ^{:key idx} [ordinary/render ordinary])]))

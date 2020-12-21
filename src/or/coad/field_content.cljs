(ns or.coad.field-content
  (:require [or.coad.division :as division]
            [or.coad.ordinary :as ordinary]
            [or.coad.tincture :as tincture]))

(defn render [content field]
  (let [division (:division content)
        ordinaries (:ordinaries content)
        tincture (get-in content [:content :tincture])]
    [:<>
     (cond
       tincture [:path {:d (:shape field)
                        :fill (get tincture/tinctures tincture)
                        :stroke (get tincture/tinctures tincture)}]
       division [division/render division field render])
     (for [[idx ordinary] (map-indexed vector ordinaries)]
       ^{:key idx} [ordinary/render ordinary field render])]))

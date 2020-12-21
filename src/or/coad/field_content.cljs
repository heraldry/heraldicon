(ns or.coad.field-content
  (:require [or.coad.division :as division]
            [or.coad.hatching :as hatching]
            [or.coad.ordinary :as ordinary]
            [or.coad.tincture :as tincture]))

(defn render [content field options]
  (let [division   (:division content)
        ordinaries (:ordinaries content)
        tincture   (get-in  content [:content :tincture])]
    [:<>
     (cond
       tincture (let [fill (case (:mode options)
                             :colours  (get tincture/tinctures tincture)
                             :hatching (hatching/get-for tincture))]
                  [:path {:d      (:shape field)
                          :fill   fill
                          :stroke fill}])
       division [division/render division field render options])
     (for [[idx ordinary] (map-indexed vector ordinaries)]
       ^{:key idx} [ordinary/render ordinary field render options])]))

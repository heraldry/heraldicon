(ns or.coad.field-content
  (:require [or.coad.division :as division]
            [or.coad.ordinary :as ordinary]
            [or.coad.tincture :as tincture]))

(defn render [content field options]
  (let [division (:division content)
        ordinaries (:ordinaries content)
        tincture (get-in content [:content :tincture])]
    [:<>
     (cond
       tincture [:<>
                 [:path {:d (:shape field)
                         :fill (get tincture/tinctures tincture)
                         :stroke (get tincture/tinctures tincture)}]
                 (when (:outline? options)
                   [:path.outline {:d (:shape field)
                                   :class (when (get-in field [:meta :ordinary?])
                                            "ordinary")}])]
       division [division/render division field render options])
     (for [[idx ordinary] (map-indexed vector ordinaries)]
       ^{:key idx} [ordinary/render ordinary field render options])]))

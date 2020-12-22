(ns or.coad.field
  (:require [or.coad.charge :as charge]
            [or.coad.division :as division]
            [or.coad.ordinary :as ordinary]
            [or.coad.tincture :as tincture]))

(defn render [{:keys [division ordinaries charges] :as field} environment options]
  (let [tincture (get-in field [:content :tincture])]
    [:<>
     (cond
       tincture (let [fill (tincture/pick tincture options)]
                  [:rect {:x      -25
                          :y      -25
                          :width  150
                          :height 250
                          :fill   fill
                          :stroke fill}])
       division [division/render division environment render options])
     (for [[idx ordinary] (map-indexed vector ordinaries)]
       ^{:key idx} [ordinary/render ordinary environment render options])
     (for [[idx charge] (map-indexed vector charges)]
       ^{:key idx} [charge/render charge environment render options])]))

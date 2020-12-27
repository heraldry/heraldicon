(ns or.coad.field
  (:require [or.coad.charge :as charge]
            [or.coad.division :as division]
            [or.coad.ordinary :as ordinary]
            [or.coad.tincture :as tincture]
            [re-frame.core :as rf]))

(defn render [{:keys [division ordinaries charges ui] :as field} environment options & {:keys [db-path]}]
  (let [tincture (get-in field [:content :tincture])]
    [:g {:on-click (fn [event]
                     (rf/dispatch [:select-component db-path])
                     (.stopPropagation event))
         :style {:pointer-events "visiblePainted"
                 :cursor "pointer"}}
     (cond
       tincture (let [fill (tincture/pick tincture options)]
                  [:rect {:x -100
                          :y -100
                          :width 250
                          :height 350
                          :fill fill
                          :stroke fill}])
       division [division/render division environment render options :db-path (conj db-path :division)])
     (for [[idx ordinary] (map-indexed vector ordinaries)]
       ^{:key idx} [ordinary/render ordinary field environment render options :db-path (conj db-path :ordinaries idx)])
     (for [[idx charge] (map-indexed vector charges)]
       ^{:key idx} [charge/render charge field environment render options :db-path (conj db-path :charges idx)])
     (when (-> ui :selected?)
       [:path {:d (:shape environment)
               :style {:opacity 0.25}
               :fill "url(#selected)"}])]))

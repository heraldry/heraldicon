(ns or.coad.field
  (:require [or.coad.charge :as charge]
            [or.coad.division :as division]
            [or.coad.ordinary :as ordinary]
            [or.coad.tincture :as tincture]
            [re-frame.core :as rf]))

(defn render [{:keys [division components] :as field} environment options & {:keys [db-path]}]
  (let [tincture  (get-in field [:content :tincture])
        selected? @(rf/subscribe [:component-selected? db-path])]
    [:g {:on-click (fn [event]
                     (rf/dispatch [:select-component db-path])
                     (.stopPropagation event))
         :style    {:pointer-events "visiblePainted"
                    :cursor         "pointer"}}
     (cond
       tincture (let [fill (tincture/pick tincture options)]
                  [:rect {:x      -500
                          :y      -500
                          :width  1100
                          :height 1100
                          :fill   fill
                          :stroke fill}])
       division [division/render division environment render options :db-path (conj db-path :division)])
     (when selected?
       [:path {:d     (:shape environment)
               :style {:opacity 0.25}
               :fill  "url(#selected)"}])
     (for [[idx element] (map-indexed vector components)]
       (if (-> element :component (= :ordinary))
         ^{:key idx} [ordinary/render element field environment render options :db-path (conj db-path :components idx)]
         ^{:key idx} [charge/render element field environment render options :db-path (conj db-path :components idx)]))]))

(ns heraldry.coat-of-arms.field
  (:require [heraldry.coat-of-arms.charge :as charge]
            [heraldry.coat-of-arms.division.core :as division]
            [heraldry.coat-of-arms.ordinary :as ordinary]
            [heraldry.coat-of-arms.tincture :as tincture]))

(defn render [{:keys [division components] :as field} environment
              {:keys [db-path render-options fn-component-selected? fn-select-component svg-export?] :as context}]
  (let [tincture (get-in field [:content :tincture])
        selected? (when fn-component-selected?
                    (fn-component-selected? db-path))
        context (-> context
                    (assoc :render-field render))]
    [:g {:on-click (when fn-select-component
                     (fn [event]
                       (fn-select-component db-path)
                       (.stopPropagation event)))
         :style (when (not svg-export?)
                  {:pointer-events "visiblePainted"
                   :cursor "pointer"})}
     (cond
       tincture (let [fill (tincture/pick tincture render-options)]
                  [:rect {:x -500
                          :y -500
                          :width 1100
                          :height 1100
                          :fill fill
                          :stroke fill}])
       division [division/render division environment (-> context
                                                          (update :db-path conj :division))])
     (when selected?
       [:path {:d (:shape environment)
               :style {:opacity 0.25}
               :fill "url(#selected)"}])
     (for [[idx element] (map-indexed vector components)]
       (if (-> element :component (= :ordinary))
         ^{:key idx} [ordinary/render element field environment (-> context
                                                                    (update :db-path conj :components idx))]
         ^{:key idx} [charge/render element field environment (-> context
                                                                  (update :db-path conj :components idx))]))]))

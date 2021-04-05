(ns heraldry.coat-of-arms.field
  (:require [heraldry.coat-of-arms.charge.core :as charge]
            [heraldry.coat-of-arms.division.core :as division]
            [heraldry.coat-of-arms.ordinary.core :as ordinary]
            [heraldry.coat-of-arms.tincture.core :as tincture]))

(defn render [{:keys [components] :as field} environment
              {:keys
               [db-path render-options fn-component-selected?
                fn-select-component svg-export? transform] :as context}]
  (let [type (:type field)
        selected? (when fn-component-selected?
                    (fn-component-selected? db-path))
        context (-> context
                    (assoc :render-field render))]
    [:<>
     [:g {:on-click (when fn-select-component
                      (fn [event]
                        (fn-select-component db-path)
                        (.stopPropagation event)))
          :style (when (not svg-export?)
                   {:pointer-events "visiblePainted"
                    :cursor "pointer"})
          :transform transform}
      (case type
        :plain (let [fill (tincture/pick (:tincture field) render-options)]
                 [:rect {:x -500
                         :y -500
                         :width 1100
                         :height 1100
                         :fill fill
                         :stroke fill}])
        [division/render field environment context])
      (for [[idx element] (map-indexed vector components)]
        (if (-> element :component (= :ordinary))
          ^{:key idx} [ordinary/render element field environment (-> context
                                                                     (update :db-path conj :components idx))]
          ^{:key idx} [charge/render element field environment (-> context
                                                                   (update :db-path conj :components idx))]))]
     (when selected?
       [:path {:d (:shape environment)
               :style {:opacity 0.25}
               :fill "url(#selected)"}])]))

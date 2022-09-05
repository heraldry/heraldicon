(ns heraldicon.heraldry.semy
  (:require
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.util.uid :as uid]))

(derive :heraldry/semy :heraldry.options/root)

(defmethod interface/options-subscriptions :heraldry/semy [_context]
  #{})

(defmethod interface/options :heraldry/semy [_context]
  (-> {:layout {:num-fields-x {:type :option.type/range
                               :min 1
                               :max 20
                               :default 6
                               :integer? true
                               :ui/label :string.option/number-of-columns}
                :num-fields-y {:type :option.type/range
                               :min 1
                               :max 20
                               :default 6
                               :integer? true
                               :ui/label :string.option/number-of-rows}
                :offset-x {:type :option.type/range
                           :min -1
                           :max 1
                           :default 0
                           :ui/label :string.option/offset-x
                           :ui/step 0.01}
                :offset-y {:type :option.type/range
                           :min -1
                           :max 1
                           :default 0
                           :ui/label :string.option/offset-y
                           :ui/step 0.01}
                :stretch-x {:type :option.type/range
                            :min 0.5
                            :max 2
                            :default 1
                            :ui/label :string.option/stretch-x
                            :ui/step 0.01}
                :stretch-y {:type :option.type/range
                            :min 0.5
                            :max 2
                            :default 1
                            :ui/label :string.option/stretch-y
                            :ui/step 0.01}
                :rotation {:type :option.type/range
                           :min -90
                           :max 90
                           :default 0
                           :ui/label :string.option/rotation}
                :ui/label :string.option/layout
                :ui/element :ui.element/semy-layout}

       :rectangular? {:type :option.type/boolean
                      :default false
                      :ui/label :string.option/rectangular?}
       :manual-blazon options/manual-blazon}))

(defn shift-environment [environment point]
  (update environment
          :points (fn [points]
                    (into {}
                          (map (fn [[k v]]
                                 [k (v/add v point)]))
                          points))))

(defmethod interface/properties :heraldry/semy [context]
  (let [{:keys [points width height]} (interface/get-parent-environment context)
        {:keys [top-left]} points
        rectangular? (interface/get-sanitized-data (c/++ context :rectangular?))
        num-fields-x (interface/get-sanitized-data (c/++ context :layout :num-fields-x))
        num-fields-y (interface/get-sanitized-data (c/++ context :layout :num-fields-y))
        raw-num-fields-y (interface/get-raw-data (c/++ context :layout :num-fields-y))
        offset-x (or (interface/get-sanitized-data (c/++ context :layout :offset-x)) 0)
        offset-y (or (interface/get-sanitized-data (c/++ context :layout :offset-y)) 0)
        stretch-x (or (interface/get-sanitized-data (c/++ context :layout :stretch-x)) 0)
        stretch-y (or (interface/get-sanitized-data (c/++ context :layout :stretch-y)) 0)
        rotation (interface/get-sanitized-data (c/++ context :layout :rotation))
        unstretched-part-width (/ width num-fields-x)
        part-width (* unstretched-part-width stretch-x)
        unstretched-part-height (if raw-num-fields-y
                                  (/ height num-fields-y)
                                  part-width)
        part-height (* unstretched-part-height stretch-y)
        middle-x (/ width 2)
        middle-y (/ height 2)
        shift-x (- middle-x
                   (* middle-x stretch-x))
        shift-y (- middle-y
                   (* middle-y stretch-y))]
    {:type :heraldry/semy
     :rectangular? rectangular?
     :top-left (v/add top-left
                      (v/dot (v/Vector. offset-x offset-y)
                             (v/Vector. part-width part-height))
                      (v/Vector. shift-x shift-y))
     :part-width part-width
     :part-height part-height
     :rotation rotation
     :num-fields-x num-fields-x
     :num-fields-y num-fields-y}))

(defmethod interface/render-component :heraldry/semy [context]
  (let [{:keys [rectangular? top-left
                part-width part-height
                rotation]} (interface/get-properties context)
        pattern-id (uid/generate "semy")
        part-width-half (/ part-width 2)
        part-height-half (/ part-height 2)
        charge-environment {:width part-width
                            :height part-height
                            :points {:top-left (v/Vector. (- part-width-half) (- part-height-half))
                                     :top (v/Vector. 0 (- part-height-half))
                                     :top-right (v/Vector. part-width-half (- part-height-half))
                                     :left (v/Vector. (- part-width-half) 0)
                                     :fess (v/Vector. 0 0)
                                     :right (v/Vector. part-width-half 0)
                                     :bottom-left (v/Vector. (- part-width-half) part-height-half)
                                     :bottom (v/Vector. 0 part-height-half)
                                     :bottom-right (v/Vector. part-width-half part-height-half)}}
        charge-context (-> context
                           (c/++ :charge)
                           (c/<< :size-default 50))]
    ;; TODO: reverse transform inside charge
    [:g
     [:defs
      (into [:pattern {:id pattern-id
                       :width part-width
                       :height part-height
                       :x (:x top-left)
                       :y (:y top-left)
                       :pattern-units "userSpaceOnUse"}]
            (map-indexed (fn [idx shift]
                           ^{:key idx}
                           [interface/render-component
                            (-> charge-context
                                (c/add-component-context {:anchor-override shift})
                                (c/set-parent-environment (shift-environment charge-environment shift)))]))
            (cond->
              [v/zero
               (v/Vector. part-width 0)
               (v/Vector. 0 part-height)
               (v/Vector. part-width part-height)]
              (not rectangular?) (conj (v/Vector. part-width-half part-height-half))))]
     [:g {:transform (str "rotate(" (- rotation) ")")}
      [:rect {:x -500
              :y -500
              :width 1100
              :height 1100
              :fill (str "url(#" pattern-id ")")}]]]))

(defmethod interface/blazon-component :heraldry/semy [context]
  (string/str-tr "semy of " (interface/blazon
                             (-> context
                                 (c/++ :charge)
                                 (assoc-in [:blazonry :drop-article?] true)))))

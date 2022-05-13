(ns heraldicon.heraldry.semy
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.charge.interface :as charge.interface]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.util.uid :as uid]))

(defmethod interface/options-subscriptions :heraldry.component/semy [_context]
  #{})

(defmethod interface/options :heraldry.component/semy [_context]
  (-> {:layout {:num-fields-x {:type :range
                               :min 1
                               :max 20
                               :default 6
                               :integer? true
                               :ui {:label :string.option/number-of-columns}}
                :num-fields-y {:type :range
                               :min 1
                               :max 20
                               :default 6
                               :integer? true
                               :ui {:label :string.option/number-of-rows}}
                :offset-x {:type :range
                           :min -1
                           :max 1
                           :default 0
                           :ui {:label :string.option/offset-x
                                :step 0.01}}
                :offset-y {:type :range
                           :min -1
                           :max 1
                           :default 0
                           :ui {:label :string.option/offset-y
                                :step 0.01}}
                :stretch-x {:type :range
                            :min 0.5
                            :max 2
                            :default 1
                            :ui {:label :string.option/stretch-x
                                 :step 0.01}}
                :stretch-y {:type :range
                            :min 0.5
                            :max 2
                            :default 1
                            :ui {:label :string.option/stretch-y
                                 :step 0.01}}
                :rotation {:type :range
                           :min -90
                           :max 90
                           :default 0
                           :ui {:label :string.option/rotation}}
                :ui {:label :string.option/layout
                     :form-type :semy-layout}}

       :rectangular? {:type :boolean
                      :default false
                      :ui {:label :string.option/rectangular?}}
       :manual-blazon options/manual-blazon}))

(defn shift-environment [environment point]
  (-> environment
      (update :points (fn [points]
                        (->> points
                             (map (fn [[k v]]
                                    [k (v/add v point)]))
                             (into {}))))))

(defmethod interface/render-component :heraldry.component/semy [context]
  (let [environment (:environment context)
        points (:points environment)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        rectangular? (interface/get-sanitized-data (c/++ context :rectangular?))
        num-fields-x (interface/get-sanitized-data (c/++ context :layout :num-fields-x))
        num-fields-y (interface/get-sanitized-data (c/++ context :layout :num-fields-y))
        raw-num-fields-y (interface/get-raw-data (c/++ context :layout :num-fields-y))
        offset-x (interface/get-sanitized-data (c/++ context :layout :offset-x))
        offset-y (interface/get-sanitized-data (c/++ context :layout :offset-y))
        stretch-x (interface/get-sanitized-data (c/++ context :layout :stretch-x))
        stretch-y (interface/get-sanitized-data (c/++ context :layout :stretch-y))
        rotation (interface/get-sanitized-data (c/++ context :layout :rotation))
        offset-x (or offset-x 0)
        stretch-x (or stretch-x 1)
        width (- (:x bottom-right)
                 (:x top-left))
        unstretched-part-width (-> width
                                   (/ num-fields-x))
        part-width (-> unstretched-part-width
                       (* stretch-x))
        offset-y (or offset-y 0)
        stretch-y (or stretch-y 1)
        height (- (:y bottom-right)
                  (:y top-left))
        unstretched-part-height (if raw-num-fields-y
                                  (-> height
                                      (/ num-fields-y))
                                  part-width)
        part-height (-> unstretched-part-height
                        (* stretch-y))
        middle-x (/ width 2)
        middle-y (/ height 2)
        shift-x (- middle-x
                   (* middle-x stretch-x))
        shift-y (- middle-y
                   (* middle-y stretch-y))
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
                           (assoc :size-default 50)
                           (assoc :environment charge-environment))]
    [:g
     [:defs
      [:pattern {:id pattern-id
                 :width part-width
                 :height part-height
                 :x (+ (:x top-left)
                       (* part-width offset-x)
                       shift-x)
                 :y (+ (:y top-left)
                       (* part-height offset-y)
                       shift-y)
                 :pattern-units "userSpaceOnUse"}
       (doall
        (for [[idx shift] (map-indexed
                           vector
                           (cond->
                             [v/zero
                              (v/Vector. part-width 0)
                              (v/Vector. 0 part-height)
                              (v/Vector. part-width part-height)]
                             (not rectangular?) (conj (v/Vector. part-width-half part-height-half))))]
          ^{:key idx}
          [charge.interface/render-charge
           (-> charge-context
               (assoc :anchor-override shift)
               (update :environment shift-environment shift))]))]]
     [:g {:transform (str "rotate(" (- rotation) ")")}
      [:rect {:x -500
              :y -500
              :width 1100
              :height 1100
              :fill (str "url(#" pattern-id ")")}]]]))

(defmethod interface/blazon-component :heraldry.component/semy [context]
  (string/str-tr "semy of " (interface/blazon
                             (-> context
                                 (c/++ :charge)
                                 (assoc-in [:blazonry :drop-article?] true)))))

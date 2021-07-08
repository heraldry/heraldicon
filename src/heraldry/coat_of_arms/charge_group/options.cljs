(ns heraldry.coat-of-arms.charge-group.options
  (:require [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.util :as util]))

(def type-choices
  [["Rows" :heraldry.charge-group.type/rows]
   ["Columns" :heraldry.charge-group.type/columns]
   ["Arc" :heraldry.charge-group.type/arc]])

(def type-map
  (util/choices->map type-choices))

(def default-options
  {:type {:type :choice
          :choices type-choices
          :ui {:label "Type"
               :form-type :charge-group-type-select}}
   :origin (-> position/default-options
               (dissoc :alignment))
   :anchor (-> position/anchor-default-options
               (assoc-in [:point :default] :angle)
               (update-in [:point :choices] (fn [choices]
                                              (-> choices
                                                  drop-last
                                                  (conj (last choices))
                                                  vec)))
               (assoc :alignment nil)
               (assoc-in [:angle :min] -180)
               (assoc-in [:angle :max] 180)
               (assoc-in [:angle :default] 0)
               (assoc-in [:ui :label] "Anchor"))
   :spacing {:type :range
             :min 1
             :max 100
             :default 40
             :ui {:label "Spacing"
                  :step 0.1}}
   :stretch {:type :range
             :min 0
             :max 5
             :default 1
             :ui {:label "Stretch"
                  :step 0.01}}
   :strip-angle {:type :range
                 :min -90
                 :max 90
                 :default 0
                 :ui {:label "Strip angle"
                      :step 1}}
   :start-angle {:type :range
                 :min -180
                 :max 180
                 :default 0
                 :ui {:label "Start angle"
                      :step 1}}
   :arc-angle {:type :range
               :min 0
               :max 360
               :default 360
               :ui {:label "Arc angle"
                    :step 1}}
   :slots {:type :range
           :min 1
           :max 20
           :default 5
           :integer? true
           :ui {:label "Number"
                :form-type :charge-group-slot-number}}
   :radius {:type :range
            :min 0
            :max 100
            :default 30
            :ui {:label "Radius"
                 :step 0.1}}
   :arc-stretch {:type :range
                 :min 0
                 :max 5
                 :default 1
                 :ui {:label "Stretch"
                      :step 0.01}}
   :rotate-charges? {:type :boolean
                     :default false
                     :ui {:label "Rotate charges"}}})

(defn options [charge-group]
  (when charge-group
    (-> (cond
          (-> charge-group :type #{:heraldry.charge-group.type/rows
                                   :heraldry.charge-group.type/columns}) (options/pick default-options
                                                                                       [[:type]
                                                                                        [:origin]
                                                                                        [:spacing]
                                                                                        [:stretch]
                                                                                        [:strip-angle]])
          (-> charge-group :type (= :heraldry.charge-group.type/arc)) (options/pick default-options
                                                                                    [[:type]
                                                                                     [:origin]
                                                                                     [:arc-stretch]
                                                                                     [:start-angle]
                                                                                     [:arc-angle]
                                                                                     [:slots]
                                                                                     [:radius]
                                                                                     [:rotate-charges?]]))
        (update :origin (fn [position]
                          (when position
                            (-> position
                                (position/adjust-options (-> charge-group :origin))))))
        (update :anchor (fn [position]
                          (when position
                            (-> position
                                (position/adjust-options (-> charge-group :anchor)))))))))

(def strip-options
  {:slots {:type :range
           :min 0
           :max 10
           :default 3
           :integer? true
           :ui {:label "Number"
                :form-type :charge-group-slot-number}}
   :stretch {:type :range
             :min 0
             :max 5
             :default 1
             :ui {:label "Stretch"
                  :step 0.01}}
   :offset {:type :range
            :min -3
            :max 3
            :default 0
            :ui {:label "Offset"
                 :step 0.01}}})

(defmethod interface/component-options :charge-group [data _path]
  (options data))

(defmethod interface/component-options :charge-group-strip [_data _path]
  strip-options)

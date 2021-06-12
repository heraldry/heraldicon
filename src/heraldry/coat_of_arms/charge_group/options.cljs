(ns heraldry.coat-of-arms.charge-group.options
  (:require [heraldry.coat-of-arms.position :as position]
            [heraldry.util :as util]))

(def default-options
  {:origin (-> position/default-options
               (assoc-in [:alignment] nil))
   :anchor (-> position/anchor-default-options
               (assoc-in [:point :default] :angle)
               (update-in [:point :choices] (fn [choices]
                                              (-> choices
                                                  drop-last
                                                  (conj (last choices))
                                                  vec)))
               (assoc-in [:alignment] nil)
               (assoc-in [:angle :min] -180)
               (assoc-in [:angle :max] 180)
               (assoc-in [:angle :default] 0))
   :spacing {:type :range
             :min 1
             :max 100
             :default 10}
   :stretch {:type :range
             :min 0
             :max 5
             :default 1}
   :strip-angle {:type :range
                 :min -90
                 :max 90
                 :default 0}})

(def type-choices
  [["Rows" :heraldry.charge-group.type/rows]
   ["Columns" :heraldry.charge-group.type/columns]])

(def type-map
  (util/choices->map type-choices))

(def strip-options
  {:num-slots {:type :range
               :min 0
               :max 10
               :default 3}
   :stretch {:type :range
             :min 0
             :max 5
             :default 1}
   :offset {:type :range
            :min -3
            :max 3
            :default 0}})

(def strip-type-choices
  [["Rows" :rows]
   ["Columns" :columns]])

(def strip-type-map
  (util/choices->map strip-type-choices))

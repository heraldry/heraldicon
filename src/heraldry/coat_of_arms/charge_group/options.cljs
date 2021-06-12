(ns heraldry.coat-of-arms.charge-group.options
  (:require [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.position :as position]
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
             :default 40}
   :stretch {:type :range
             :min 0
             :max 5
             :default 1}
   :strip-angle {:type :range
                 :min -90
                 :max 90
                 :default 0}
   :start-angle {:type :range
                 :min -180
                 :max 180
                 :default 0}
   :arc-angle {:type :range
               :min 0
               :max 360
               :default 360}
   :num-slots {:type :range
               :min 1
               :max 20
               :default 5}
   :radius {:type :range
            :min 0
            :max 100
            :default 30}
   :arc-stretch {:type :range
                 :min 0
                 :max 5
                 :default 1}
   :rotate-charges? {:type :boolean
                     :default false}})

(defn options [charge-group]
  (when charge-group
    (cond
      (-> charge-group :type #{:heraldry.charge-group.type/rows
                               :heraldry.charge-group.type/columns}) (options/pick default-options
                                                                                   [[:origin]
                                                                                    [:spacing]
                                                                                    [:stretch]
                                                                                    [:strip-angle]])
      (-> charge-group :type (= :heraldry.charge-group.type/arc)) (options/pick default-options
                                                                                [[:origin]
                                                                                 [:arc-stretch]
                                                                                 [:start-angle]
                                                                                 [:arc-angle]
                                                                                 [:num-slots]
                                                                                 [:radius]
                                                                                 [:rotate-charges?]]))))

(def type-choices
  [["Rows" :heraldry.charge-group.type/rows]
   ["Columns" :heraldry.charge-group.type/columns]
   ["Arc" :heraldry.charge-group.type/arc]])

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

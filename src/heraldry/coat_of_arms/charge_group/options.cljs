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
               (assoc-in [:angle :default] 0))})

(def alignment-choices
  [["Left" :left]
   ["Center" :center]
   ["Right" :right]])

(def alignment-map
  (util/choices->map alignment-choices))

(def strip-options
  {:spacing {:type :range
             :min 1
             :max 100
             :default 100}
   :alignment {:type :choice
               :choices alignment-choices}
   :offset {:type :range
            :min 0
            :max 1
            :default 0}})

(def strip-type-choices
  [["Rows" :rows]
   ["Columns" :columns]])

(def strip-type-map
  (util/choices->map strip-type-choices))

(def strips-options
  {:strip-type {:type :choice
                :choices strip-type-choices}
   :spacing {:type :range
             :min 1
             :max 100
             :default 100}
   :strip-angle {:type :range
                 :min -45
                 :max 45
                 :default 0}})

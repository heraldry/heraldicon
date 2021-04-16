(ns heraldry.coat-of-arms.semy.options
  (:require [heraldry.coat-of-arms.position :as position]))

(def default-options
  {:origin (-> position/default-options
               (dissoc :alignment))
   :layout {:num-fields-x {:type     :range
                           :min      1
                           :max      20
                           :default  6
                           :integer? true}
            :num-fields-y {:type     :range
                           :min      1
                           :max      20
                           :default  6
                           :integer? true}
            :offset-x     {:type    :range
                           :min     -1
                           :max     1
                           :default 0}
            :offset-y     {:type    :range
                           :min     -1
                           :max     1
                           :default 0}
            :stretch-x    {:type    :range
                           :min     0.5
                           :max     2
                           :default 1}
            :stretch-y    {:type    :range
                           :min     0.5
                           :max     2
                           :default 1}
            :rotation     {:type    :range
                           :min     -90
                           :max     90
                           :default 0}}})


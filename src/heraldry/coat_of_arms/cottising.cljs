(ns heraldry.coat-of-arms.cottising
  (:require [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.tincture.core :as tincture]))

(def cottise-options
  {:line          line/default-options
   :opposite-line line/default-options
   :distance      {:type    :range
                   :min     1
                   :max     10
                   :default 4}
   :thickness     {:type    :range
                   :min     1
                   :max     10
                   :default 4}
   :tincture      {:type    :choice
                   :choices (-> [["None" :none]]
                                (into tincture/choices))
                   :default :none}})

(def cottising-choices
  [["None" :none]
   ["Single" :single]
   ["Double" :double]])

(def options
  {:mode               {:type    :choice
                        :choices cottising-choices
                        :default :none}
   :cottise-1          cottise-options
   :cottise-opposite-1 cottise-options
   :cottise-2          cottise-options
   :cottise-opposite-2 cottise-options})


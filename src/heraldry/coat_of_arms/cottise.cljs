(ns heraldry.coat-of-arms.cottise
  (:require [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.tincture.core :as tincture]))

(def base-options
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

(def options
  (assoc base-options :cottise base-options))


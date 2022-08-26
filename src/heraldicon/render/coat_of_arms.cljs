(ns heraldicon.render.coat-of-arms
  (:require
   [heraldicon.interface :as interface]))

(defn render [context width]
  (let [context (assoc context :target-width width)]
    {:environment (interface/get-environment context)
     :result (interface/render-component context)}))

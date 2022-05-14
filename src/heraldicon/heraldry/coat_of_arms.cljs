(ns heraldicon.heraldry.coat-of-arms
  (:require
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]))

(defmethod interface/options-subscriptions :heraldry/coat-of-arms [_context]
  #{})

(defmethod interface/options :heraldry/coat-of-arms [_context]
  {:manual-blazon options/manual-blazon})

(defmethod interface/blazon-component :heraldry/coat-of-arms [context]
  (interface/blazon (-> context
                        (c/++ :field)
                        (assoc-in [:blazonry :root?] true))))

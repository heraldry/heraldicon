(ns heraldicon.heraldry.coat-of-arms
  (:require
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]))

(defmethod interface/options-subscriptions :heraldry.component/coat-of-arms [_context]
  #{})

(defmethod interface/options :heraldry.component/coat-of-arms [_context]
  {:manual-blazon options/manual-blazon})

(defmethod interface/blazon-component :heraldry.component/coat-of-arms [context]
  (interface/blazon (-> context
                        (c/++ :field)
                        (assoc-in [:blazonry :root?] true))))

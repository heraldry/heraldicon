(ns heraldicon.heraldry.coat-of-arms
  (:require
   [clojure.string :as s]
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]))

(derive :heraldry/coat-of-arms :heraldry.options/root)

(defmethod interface/options-subscriptions :heraldry/coat-of-arms [_context]
  #{})

(defmethod interface/options :heraldry/coat-of-arms [_context]
  {:manual-blazon options/manual-blazon})

(defmethod interface/blazon-component :heraldry/coat-of-arms [context]
  (interface/blazon (-> context
                        (c/++ :field)
                        (assoc-in [:blazonry :root?] true))))

(defmethod interface/environment :heraldry/coat-of-arms [context]
  (:environment context))

(defmethod interface/render-shape :heraldry/coat-of-arms [context]
  (s/join "" (-> context :environment :shape :paths)))

(defmethod interface/exact-shape :heraldry/coat-of-arms [context]
  (interface/render-shape context))

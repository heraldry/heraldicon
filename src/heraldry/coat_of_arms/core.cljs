(ns heraldry.coat-of-arms.core
  (:require
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.strings :as strings]))

(defmethod interface/options :heraldry.component/coat-of-arms [_context]
  {:manual-blazon {:type :text
                   :default nil
                   :ui {:label strings/manual-blazon}}})

(defmethod interface/blazon-component :heraldry.component/coat-of-arms [context]
  (interface/blazon (-> context
                        (c/++ :field)
                        (assoc-in [:blazonry :root?] true))))

(ns heraldry.coat-of-arms.core
  (:require
   [heraldry.context :as c]
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]))

(defmethod interface/options-subscriptions :heraldry.component/coat-of-arms [_context]
  #{})

(defmethod interface/options :heraldry.component/coat-of-arms [_context]
  {:manual-blazon {:type :text
                   :default nil
                   :ui {:label (string "Manual blazon")}}})

(defmethod interface/blazon-component :heraldry.component/coat-of-arms [context]
  (interface/blazon (-> context
                        (c/++ :field)
                        (assoc-in [:blazonry :root?] true))))

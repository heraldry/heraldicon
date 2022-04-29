(ns heraldicon.coat-of-arms.core
  (:require
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]))

(defmethod interface/options-subscriptions :heraldry.component/coat-of-arms [_context]
  #{})

(defmethod interface/options :heraldry.component/coat-of-arms [_context]
  {:manual-blazon {:type :text
                   :default nil
                   :ui {:label :string.option/manual-blazon}}})

(defmethod interface/blazon-component :heraldry.component/coat-of-arms [context]
  (interface/blazon (-> context
                        (c/++ :field)
                        (assoc-in [:blazonry :root?] true))))

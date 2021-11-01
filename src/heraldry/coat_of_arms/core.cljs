(ns heraldry.coat-of-arms.core
  (:require
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.strings :as strings]))

(def default-options
  {:manual-blazon {:type :text
                   :default nil
                   :ui {:label strings/manual-blazon}}})

(defn options [_coat-of-arms]
  default-options)

(defmethod interface/component-options :heraldry.component/coat-of-arms [_path data]
  (options data))

(defmethod interface/blazon-component :heraldry.component/coat-of-arms [context]
  (interface/blazon (-> context
                        (c/++ :field)
                        (assoc-in [:blazonry :root?] true))))

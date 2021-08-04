(ns heraldry.coat-of-arms.core
  (:require [heraldry.interface :as interface]))

(def default-options
  {:manual-blazon {:type :text
                   :default nil
                   :ui {:label "Manual blazon"}}})

(defn options [_coat-of-arms]
  default-options)

(defmethod interface/component-options :heraldry.component/coat-of-arms [_path data]
  (options data))

(defmethod interface/blazon-component :heraldry.component/coat-of-arms [path context]
  (interface/blazon (conj path :field) (assoc-in context [:blazonry :root?] true)))


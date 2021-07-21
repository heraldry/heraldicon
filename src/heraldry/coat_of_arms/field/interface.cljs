(ns heraldry.coat-of-arms.field.interface
  (:require [heraldry.interface :as interface]
            [taoensso.timbre :as log]))

(defmulti display-name identity)

(defmulti part-names identity)

(defmulti render-field (fn [path _environment context]
                         (let [field-type (interface/get-sanitized-data (conj path :type) context)]
                           (when (keyword? field-type)
                             field-type))))

(defmethod render-field nil [path _environment context]
  (log/warn :not-implemented path context)
  [:<>])

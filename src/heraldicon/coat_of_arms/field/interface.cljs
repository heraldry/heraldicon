(ns heraldicon.coat-of-arms.field.interface
  (:require
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [taoensso.timbre :as log]))

(defmulti display-name identity)

(defmulti part-names identity)

(defmulti render-field (fn [context]
                         (let [field-type (interface/get-sanitized-data (c/++ context :type))]
                           (when (keyword? field-type)
                             field-type))))

(defmethod render-field nil [context]
  (log/warn :not-implemented "render-field" context)
  [:<>])

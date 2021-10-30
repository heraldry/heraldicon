(ns heraldry.coat-of-arms.field.interface
  (:require
   [heraldry.interface :as interface]
   [taoensso.timbre :as log]))

(defmulti display-name identity)

(defmulti part-names identity)

(defmulti render-field (fn [context]
                         (let [field-type (interface/get-sanitized-data (update context :path conj :type))]
                           (when (keyword? field-type)
                             field-type))))

(defmethod render-field nil [context]
  (log/warn :not-implemented "render-field" context)
  [:<>])

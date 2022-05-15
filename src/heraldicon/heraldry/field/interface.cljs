(ns heraldicon.heraldry.field.interface
  (:require
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [taoensso.timbre :as log]))

(defmulti display-name identity)

(defmulti part-names identity)

(defmulti render-field (fn [context]
                         (interface/get-sanitized-data (c/++ context :type))))

(defmethod render-field nil [context]
  (log/warn :not-implemented "render-field" context)
  [:<>])

(defmulti options interface/effective-component-type)

(defmethod options nil [context]
  (log/warn :not-implemented "field.options" context)
  [:<>])

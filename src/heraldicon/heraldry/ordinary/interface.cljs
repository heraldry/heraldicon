(ns heraldicon.heraldry.ordinary.interface
  (:require
   [heraldicon.interface :as interface]
   [taoensso.timbre :as log]))

(defmulti display-name identity)

(defmulti options interface/effective-component-type)

(defmethod options nil [context]
  (log/warn :not-implemented "ordinary.options" context)
  [:<>])

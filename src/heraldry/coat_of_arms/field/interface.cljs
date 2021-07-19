(ns heraldry.coat-of-arms.field.interface
  (:require [heraldry.coat-of-arms.options :as options]
            [taoensso.timbre :as log]))

(defmulti display-name identity)

(defmulti part-names identity)

(defmulti render-field (fn [path _environment context]
                         (options/sanitized-value (conj path :type) context)))

(defmethod render-field nil [path _environment context]
  (log/warn :not-implemented path context)
  [:<>])

(defmethod render-field {} [path _environment context]
  (log/warn :not-implemented path context)
  [:<>])

(ns heraldry.coat-of-arms.field.interface
  (:require [heraldry.coat-of-arms.options :as options]
            [taoensso.timbre :as log]))

(defmulti display-name identity)

(defmulti part-names identity)

(defmulti render-field (fn [path _environment context]
                         ;; TODO: still got that foot gun here: type must not be sanitized right now
                         (options/raw-value (conj path :type) context)))

(defmethod render-field nil [path _environment context]
  (log/warn :not-implemented path context)
  [:<>])

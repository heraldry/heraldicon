(ns heraldry.coat-of-arms.ordinary.interface
  (:require [heraldry.interface :as interface]
            [taoensso.timbre :as log]))

(defmulti display-name identity)

(defmulti render-ordinary (fn [path _parent-path _environment context]
                            (let [ordinary-type (interface/get-sanitized-data (conj path :type) context)]
                              (when (keyword? ordinary-type)
                                ordinary-type))))

(defmethod render-ordinary nil [path _parent-path _environment context]
  (log/warn :not-implemented path context)
  [:<>])

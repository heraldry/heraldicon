(ns heraldicon.heraldry.ordinary.interface
  (:require
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [taoensso.timbre :as log]))

(defmulti display-name identity)

(defmulti render-ordinary (fn [context]
                            (let [ordinary-type (interface/get-sanitized-data (c/++ context :type))]
                              (when (keyword? ordinary-type)
                                ordinary-type))))

(defmethod render-ordinary nil [context]
  (log/warn :not-implemented "render-ordinary" context)
  [:<>])

(defmulti options interface/effective-component-type)

(defmethod options nil [context]
  (log/warn :not-implemented "ordinary.options" context)
  [:<>])

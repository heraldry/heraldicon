(ns heraldry.coat-of-arms.ordinary.interface
  (:require
   [heraldry.interface :as interface]
   [taoensso.timbre :as log]))

(defmulti display-name identity)

(defmulti render-ordinary (fn [context]
                            (let [ordinary-type (interface/get-sanitized-data (update context :path conj :type))]
                              (when (keyword? ordinary-type)
                                ordinary-type))))

(defmethod render-ordinary nil [context]
  (log/warn :not-implemented "render-ordinary" context)
  [:<>])

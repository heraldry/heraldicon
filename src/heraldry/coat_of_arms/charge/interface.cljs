(ns heraldry.coat-of-arms.charge.interface
  (:require
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [taoensso.timbre :as log]))

(defmulti display-name identity)

(defmulti render-charge (fn [context]
                          (let [data (interface/get-raw-data (c/++ context :data))
                                variant (interface/get-raw-data (c/++ context :variant))
                                charge-type (interface/get-sanitized-data (c/++ context :type))]
                            (if (or data (seq variant))
                              :heraldry.charge.type/other
                              charge-type))))

(defmethod render-charge nil [context]
  (log/warn :not-implemented "render-charge" context)
  [:<>])

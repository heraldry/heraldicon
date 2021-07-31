(ns heraldry.coat-of-arms.charge.interface
  (:require [heraldry.interface :as interface]
            [taoensso.timbre :as log]))

(defmulti display-name identity)

(defmulti render-charge (fn [path _parent-path _environment context]
                          (let [data (interface/get-raw-data (conj path :data) context)
                                variant (interface/get-raw-data (conj path :variant) context)
                                charge-type (interface/get-sanitized-data (conj path :type) context)]
                            (if (or data (seq variant))
                              :heraldry.charge.type/other
                              charge-type))))

(defmethod render-charge nil [path _parent-path _environment context]
  (log/warn :not-implemented "render-charge" path context)
  [:<>])

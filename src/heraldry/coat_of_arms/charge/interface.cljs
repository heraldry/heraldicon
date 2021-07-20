(ns heraldry.coat-of-arms.charge.interface
  (:require [heraldry.options :as options]
            [taoensso.timbre :as log]))

(defmulti display-name identity)

(defmulti render-charge (fn [path _parent-path _environment context]
                          (let [data (options/raw-value (conj path :data) context)
                                variant (options/raw-value (conj path :variant) context)
                                charge-type (options/sanitized-value (conj path :type) context)]
                            (if (or data (seq variant))
                              :heraldry.charge.type/other
                              charge-type))))

(defmethod render-charge nil [path _parent-path _environment context]
  (log/warn :not-implemented path context)
  [:<>])

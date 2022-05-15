(ns heraldicon.heraldry.charge.interface
  (:require
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [taoensso.timbre :as log]))

(defmulti display-name identity)

(defn effective-type [context]
  (let [data (interface/get-raw-data (c/++ context :data))
        variant (interface/get-raw-data (c/++ context :variant))
        charge-type (interface/get-raw-data (c/++ context :type))]
    ;; TODO: this would fail if there's ever a charge-type for which no render method
    ;; exists and no variant is given
    (if (or data
            (seq variant)
            (= charge-type :heraldry.charge.type/preview))
      :heraldry.charge.type/other
      charge-type)))

(defmulti render-charge effective-type)

(defmethod render-charge nil [context]
  (log/warn :not-implemented "render-charge" context)
  [:<>])

(defmulti options effective-type)

(defmethod options nil [context]
  (log/warn :not-implemented "charge.options" context)
  [:<>])

(ns heraldry.coat-of-arms.field.type.plain
  (:require
   [heraldry.coat-of-arms.field.interface :as field-interface]
   [heraldry.coat-of-arms.tincture.core :as tincture]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.strings :as strings]))

(def field-type :heraldry.field.type/plain)

(defmethod field-interface/display-name field-type [_] {:en "Plain"
                                                        :de "Ungeteilt"})

(defmethod field-interface/part-names field-type [_] [])

(defmethod interface/options field-type [context]
  (let [tincture (interface/get-raw-data (c/++ context :tincture))]
    (cond-> {:tincture {:type :choice
                        :choices tincture/choices
                        :default :none
                        :ui {:label strings/tincture
                             :form-type :tincture-select}}}
      (tincture/furs tincture) (assoc :pattern-scaling {:type :range
                                                        :min 0.1
                                                        :max 3
                                                        :default 1
                                                        :ui {:label {:en "Pattern scaling"
                                                                     :de "Musterskalierung"}
                                                             :step 0.01}}))))

(defmethod field-interface/render-field field-type
  [context]
  (tincture/tinctured-field context))

(ns heraldry.coat-of-arms.field.type.plain
  (:require
   [heraldry.coat-of-arms.field.interface :as field-interface]
   [heraldry.coat-of-arms.tincture.core :as tincture]
   [heraldry.context :as c]))

(def field-type :heraldry.field.type/plain)

(defmethod field-interface/display-name field-type [_] {:en "Plain"
                                                        :de "Ungeteilt"})

(defmethod field-interface/part-names field-type [_] [])

(defmethod field-interface/render-field field-type
  [context]
  (tincture/tinctured-field
   (c/++ context :tincture)))

(ns heraldicon.heraldry.field.type.counterchanged
  (:require
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.interface :as interface]))

(def field-type :heraldry.field.type/counterchanged)

(defmethod field.interface/display-name field-type [_] :string.field.type/counterchanged)

(defmethod field.interface/part-names field-type [_] [])

(defmethod field.interface/options field-type [_context]
  {})

(defmethod interface/properties field-type [_context]
  {:type field-type})

;; the field shows its parent field; it has no subfields of its own
(defmethod interface/subfield-environments field-type [_context])

(defmethod interface/subfield-render-shapes field-type [_context])

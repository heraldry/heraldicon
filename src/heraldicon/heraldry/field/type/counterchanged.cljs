(ns heraldicon.heraldry.field.type.counterchanged
  (:require
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.interface :as interface]))

(def field-type :heraldry.field.type/counterchanged)

(defmethod field.interface/display-name field-type [_] :string.field.type/counterchanged)

(defmethod field.interface/part-names field-type [_] [])

(defmethod interface/options field-type [_context]
  {})

(defmethod field.interface/render-field field-type
  [_context]
  [:<>])

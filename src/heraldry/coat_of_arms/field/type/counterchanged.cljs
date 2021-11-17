(ns heraldry.coat-of-arms.field.type.counterchanged
  (:require
   [heraldry.coat-of-arms.field.interface :as field-interface]
   [heraldry.interface :as interface]))

(def field-type :heraldry.field.type/counterchanged)

(defmethod field-interface/display-name field-type [_] {:en "Counterchanged"
                                                        :de "Verwechselt"})

(defmethod field-interface/part-names field-type [_] [])

(defmethod interface/options-subscriptions field-type [_context]
  #{})

(defmethod interface/options field-type [_context]
  {})

(defmethod field-interface/render-field field-type
  [_context]
  [:<>])
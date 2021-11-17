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

(defmethod interface/options-subscriptions field-type [_context]
  #{})

(defmethod interface/options field-type [_context]
  {:tincture {:type :choice
              :choices tincture/choices
              :default :none
              :ui {:label strings/tincture
                   :form-type :tincture-select}} })

(defmethod field-interface/render-field field-type
  [context]
  (tincture/tinctured-field
   (c/++ context :tincture)))

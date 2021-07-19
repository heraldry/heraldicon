(ns heraldry.coat-of-arms.field.type.plain
  (:require [heraldry.coat-of-arms.field.interface :as interface]
            [heraldry.options :as options]
            [heraldry.coat-of-arms.tincture.core :as tincture]))

(def field-type
  :heraldry.field.type/plain)

(defmethod interface/display-name field-type [_] "Plain")

(defmethod interface/part-names field-type [_] [])

(defmethod interface/render-field field-type
  [path _environment context]
  (let [tincture (options/sanitized-value (conj path :tincture) context)
        fill (tincture/pick2 tincture context)]
    [:rect {:x -500
            :y -500
            :width 1100
            :height 1100
            :fill fill
            :stroke fill}]))

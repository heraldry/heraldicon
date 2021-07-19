(ns heraldry.coat-of-arms.field.type.plain
  (:require [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.tincture.core :as tincture]))

(defn render
  {:display-name "Plain"
   :value :heraldry.field.type/plain}
  [path _environment context]
  (let [tincture (options/sanitized-value (conj path :tincture) context)
        fill (tincture/pick2 tincture context)]
    [:rect {:x -500
            :y -500
            :width 1100
            :height 1100
            :fill fill
            :stroke fill}]))

(ns heraldry.coat-of-arms.field.type.plain
  (:require [heraldry.coat-of-arms.tincture.core :as tincture]))

(defn render
  {:display-name "Plain"
   :value        :plain}
  [field _environment {:keys [render-options] :as _context}]
  (let [fill (tincture/pick (:tincture field) render-options)]
    [:rect {:x      -500
            :y      -500
            :width  1100
            :height 1100
            :fill   fill
            :stroke fill}]))


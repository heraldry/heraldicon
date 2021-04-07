(ns heraldry.coat-of-arms.ordinary.type.base
  (:require [heraldry.coat-of-arms.counterchange :as counterchange]
            [heraldry.coat-of-arms.field.shared :as division-shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.ordinary.options :as ordinary-options]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(defn render
  {:display-name "Base"
   :value :base}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line geometry]} (options/sanitize ordinary (ordinary-options/options ordinary))
        {:keys [size]} geometry
        points (:points environment)
        bottom (:bottom points)
        bottom-right (:bottom-right points)
        left (:left points)
        right (:right points)
        height (:height environment)
        band-height (-> size
                        ((util/percent-of height)))
        row (- (:y bottom) band-height)
        row-left (v/v (:x left) row)
        row-right (v/v (:x right) row)
        line (-> line
                 (update-in [:fimbriation :thickness-1] (util/percent-of height))
                 (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        {line-one :line
         line-one-start :line-start
         :as line-one-data} (line/create line
                                         row-left row-right
                                         :render-options render-options
                                         :environment environment)
        parts [[["M" (v/+ row-left
                          line-one-start)
                 (svg/stitch line-one)
                 (infinity/path :clockwise
                                [:right :left]
                                [(v/+ row-right
                                      line-one-start)
                                 (v/+ row-left
                                      line-one-start)])
                 "z"]
                [row-left bottom-right]]]
        field (if (:counterchanged? field)
                (counterchange/counterchange-field ordinary parent)
                field)
        outline? (or (:outline? render-options)
                     (:outline? hints))]
    [:<>
     [division-shared/make-subfields
      :ordinary-base [field] parts
      [:all]
      environment ordinary context]
     (line/render line [line-one-data] row-left outline? render-options)]))

(ns heraldry.coat-of-arms.ordinary.type.chief
  (:require [heraldry.coat-of-arms.counterchange :as counterchange]
            [heraldry.coat-of-arms.field.shared :as field-shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.ordinary.options :as ordinary-options]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(defn render
  {:display-name "Chief"
   :value        :heraldry.ordinary.type/chief}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line geometry]}                  (options/sanitize ordinary (ordinary-options/options ordinary))
        {:keys [size]}                           geometry
        points                                   (:points environment)
        top                                      (:top points)
        top-left                                 (:top-left points)
        left                                     (:left points)
        right                                    (:right points)
        height                                   (:height environment)
        band-height                              (-> size
                                                     ((util/percent-of height)))
        row                                      (+ (:y top) band-height)
        row-left                                 (v/v (:x left) row)
        row-right                                (v/v (:x right) row)
        line                                     (-> line
                                                     (update-in [:fimbriation :thickness-1] (util/percent-of height))
                                                     (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        {line-reversed       :line
         line-reversed-start :line-start
         :as                 line-reversed-data} (line/create line
                                                              row-left row-right
                                                              :reversed? true
                                                              :render-options render-options
                                                              :environment environment)
        parts                                    [[["M" (v/+ row-right
                                                             line-reversed-start)
                                                    (svg/stitch line-reversed)
                                                    (infinity/path :clockwise
                                                                   [:left :right]
                                                                   [(v/+ row-left
                                                                         line-reversed-start)
                                                                    (v/+ row-right
                                                                         line-reversed-start)])
                                                    "z"]
                                                   [top-left row-right]]]
        field                                    (if (:counterchanged? field)
                                                   (counterchange/counterchange-field ordinary parent)
                                                   field)
        outline?                                 (or (:outline? render-options)
                                                     (:outline? hints))]
    [:<>
     [field-shared/make-subfields
      :ordinary-chief [field] parts
      [:all]
      environment ordinary context]
     (line/render line [line-reversed-data] row-right outline? render-options)]))


(ns heraldry.coat-of-arms.ordinary.type.base
  (:require [heraldry.coat-of-arms.cottising :as cottising]
            [heraldry.coat-of-arms.counterchange :as counterchange]
            [heraldry.coat-of-arms.field.shared :as field-shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.ordinary.options :as ordinary-options]
            [heraldry.coat-of-arms.ordinary.type.fess :as fess]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(defn render
  {:display-name "Base"
   :value        :heraldry.ordinary.type/base}
  [{:keys [field hints] :as ordinary} parent environment
   {:keys [render-options override-real-start override-real-end override-shared-start-x] :as context}]
  (let [{:keys [line geometry]}           (options/sanitize ordinary (ordinary-options/options ordinary))
        {:keys [size]}                    geometry
        points                            (:points environment)
        bottom                            (:bottom points)
        bottom-right                      (:bottom-right points)
        left                              (:left points)
        right                             (:right points)
        height                            (:height environment)
        band-height                       (-> size
                                              ((util/percent-of height)))
        row                               (- (:y bottom) band-height)
        row-left                          (v/v (:x left) row)
        row-right                         (v/v  (:x right) row)
        [row-real-left _row-real-right]   (v/environment-intersections
                                           row-left
                                           row-right
                                           environment)
        shared-start-x                    (or override-shared-start-x
                                              (- (:x row-real-left)
                                                 30))
        real-start                        (or override-real-start
                                              (-> row-left :x (- shared-start-x)))
        real-end                          (or override-real-end
                                              (-> row-right :x (- shared-start-x)))
        shared-end-x                      (+ real-end 30)
        row-left                          (v/v shared-start-x (:y row-left))
        row-right                         (v/v shared-end-x (:y row-right))
        line                              (-> line
                                              (update-in [:fimbriation :thickness-1] (util/percent-of height))
                                              (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        {line-one          :line
         line-one-start    :line-start
         line-reversed-min :line-min
         :as               line-one-data} (line/create line
                                                       row-left row-right
                                                       :render-options render-options
                                                       :environment environment)
        parts                             [[["M" (v/+ row-left
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
        field                             (if (:counterchanged? field)
                                            (counterchange/counterchange-field ordinary parent)
                                            field)
        outline?                          (or (:outline? render-options)
                                              (:outline? hints))
        {:keys [cottise-1
                cottise-2]}               (-> ordinary :cottising)]
    [:<>
     [field-shared/make-subfields
      :ordinary-base [field] parts
      [:all]
      environment ordinary context]
     (line/render line [line-one-data] row-left outline? render-options)
     (when (:enabled? cottise-1)
       (let [cottise-1-data (options/sanitize cottise-1 cottising/cottise-options)]
         [fess/render {:type          :heraldry.ordinary.type/fess
                       :hints         {:outline? (-> ordinary :hints :outline?)}
                       :cottising     {:cottise-1 cottise-2}
                       :line          (:line cottise-1)
                       :opposite-line (:opposite-line cottise-1)
                       :field         (:field cottise-1)
                       :geometry      {:size (:thickness cottise-1-data)}
                       :origin        {:point     :bottom
                                       :offset-y  (-> bottom
                                                      :y
                                                      (- row)
                                                      (- line-reversed-min)
                                                      (/ height)
                                                      (* 100)
                                                      (+ (:distance cottise-1-data)))
                                       :alignment :right}} parent environment
          (-> context
              (assoc :override-shared-start-x shared-start-x)
              (assoc :override-real-start real-start)
              (assoc :override-real-end real-end))]))]))


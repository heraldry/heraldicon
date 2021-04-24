(ns heraldry.coat-of-arms.field.type.per-fess
  (:require [heraldry.coat-of-arms.field.options :as field-options]
            [heraldry.coat-of-arms.field.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]))

(defn render
  {:display-name "Per fess"
   :value :heraldry.field.type/per-fess
   :parts ["chief" "base"]}
  [{:keys [type fields hints] :as field} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin]} (options/sanitize field (field-options/options field))
        points (:points environment)
        origin-point (position/calculate origin environment :fess)
        top-left (:top-left points)
        real-left (assoc (:left points) :y (:y origin-point))
        real-right (assoc (:right points) :y (:y origin-point))
        effective-width (or (:width line) 1)
        effective-width (cond-> effective-width
                          (:spacing line) (+ (* (:spacing line) effective-width)))
        required-extra-length (-> 30
                                  (/ effective-width)
                                  Math/ceil
                                  inc
                                  (* effective-width))
        left (v/- real-left (v/v required-extra-length 0))
        right (v/+ real-right (v/v required-extra-length 0))
        bottom-right (:bottom-right points)
        {line-one :line
         line-one-start :line-start
         line-one-end :line-end
         :as line-one-data} (line/create line
                                         left right
                                         :render-options render-options
                                         :environment environment)
        parts [[["M" (v/+ left
                          line-one-start)
                 (svg/stitch line-one)
                 (infinity/path :counter-clockwise
                                [:right :left]
                                [(v/+ right
                                      line-one-end)
                                 (v/+ left
                                      line-one-start)])
                 "z"]
                [top-left
                 real-right]]

               [["M" (v/+ left
                          line-one-start)
                 (svg/stitch line-one)
                 (infinity/path :clockwise
                                [:right :left]
                                [(v/+ right
                                      line-one-end)
                                 (v/+ left
                                      line-one-start)])
                 "z"]
                [real-left
                 bottom-right]]]
        outline? (or (:outline? render-options)
                     (:outline? hints))]
    [:<>
     [shared/make-subfields
      (shared/field-context-key type) fields parts
      [:all nil]
      environment field context]
     (line/render line [line-one-data] left outline? render-options)]))

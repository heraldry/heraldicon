(ns heraldry.coat-of-arms.division.type.per-bend
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.division.options :as division-options]
            [heraldry.coat-of-arms.division.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.line.fimbriation :as fimbriation]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]))

(defn render
  {:display-name "Per bend"
   :value :per-bend
   :parts ["chief" "base"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin diagonal-mode]} (options/sanitize division (division-options/options division))
        points (:points environment)
        origin-point (position/calculate origin environment :fess)
        top-right (:top-right points)
        bottom-left (:bottom-left points)
        left (:left points)
        right (:right points)
        direction (angle/direction diagonal-mode points origin-point)
        diagonal-start (v/project-x origin-point (v/dot direction (v/v -1 -1)) (:x left))
        diagonal-end (v/project-x origin-point (v/dot direction (v/v 1 1)) (:x right))
        angle (angle/angle-to-point diagonal-start diagonal-end)
        {line-one :line
         line-one-start :line-start
         line-one-end :line-end
         :as line-one-data} (line/create line
                                         (v/abs (v/- diagonal-end diagonal-start))
                                         :angle angle
                                         :render-options render-options)
        parts [[["M" (v/+ diagonal-start
                          line-one-start)
                 (svg/stitch line-one)
                 (infinity/path :counter-clockwise
                                [:right :top]
                                [(v/+ diagonal-end
                                      line-one-end)
                                 (v/+ diagonal-start
                                      line-one-start)])
                 "z"]
                [diagonal-start
                 top-right
                 diagonal-end]]
               [["M" (v/+ diagonal-start
                          line-one-start)
                 (svg/stitch line-one)
                 (infinity/path :clockwise
                                [:right :top]
                                [(v/+ diagonal-end
                                      line-one-end)
                                 (v/+ diagonal-start
                                      line-one-start)])
                 "z"]
                [diagonal-start
                 diagonal-end
                 bottom-left]]]
        [fimbriation-elements
         fimbriation-outlines] (fimbriation/render
                                [diagonal-start :top]
                                [diagonal-end :right]
                                [line-one-data]
                                (:fimbriation line)
                                render-options)]
    [:<>
     [shared/make-division
      (shared/division-context-key type) fields parts
      [:all nil]
      (when (or (:outline? render-options)
                (:outline? hints))
        [:g outline/style
         [:path {:d (svg/make-path
                     ["M" (v/+ diagonal-start
                               line-one-start)
                      (svg/stitch line-one)])}]
         fimbriation-outlines])
      environment division context]
     fimbriation-elements]))

(ns heraldry.coat-of-arms.division.type.per-bend
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.division.options :as division-options]
            [heraldry.coat-of-arms.division.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]))

(defn render
  {:display-name "Per bend"
   :value        :per-bend
   :parts        ["chief" "base"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin anchor]}   (options/sanitize division (division-options/options division))
        points                         (:points environment)
        top-right                      (:top-right points)
        bottom-left                    (:bottom-left points)
        left                           (:left points)
        right                          (:right points)
        {origin-point :real-origin
         anchor-point :real-anchor}    (angle/calculate-origin-and-anchor
                                        environment
                                        origin
                                        anchor
                                        0
                                        nil)
        direction                      (v/- anchor-point origin-point)
        direction                      (v/v (-> direction :x Math/abs)
                                            (-> direction :y Math/abs))
        real-diagonal-start            (v/project-x origin-point (v/dot direction (v/v -1 -1)) (:x left))
        real-diagonal-end              (v/project-x origin-point (v/dot direction (v/v 1 1)) (:x right))
        intersections (-> (v/find-intersections real-diagonal-start real-diagonal-end environment)
                          (->> (filter (comp zero? :parent-level))))
        real-diagonal-start (-> intersections
                                first
                                (or real-diagonal-start))
        real-diagonal-end (-> intersections
                              last
                              (or real-diagonal-end))
        effective-width                (or (:width line) 1)
        required-extra-length          (-> 30
                                           (/ effective-width)
                                           Math/ceil
                                           inc
                                           (* effective-width))
        diagonal-start                 (v/+ real-diagonal-start (-> direction
                                                                    (v/dot (v/v -1 -1))
                                                                    (v// (v/abs direction))
                                                                    (v/* required-extra-length)))
        diagonal-end                   (v/+ real-diagonal-end (-> direction
                                                                  (v// (v/abs direction))
                                                                  (v/* required-extra-length)))
        {line-one       :line
         line-one-start :line-start
         line-one-end   :line-end
         :as            line-one-data} (line/create2 line
                                                     diagonal-start diagonal-end
                                                     :render-options render-options
                                                     :environment environment)
        parts                          [[["M" (v/+ diagonal-start
                                                   line-one-start)
                                          (svg/stitch line-one)
                                          (infinity/path :counter-clockwise
                                                         [:right :top]
                                                         [(v/+ diagonal-end
                                                               line-one-end)
                                                          (v/+ diagonal-start
                                                               line-one-start)])
                                          "z"]
                                         [real-diagonal-start
                                          top-right
                                          real-diagonal-end]]
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
                                         [real-diagonal-start
                                          real-diagonal-end
                                          bottom-left]]]
        outline?                       (or (:outline? render-options)
                                           (:outline? hints))]
    [:<>
     [shared/make-division
      (shared/division-context-key type) fields parts
      [:all nil]
      environment division context]
     (line/render line [line-one-data] diagonal-start outline? render-options)]))


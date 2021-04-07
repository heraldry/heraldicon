(ns heraldry.coat-of-arms.field.type.quarterly
  (:require [heraldry.coat-of-arms.division.options :as division-options]
            [heraldry.coat-of-arms.division.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]))

(defn quarterly-parts [{:keys [num-fields-x
                               offset-x
                               stretch-x
                               num-fields-y
                               offset-y
                               stretch-y]} top-left bottom-right hints render-options]
  (let [offset-x (or offset-x 0)
        stretch-x (or stretch-x 1)
        width (- (:x bottom-right)
                 (:x top-left))
        part-width (-> width
                       (/ num-fields-x)
                       (* stretch-x))
        required-width (* part-width
                          num-fields-x)
        middle-x (-> width
                     (/ 2)
                     (+ (:x top-left)))
        x0 (-> middle-x
               (- (/ required-width 2))
               (+ (* offset-x
                     part-width)))
        offset-y (or offset-y 0)
        stretch-y (or stretch-y 1)
        height (- (:y bottom-right)
                  (:y top-left))
        part-height (-> height
                        (/ num-fields-y)
                        (* stretch-y))
        required-height (* part-height
                           num-fields-y)
        middle-y (-> height
                     (/ 2)
                     (+ (:y top-left)))
        y0 (-> middle-y
               (- (/ required-height 2))
               (+ (* offset-y
                     part-height)))
        parts (->> (for [j (range num-fields-y)
                         i (range num-fields-x)]
                     (let [x1 (+ x0 (* i part-width))
                           x2 (+ x1 part-width)
                           y1 (+ y0 (* j part-height))
                           y2 (+ y1 part-height)
                           first-x? (zero? i)
                           first-y? (zero? j)
                           last-x? (-> num-fields-x dec (= i))
                           last-y? (-> num-fields-y dec (= j))]
                       (cond
                         (and first-x?
                              last-x?
                              first-y?
                              last-y?) [["M" -1000 -1000
                                         "h" 2000
                                         "v" 2000
                                         "h" -2000
                                         "z"]
                                        [(v/v x1 y1) (v/v x2 y2)]]
                         (and first-x?
                              first-y?) [["M" [x1 y2]
                                          "L" [x2 y2]
                                          "L" [x2 y1]
                                          (infinity/path :counter-clockwise
                                                         [:top :left]
                                                         [(v/v x2 y1) (v/v x1 y2)])
                                          "z"]
                                         [(v/v x1 y1) (v/v x2 y2)]]
                         (and last-x?
                              first-y?) [["M" [x1 y1]
                                          "L" [x1 y2]
                                          "L" [x2 y2]
                                          (infinity/path :counter-clockwise
                                                         [:right :top]
                                                         [(v/v x2 y2) (v/v x1 y1)])
                                          "z"]
                                         [(v/v x1 y1) (v/v x2 y2)]]
                         (and first-x?
                              last-y?) [["M" [x1 y1]
                                         "L" [x2 y1]
                                         "L" [x2 y2]
                                         (infinity/path :clockwise
                                                        [:bottom :left]
                                                        [(v/v x2 y2) (v/v x1 y1)])
                                         "z"]
                                        [(v/v x1 y1) (v/v x2 y2)]]
                         (and last-x?
                              last-y?) [["M" [x1 y2]
                                         "L" [x1 y1]
                                         "L" [x2 y1]
                                         (infinity/path :clockwise
                                                        [:right :bottom]
                                                        [(v/v x2 y1) (v/v x1 y2)])
                                         "z"]
                                        [(v/v x1 y1) (v/v x2 y2)]]
                         first-x? [["M" [x1 y1]
                                    "L" [x2 y1]
                                    "L" [x2 y2]
                                    "L" [x1 y2]
                                    (infinity/path :counter-clockwise
                                                   [:left :left]
                                                   [(v/v x1 y2) (v/v x1 y1)])
                                    "z"]
                                   [(v/v x1 y1) (v/v x2 y2)]]
                         first-y? [["M" [x1 y1]
                                    "L" [x1 y2]
                                    "L" [x2 y2]
                                    "L" [x2 y1]
                                    (infinity/path :counter-clockwise
                                                   [:top :top]
                                                   [(v/v x2 y1) (v/v x1 y1)])
                                    "z"]
                                   [(v/v x1 y1) (v/v x2 y2)]]
                         last-x? [["M" [x2 y2]
                                   "L" [x1 y2]
                                   "L" [x1 y1]
                                   "L" [x2 y1]
                                   (infinity/path :clockwise
                                                  [:right :right]
                                                  [(v/v x2 y1) (v/v x2 y2)])
                                   "z"]
                                  [(v/v x1 y1) (v/v x2 y2)]]
                         last-y? [["M" [x1 y2]
                                   "L" [x1 y1]
                                   "L" [x2 y1]
                                   "L" [x2 y2]
                                   (infinity/path :clockwise
                                                  [:bottom :bottom]
                                                  [(v/v x2 y2) (v/v x1 y2)])
                                   "z"]
                                  [(v/v x1 y1) (v/v x2 y2)]]
                         :else [["M" [x1 y1]
                                 "L" [x2 y1]
                                 "L" [x2 y2]
                                 "L" [x1 y2]
                                 "z"]
                                [(v/v x1 y1) (v/v x2 y2)]])))
                   vec)
        overlap (->> (for [j (range num-fields-y)
                           i (range num-fields-x)]
                       (let [x1 (+ x0 (* i part-width))
                             x2 (+ x1 part-width)
                             y1 (+ y0 (* j part-height))
                             y2 (+ y1 part-height)]
                         [(svg/make-path ["M" [x2 y1]
                                          "L" [x2 y2]
                                          "L" [x1 y2]])]))
                     vec)
        outline-extra 50
        outlines (when (or (:outline? render-options)
                           (:outline? hints))
                   [:g outline/style
                    (for [i (range 1 num-fields-x)]
                      (let [x1 (+ x0 (* i part-width))]
                        ^{:key [:x i]}
                        [:path {:d (svg/make-path ["M" [x1 (- y0 outline-extra)]
                                                   "L" [x1 (+ y0 required-height outline-extra)]])}]))
                    (for [j (range 1 num-fields-y)]
                      (let [y1 (+ y0 (* j part-height))]
                        ^{:key [:y j]}
                        [:path {:d (svg/make-path ["M" [(- x0 outline-extra) y1]
                                                   "L" [(+ x0 required-width outline-extra) y1]])}]))])]
    [parts overlap outlines]))

(defn render
  {:display-name "Quarterly NxM"
   :value :quarterly
   :parts []}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [layout]} (options/sanitize division (division-options/options division))
        points (:points environment)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        [parts overlap outlines] (quarterly-parts layout top-left bottom-right hints render-options)]
    [:<>
     [shared/make-division
      (shared/division-context-key type) fields parts
      overlap
      environment division context]
     outlines]))

(ns heraldry.coat-of-arms.field.type.barry
  (:require [heraldry.coat-of-arms.field.interface :as interface]
            [heraldry.coat-of-arms.field.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]))

(def field-type
  :heraldry.field.type/barry)

(defmethod interface/display-name field-type [_] "Barry")

(defmethod interface/part-names field-type [_] nil)

(defn barry-parts [path top-left bottom-right line outline? context environment]
  (let [num-fields-y (options/sanitized-value (conj path :layout :num-fields-y) context)
        offset-y (options/sanitized-value (conj path :layout :offset-y) context)
        stretch-y (options/sanitized-value (conj path :layout :stretch-y) context)
        height (- (:y bottom-right)
                  (:y top-left))
        bar-height (-> height
                       (/ num-fields-y)
                       (* stretch-y))
        required-height (* bar-height
                           num-fields-y)
        middle (-> height
                   (/ 2)
                   (+ (:y top-left)))
        y0 (-> middle
               (- (/ required-height 2))
               (+ (* offset-y
                     bar-height)))
        x1 (:x top-left)
        x2 (:x bottom-right)
        width (- x2 x1)
        {line-right :line
         line-right-start :line-start
         line-right-end :line-end} (line/create line
                                                top-left
                                                (v/+ top-left (v/v width 0))
                                                :real-start 0
                                                :real-end width
                                                :context context
                                                :environment environment)
        {line-left :line
         line-left-start :line-start
         line-left-end :line-end} (line/create line
                                               top-left
                                               (v/+ top-left (v/v width 0))
                                               :flipped? true
                                               :mirrored? true
                                               :reversed? true
                                               :real-start 0
                                               :real-end width
                                               :context context
                                               :environment environment)
        parts (->> (range num-fields-y)
                   (map (fn [i]
                          (let [y1 (+ y0 (* i bar-height))
                                y2 (+ y1 bar-height)
                                last-part? (-> i inc (= num-fields-y))
                                line-one-left (v/v x1 y1)
                                line-one-right (v/v x2 y1)
                                line-two-left (v/v x1 y2)
                                line-two-right (v/v x2 y2)]
                            [(cond
                               (and (zero? i)
                                    last-part?) ["M" -1000 -1000
                                                 "h" 2000
                                                 "v" 2000
                                                 "h" -2000
                                                 "z"]
                               (zero? i) ["M" (v/+ line-two-left
                                                   line-right-start)
                                          (svg/stitch line-right)
                                          (infinity/path :counter-clockwise
                                                         [:right :left]
                                                         [(v/+ line-two-right
                                                               line-right-end)
                                                          (v/+ line-two-left
                                                               line-right-start)])
                                          "z"]
                               (even? i) (concat ["M" (v/+ line-one-right
                                                           line-left-start)
                                                  (svg/stitch line-left)]
                                                 (cond
                                                   last-part? [(infinity/path :counter-clockwise
                                                                              [:left :right]
                                                                              [(v/+ line-one-left
                                                                                    line-left-end)
                                                                               (v/+ line-one-right
                                                                                    line-left-start)])
                                                               "z"]
                                                   :else [(infinity/path :counter-clockwise
                                                                         [:left :left]
                                                                         [(v/+ line-one-left
                                                                               line-left-end)
                                                                          (v/+ line-two-left
                                                                               line-right-start)])
                                                          (svg/stitch line-right)
                                                          (infinity/path :counter-clockwise
                                                                         [:right :right]
                                                                         [(v/+ line-two-right
                                                                               line-right-end)
                                                                          (v/+ line-one-right
                                                                               line-left-start)])]))
                               :else (concat ["M" (v/+ line-one-left
                                                       line-right-start)
                                              (svg/stitch line-right)]
                                             (cond
                                               last-part? [(infinity/path :clockwise
                                                                          [:right :left]
                                                                          [(v/+ line-one-right
                                                                                line-right-end)
                                                                           (v/+ line-one-left
                                                                                line-right-start)])
                                                           "z"]
                                               :else [(infinity/path :clockwise
                                                                     [:right :right]
                                                                     [(v/+ line-one-right
                                                                           line-right-end)
                                                                      (v/+ line-two-right
                                                                           line-left-start)])
                                                      (svg/stitch line-left)
                                                      (infinity/path :clockwise
                                                                     [:left :left]
                                                                     [(v/+ line-two-left
                                                                           line-left-end)
                                                                      (v/+ line-one-left
                                                                           line-right-start)])
                                                      "z"])))
                             [line-one-left line-two-right]])))
                   vec)
        edges (->> num-fields-y
                   dec
                   range
                   (map (fn [i]
                          (let [y1 (+ y0 (* i bar-height))
                                y2 (+ y1 bar-height)
                                line-two-left (v/v x1 y2)
                                line-two-right (v/v x2 y2)]
                            (if (even? i)
                              (svg/make-path ["M" (v/+ line-two-left
                                                       line-right-start)
                                              (svg/stitch line-right)])
                              (svg/make-path ["M" (v/+ line-two-right
                                                       line-left-start)
                                              (svg/stitch line-left)])))))
                   vec)
        overlap (-> edges
                    (->> (map vector))
                    vec
                    (conj nil))
        outlines (when outline?
                   [:g outline/style
                    (for [i (range (dec num-fields-y))]
                      ^{:key i}
                      [:path {:d (nth edges i)}])])]
    [parts overlap outlines]))

(defmethod interface/render-field field-type
  [path environment context]
  (let [line (options/sanitized-value (conj path :line) context)
        outline? (or (options/render-option :outline? context)
                     (options/sanitized-value (conj path :outline?) context))
        points (:points environment)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        [parts overlap outlines] (barry-parts path top-left bottom-right line outline? context environment)]
    [:<>
     [shared/make-subfields2
      path parts
      overlap
      environment context]
     outlines]))

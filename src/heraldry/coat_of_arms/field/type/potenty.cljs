(ns heraldry.coat-of-arms.field.type.potenty
  (:require [heraldry.coat-of-arms.division.options :as division-options]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.util :as util]))

(defn units [n]
  (-> n
      (* 4)
      (- 1)))

(defn potent-default [part-width part-height]
  (let [width    part-width
        height   (* 2 part-height)
        middle-x (/ width 2)
        middle-y (/ height 2)
        w        (/ width 4)
        h        (/ middle-y 2)]
    {:width   width
     :height  height
     :pattern [:<>
               [:path {:d (str "M 0," middle-y
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "h" w
                               "v" h
                               "z")}]
               [:path {:d (str "M 0," height
                               "v" (- h)
                               "h" w
                               "v" h
                               "z")}]
               [:path {:d (str "M " middle-x "," height
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "H" width
                               "V" height
                               "z")}]]
     :outline [:<>
               [:path {:d (str "M 0," middle-y
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "h" w
                               "v" h
                               "z")}]
               [:path {:d (str "M 0," middle-y
                               "v" h
                               "h" w
                               "v" h
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "z")}]
               [:path {:d (str "M 0,0"
                               "h" width)}]
               [:path {:d (str "M 0," height
                               "h" width)}]
               [:path {:d (str "M" width "," (- middle-y h)
                               "V" (+ middle-y h))}]
               [:path {:d (str "M" width "," middle-y
                               "h" (- w))}]]}))

(defn potent-counter [part-width part-height]
  (let [width    part-width
        height   (* 2 part-height)
        middle-y (/ height 2)
        w        (/ width 4)
        h        (/ middle-y 2)]
    {:width   width
     :height  height
     :pattern [:<>
               [:path {:d (str "M 0," middle-y
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "h" w
                               "v" h
                               "z")}]
               [:path {:d (str "M 0," middle-y
                               "v" h
                               "h" w
                               "v" h
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "z")}]]
     :outline [:<>
               [:path {:d (str "M 0," middle-y
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "h" w
                               "v" h
                               "z")}]
               [:path {:d (str "M 0," middle-y
                               "v" h
                               "h" w
                               "v" h
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "z")}]
               [:path {:d (str "M 0,0"
                               "h" width)}]
               [:path {:d (str "M 0," height
                               "h" width)}]
               [:path {:d (str "M" width "," (- middle-y h)
                               "V" (+ middle-y h))}]
               [:path {:d (str "M" width "," middle-y
                               "h" (- w))}]]}))

(defn potent-in-pale [part-width part-height]
  (let [width    part-width
        height   (* 2 part-height)
        middle-y (/ height 2)
        w        (/ width 4)
        h        (/ middle-y 2)]
    {:width   width
     :height  height
     :pattern [:<>
               [:path {:d (str "M 0," middle-y
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "h" w
                               "v" h
                               "z")}]
               [:path {:d (str "M 0," height
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "h" w
                               "v" h
                               "z")}]]
     :outline [:<>
               [:path {:d (str "M 0," middle-y
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "h" w
                               "v" h
                               "z")}]
               [:path {:d (str "M 0," height
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "h" w
                               "v" h
                               "z")}]
               [:path {:d (str "M 0,0"
                               "h" width)}]
               [:path {:d (str "M 0," height
                               "h" width)}]
               [:path {:d (str "M" width "," middle-y
                               "h" (- w))}]
               [:path {:d (str "M" width "," middle-y
                               "v" (- h))}]
               [:path {:d (str "M" width "," height
                               "v" (- h))}]]}))

(defn potent-en-point [part-width part-height]
  (let [width    part-width
        height   (* 2 part-height)
        middle-x (/ width 2)
        middle-y (/ height 2)
        w        (/ width 4)
        h        (/ middle-y 2)]
    {:width   width
     :height  height
     :pattern [:<>
               [:path {:d (str "M 0," middle-y
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "H" width
                               "v" (* 2 h)
                               "h" (- w)
                               "v" (- h)
                               "h" (- w)
                               "v" h
                               "h" (- w)
                               "v" h
                               "H 0"
                               "z")}]]
     :outline [:<>
               [:path {:d (str "M 0," middle-y
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "H" width
                               "v" h)}]
               [:path {:d (str "M " width "," height
                               "v" (- h)
                               "h" (- w)
                               "v" (- h)
                               "h" (- w)
                               "v" h
                               "h" (- w)
                               "v" h
                               "H 0"
                               "v" (- h))}]
               [:path {:d (str "M 0,0"
                               "h" w)}]
               [:path {:d (str "M" width ",0"
                               "h" w)}]
               [:path {:d (str "M 0," middle-y
                               "v" (- h))}]
               [:path {:d (str "M" middle-x "," height
                               "h" w)}]]}))

(defn render
  {:display-name "Potenty"
   :value        :potenty
   :parts        []}
  [{:keys [fields hints] :as division} environment {:keys [render-options]}]
  (let [{:keys [layout variant]}  (options/sanitize division (division-options/options division))
        points                    (:points environment)
        top-left                  (:top-left points)
        bottom-right              (:bottom-right points)
        {:keys [num-fields-x
                offset-x
                stretch-x
                num-fields-y
                offset-y
                stretch-y]}       layout
        offset-x                  (or offset-x 0)
        stretch-x                 (or stretch-x 1)
        width                     (- (:x bottom-right)
                                     (:x top-left))
        unstretched-part-width    (-> width
                                      (/ (units num-fields-x))
                                      (* 4))
        part-width                (-> unstretched-part-width
                                      (* stretch-x))
        offset-y                  (or offset-y 0)
        stretch-y                 (or stretch-y 1)
        height                    (- (:y bottom-right)
                                     (:y top-left))
        unstretched-part-height   (if num-fields-y
                                    (-> height
                                        (/ num-fields-y))
                                    (-> part-width
                                        (/ 2)))
        part-height               (-> unstretched-part-height
                                      (* stretch-y))
        middle-x                  (/ width 2)
        origin-x                  (+ (:x top-left)
                                     middle-x)
        pattern-id                (util/id "potenty")
        potent-function           (case variant
                                    :counter  potent-counter
                                    :in-pale  potent-in-pale
                                    :en-point potent-en-point
                                    potent-default)
        {pattern-width  :width
         pattern-height :height
         potent-pattern :pattern
         potent-outline :outline} (potent-function part-width part-height)]
    [:g
     [:defs
      (when (or (:outline? render-options)
                (:outline? hints))
        [:pattern {:id            (str pattern-id "-outline")
                   :width         pattern-width
                   :height        pattern-height
                   :x             (+ (* part-width offset-x)
                                     (:x top-left)
                                     (- middle-x
                                        (* origin-x stretch-x)))
                   :y             (+ (* part-height offset-y)
                                     (:y top-left))
                   :pattern-units "userSpaceOnUse"}
         [:g outline/style
          potent-outline]])
      (for [idx (range 2)]
        ^{:key idx}
        [:pattern {:id            (str pattern-id "-" idx)
                   :width         pattern-width
                   :height        pattern-height
                   :x             (+ (* part-width offset-x)
                                     (:x top-left)
                                     (- middle-x
                                        (* origin-x stretch-x)))
                   :y             (+ (* part-height offset-y)
                                     (:y top-left))
                   :pattern-units "userSpaceOnUse"}
         [:rect {:x      0
                 :y      0
                 :width  pattern-width
                 :height pattern-height
                 :fill   (get ["#000000" "#ffffff"] idx)}]
         [:g {:fill (get ["#ffffff" "#000000"] idx)}
          potent-pattern]])]
     (for [idx (range 2)]
       (let [mask-id  (util/id "mask")
             tincture (-> fields
                          (get idx)
                          :tincture)]
         ^{:key idx}
         [:<>
          [:mask {:id mask-id}
           [:rect {:x      -500
                   :y      -500
                   :width  1100
                   :height 1100
                   :fill   (str "url(#" pattern-id "-" idx ")")}]]
          [:rect {:x      -500
                  :y      -500
                  :width  1100
                  :height 1100
                  :mask   (str "url(#" mask-id ")")
                  :fill   (tincture/pick tincture render-options)}]]))
     (when (or (:outline? render-options)
               (:outline? hints))
       [:rect {:x      -500
               :y      -500
               :width  1100
               :height 1100
               :fill   (str "url(#" pattern-id "-outline)")}])]))


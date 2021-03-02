(ns heraldry.coat-of-arms.division.type.vair
  (:require [heraldry.coat-of-arms.division.options :as division-options]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.util :as util]))

(def sqr2 1.4142135623730951)

(defn vair-default [part-width part-height]
  (let [width part-width
        height (* 2 part-height)
        middle-x (/ width 2)
        middle-y (/ height 2)
        w (/ width 4)
        h (/ middle-y (+ 1 1 sqr2))]
    {:width width
     :height height
     :pattern [:<>
               [:path {:d (str "M 0," middle-y
                               "l" w "," (- h)
                               "v" (* sqr2 (- h))
                               "L" middle-x ",0"
                               "l" w "," h
                               "v" (* sqr2 h)
                               "L" width "," middle-y
                               "z")}]
               [:path {:d (str "M 0," middle-y
                               "l" w "," h
                               "v" (* sqr2 h)
                               "L" middle-x "," height
                               "L 0," height
                               "z")}]
               [:path {:d (str "M " width "," middle-y
                               "l" (- w) "," h
                               "v" (* sqr2 h)
                               "L" middle-x "," height
                               "L " width "," height
                               "z")}]]
     :outline [:<>
               [:path {:d (str "M 0,0"
                               "h" width)}]
               [:path {:d (str "M 0," middle-y
                               "l" w "," (- h)
                               "v" (* sqr2 (- h))
                               "L" middle-x ",0"
                               "l" w "," h
                               "v" (* sqr2 h)
                               "L" width "," middle-y)}]
               [:path {:d (str "M 0," middle-y
                               "h" width)}]
               [:path {:d (str "M 0," middle-y
                               "l" w "," h
                               "v" (* sqr2 h)
                               "L" middle-x "," height
                               "L 0," height)}]
               [:path {:d (str "M " width "," middle-y
                               "l" (- w) "," h
                               "v" (* sqr2 h)
                               "L" middle-x "," height
                               "L " width "," height)}]]}))

(defn vair-counter [part-width part-height]
  (let [width part-width
        height (* 2 part-height)
        middle-x (/ width 2)
        middle-y (/ height 2)
        w (/ width 4)
        h (/ middle-y (+ 1 1 sqr2))]
    {:width width
     :height height
     :pattern [:<>
               [:path {:d (str "M 0," middle-y
                               "l" w "," (- h)
                               "v" (* sqr2 (- h))
                               "L" middle-x ",0"
                               "l" w "," h
                               "v" (* sqr2 h)
                               "L" width "," middle-y
                               "l" (- w) "," h
                               "v" (* sqr2 h)
                               "L" middle-x "," height
                               "l" (- w) "," (- h)
                               "v" (* sqr2 (- h))
                               "z")}]]
     :outline [:<>
               [:path {:d (str "M 0," middle-y
                               "l" w "," (- h)
                               "v" (* sqr2 (- h))
                               "L" middle-x ",0"
                               "l" w "," h
                               "v" (* sqr2 h)
                               "L" width "," middle-y
                               "l" (- w) "," h
                               "v" (* sqr2 h)
                               "L" middle-x "," height
                               "l" (- w) "," (- h)
                               "v" (* sqr2 (- h))
                               "z")}]]}))

(defn vair-in-pale [part-width part-height]
  (let [width part-width
        height part-height
        middle-x (/ width 2)
        w (/ width 4)
        h (/ height (+ 1 1 sqr2))]
    {:width width
     :height height
     :pattern [:<>
               [:path {:d (str "M 0," height
                               "l" w "," (- h)
                               "v" (* sqr2 (- h))
                               "L" middle-x ",0"
                               "l" w "," h
                               "v" (* sqr2 h)
                               "L" width "," height
                               "z")}]]
     :outline [:<>
               [:path {:d (str "M 0,0"
                               "h" width)}]
               [:path {:d (str "M 0," height
                               "l" w "," (- h)
                               "v" (* sqr2 (- h))
                               "L" middle-x ",0"
                               "l" w "," h
                               "v" (* sqr2 h)
                               "L" width "," height)}]
               [:path {:d (str "M 0," height
                               "h" width)}]]}))

(defn vair-en-point [part-width part-height]
  (let [width part-width
        height (* 2 part-height)
        middle-x (/ width 2)
        middle-y (/ height 2)
        w (/ width 4)
        h (/ middle-y (+ 1 1 sqr2))]
    {:width width
     :height height
     :pattern [:<>
               [:path {:d (str "M 0," middle-y
                               "l" w "," (- h)
                               "v" (* sqr2 (- h))
                               "L" middle-x ",0"
                               "l" w "," h
                               "v" (* sqr2 h)
                               "L" width "," middle-y
                               "L" width "," height
                               "l" (- w) "," (- h)
                               "v" (* sqr2 (- h))
                               "L" middle-x "," middle-y
                               "l" (- w) "," h
                               "v" (* sqr2 h)
                               "L 0," height
                               "z")}]]
     :outline [:<>
               [:path {:d (str "M 0," middle-y
                               "l" w "," (- h)
                               "v" (* sqr2 (- h))
                               "L" middle-x ",0"
                               "l" w "," h
                               "v" (* sqr2 h)
                               "L" width "," middle-y)}]
               [:path {:d (str "M " middle-x "," middle-y
                               "l" w "," h
                               "v" (* sqr2 h)
                               "L" width "," height)}]
               [:path {:d (str "M " middle-x "," middle-y
                               "l" (- w) "," h
                               "v" (* sqr2 h)
                               "L 0," height)}]]}))

(defn vair-ancien [part-width part-height]
  (let [width part-width
        height part-height
        dy 0.333
        w (/ width 4)
        h (/ height (+ 1 1 1 (* 2 dy)))]
    {:width width
     :height height
     :pattern [:<>
               [:path {:d (str "M 0,0"
                               "L" width ",0"
                               "L" width "," (- height (* dy h))
                               "a" w " " h " 0 0 1 " (- w) "," (- h)
                               "v" (- h)
                               "a" w " " h " 0 0 0 " (- (* 2 w)) ",0"
                               "v" h
                               "a" w " " h " 0 0 1 " (- w) "," h
                               "z")}]]
     :outline [:<>
               [:path {:d (str "M 0,0"
                               "h" width)}]
               [:path {:d (str "M" width "," (- height (* dy h))
                               "a" w " " h " 0 0 1 " (- w) "," (- h)
                               "v" (- h)
                               "a" w " " h " 0 0 0 " (- (* 2 w)) ",0"
                               "v" h
                               "a" w " " h " 0 0 1 " (- w) "," h)}]
               [:path {:d (str "M 0," height
                               "h" width)}]]}))

(defn render
  {:display-name "Vair"
   :value :vair
   :parts []}
  [{:keys [fields hints] :as division} environment {:keys [render-options]}]
  (let [{:keys [layout]} (options/sanitize division (division-options/options division))
        points (:points environment)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        {:keys [variant
                num-fields-x
                offset-x
                stretch-x
                num-fields-y
                offset-y
                stretch-y]} layout
        offset-x (or offset-x 0)
        stretch-x (or stretch-x 1)
        width (- (:x bottom-right)
                 (:x top-left))
        unstretched-part-width (-> width
                                   (/ num-fields-x))
        part-width (-> unstretched-part-width
                       (* stretch-x))
        offset-y (or offset-y 0)
        stretch-y (or stretch-y 1)
        height (- (:y bottom-right)
                  (:y top-left))
        unstretched-part-height (if num-fields-y
                                  (-> height
                                      (/ num-fields-y))
                                  part-width)
        part-height (-> unstretched-part-height
                        (* stretch-y))
        middle-x (/ width 2)
        origin-x (+ (:x top-left)
                    middle-x)
        pattern-id (util/id "vair")
        vair-function (case variant
                        :counter vair-counter
                        :in-pale vair-in-pale
                        :en-point vair-en-point
                        :ancien vair-ancien
                        vair-default)
        {pattern-width :width
         pattern-height :height
         vair-pattern :pattern
         vair-outline :outline} (vair-function part-width part-height)]
    [:g
     [:defs
      (when (or (:outline? render-options)
                (:outline? hints))
        [:pattern {:id (str pattern-id "-outline")
                   :width pattern-width
                   :height pattern-height
                   :x (+ (* part-width offset-x)
                         (:x top-left)
                         (- middle-x
                            (* origin-x stretch-x)))
                   :y (+ (* part-height offset-y)
                         (:y top-left))
                   :pattern-units "userSpaceOnUse"}
         [:g outline/style
          vair-outline]])
      (for [idx (range 2)]
        ^{:key idx}
        [:pattern {:id (str pattern-id "-" idx)
                   :width pattern-width
                   :height pattern-height
                   :x (+ (* part-width offset-x)
                         (:x top-left)
                         (- middle-x
                            (* origin-x stretch-x)))
                   :y (+ (* part-height offset-y)
                         (:y top-left))
                   :pattern-units "userSpaceOnUse"}
         [:rect {:x 0
                 :y 0
                 :width pattern-width
                 :height pattern-height
                 :fill (get ["#000000" "#ffffff"] idx)}]
         [:g {:fill (get ["#ffffff" "#000000"] idx)}
          vair-pattern]])]
     (for [idx (range 2)]
       (let [mask-id (util/id "mask")
             tincture (-> fields
                          (get idx)
                          :content
                          :tincture)]
         ^{:key idx}
         [:<>
          [:mask {:id mask-id}
           [:rect {:x -500
                   :y -500
                   :width 1100
                   :height 1100
                   :fill (str "url(#" pattern-id "-" idx ")")}]]
          [:rect {:x -500
                  :y -500
                  :width 1100
                  :height 1100
                  :mask (str "url(#" mask-id ")")
                  :fill (tincture/pick tincture render-options)}]]))
     (when (or (:outline? render-options)
               (:outline? hints))
       [:rect {:x -500
               :y -500
               :width 1100
               :height 1100
               :fill (str "url(#" pattern-id "-outline)")}])]))

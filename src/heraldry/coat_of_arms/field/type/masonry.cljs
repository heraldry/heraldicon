(ns heraldry.coat-of-arms.field.type.masonry
  (:require [heraldry.coat-of-arms.field.options :as field-options]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.util :as util]))

(defn masonry-default [part-width part-height thickness]
  (let [width part-width
        height (* 2 part-height)
        middle-x (/ width 2)
        middle-y (/ height 2)
        dx (-> thickness
               (* width)
               (/ 2))
        mask-id (util/id "brick-mask")]
    {:width width
     :height height
     :pattern [:<>
               [:mask {:id mask-id}
                [:rect {:x 0
                        :y 0
                        :width width
                        :height height
                        :fill "#ffffff"}]
                [:path {:d (str "M" dx "," dx
                                "V" (- middle-y dx)
                                "H" (- width dx)
                                "V" dx
                                "z")
                        :fill "#000000"}]]
               [:path {:mask (str "url(#" mask-id ")")
                       :d (str "M 0,0"
                               "H" width
                               "V" (+ middle-y dx)
                               "H" (+ middle-x dx)
                               "V" (- height dx)
                               "H" width
                               "V" height
                               "H" 0
                               "V" (- height dx)
                               "H" (- middle-x dx)
                               "V" (+ middle-y dx)
                               "H 0"
                               "z")}]]
     :outline [:<>
               [:path {:d (str "M" dx "," dx
                               "V" (- middle-y dx)
                               "H" (- width dx)
                               "V" dx
                               "z")}]
               [:path {:d (str "M 0," (+ middle-y dx)
                               "H" (- middle-x dx)
                               "V" (- height dx)
                               "H 0")}]
               [:path {:d (str "M" width "," (+ middle-y dx)
                               "H" (+ middle-x dx)
                               "V" (- height dx)
                               "H" width)}]]}))

(defn render
  {:display-name "Masonry"
   :value :masonry
   :parts []}
  [{:keys [fields hints] :as division} environment {:keys [render-options]}]
  (let [{:keys [layout thickness]} (options/sanitize division (field-options/options division))
        points (:points environment)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        {:keys [num-fields-x
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
                                  (/ part-width 2))
        part-height (-> unstretched-part-height
                        (* stretch-y))
        middle-x (/ width 2)
        origin-x (+ (:x top-left)
                    middle-x)
        pattern-id (util/id "masonry")
        {pattern-width :width
         pattern-height :height
         masonry-pattern :pattern
         masonry-outline :outline} (masonry-default part-width part-height thickness)]
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
          masonry-outline]])
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
                 :fill (get ["#ffffff" "#000000"] idx)}]
         [:g {:fill (get ["#000000" "#ffffff"] idx)}
          masonry-pattern]])]
     (for [idx (range 2)]
       (let [mask-id (util/id "mask")
             tincture (-> fields
                          (get idx)
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

(ns heraldry.coat-of-arms.field.type.fretty
  (:require
   [heraldry.coat-of-arms.field.interface :as field-interface]
   [heraldry.coat-of-arms.outline :as outline]
   [heraldry.coat-of-arms.tincture.core :as tincture]
   [heraldry.interface :as interface]
   [heraldry.util :as util]))

(def field-type :heraldry.field.type/fretty)

(defmethod field-interface/display-name field-type [_] {:en "Fretty"
                                                        :de "Flechtgitter"})

(defmethod field-interface/part-names field-type [_] nil)

(defn fretty-default [part-width part-height thickness gap]
  (let [width (* 2 part-width)
        height (* 2 part-height)
        middle-x (/ width 2)
        middle-y (/ height 2)
        thickness (* thickness width)
        gap (* gap width)
        half-thickness (/ thickness 2)]
    {:width width
     :height height
     :pattern [:<>
               ;; -|-
               [:path {:d (str "M 0,0"
                               "h " (- middle-x half-thickness gap)
                               "v " half-thickness
                               "h " (- (- middle-x half-thickness gap))
                               "z")}]
               [:path {:d (str "M" (- middle-x half-thickness) "," 0
                               "h " thickness
                               "v " (- middle-y half-thickness gap)
                               "h " (- thickness)
                               "z")}]
               [:path {:d (str "M " (+ middle-x half-thickness gap) ",0"
                               "h " (- middle-x half-thickness gap)
                               "v " half-thickness
                               "h " (- (- middle-x half-thickness gap))
                               "z")}]

               ;; |-|
               [:path {:d (str "M 0," (+ half-thickness gap)
                               "h " half-thickness
                               "v " (- height thickness gap gap)
                               "h " (- half-thickness)
                               "z")}]
               [:path {:d (str "M " (+ half-thickness gap) "," (- middle-y half-thickness)
                               "h " (- width thickness gap gap)
                               "v " thickness
                               "h " (- (- width thickness gap gap))
                               "z")}]
               [:path {:d (str "M " width "," (+ half-thickness gap)
                               "h " (- half-thickness)
                               "v " (- height thickness gap gap)
                               "h " half-thickness
                               "z")}]

               ;; -|-
               [:path {:d (str "M 0," height
                               "h " (- middle-x half-thickness gap)
                               "v " (- half-thickness)
                               "h " (- (- middle-x half-thickness gap))
                               "z")}]
               [:path {:d (str "M" (- middle-x half-thickness) "," height
                               "h " thickness
                               "v " (- (- middle-y half-thickness gap))
                               "h " (- thickness)
                               "z")}]
               [:path {:d (str "M " (+ middle-x half-thickness gap) "," height
                               "h " (- middle-x half-thickness gap)
                               "v " (- half-thickness)
                               "h " (- (- middle-x half-thickness gap))
                               "z")}]]

     :outline [:<>
               ;; -|-
               [:path {:d (str "M " (- middle-x half-thickness gap) ",0"
                               "v " half-thickness
                               "h " (- (- middle-x half-thickness gap)))}]
               [:path {:d (str "M" (- middle-x half-thickness) "," 0
                               "v " (- middle-y half-thickness gap)
                               "h " thickness
                               "v " (- (- middle-y half-thickness gap)))}]
               [:path {:d (str "M " (+ middle-x half-thickness gap) ",0"
                               "v " half-thickness
                               "h " (- middle-x half-thickness gap))}]

               ;; |-|


               [:path {:d (str "M 0," (+ half-thickness gap)
                               "h " half-thickness
                               "v " (- height thickness gap gap)
                               "h " (- half-thickness))}]
               [:path {:d (str "M " (+ half-thickness gap) "," (- middle-y half-thickness)
                               "h " (- width thickness gap gap)
                               "v " thickness
                               "h " (- (- width thickness gap gap))
                               "z")}]
               [:path {:d (str "M " width "," (+ half-thickness gap)
                               "h " (- half-thickness)
                               "v " (- height thickness gap gap)
                               "h " half-thickness)}]

               ;; -|-


               [:path {:d (str "M " (- middle-x half-thickness gap) "," height
                               "v " (- half-thickness)
                               "h " (- (- middle-x half-thickness gap)))}]
               [:path {:d (str "M" (- middle-x half-thickness) "," height
                               "v " (- (- middle-y half-thickness gap))
                               "h " thickness
                               "v " (- middle-y half-thickness gap))}]
               [:path {:d (str "M " (+ middle-x half-thickness gap) "," height
                               "v " (- half-thickness)
                               "h " (- middle-x half-thickness gap))}]]}))

(defmethod field-interface/render-field field-type
  [path environment context]
  (let [thickness (interface/get-sanitized-data (conj path :thickness) context)
        gap (/ (interface/get-sanitized-data (conj path :gap) context)
               5)
        num-fields-x (interface/get-sanitized-data (conj path :layout :num-fields-x) context)
        num-fields-y (interface/get-sanitized-data (conj path :layout :num-fields-y) context)
        raw-num-fields-y (interface/get-raw-data (conj path :layout :num-fields-y) context)
        offset-x (interface/get-sanitized-data (conj path :layout :offset-x) context)
        offset-y (interface/get-sanitized-data (conj path :layout :offset-y) context)
        stretch-x (interface/get-sanitized-data (conj path :layout :stretch-x) context)
        stretch-y (interface/get-sanitized-data (conj path :layout :stretch-y) context)
        rotation (+ 45 (interface/get-sanitized-data (conj path :layout :rotation) context))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (conj path :outline?) context))
        points (:points environment)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        width (- (:x bottom-right)
                 (:x top-left))
        unstretched-part-width (-> width
                                   (/ num-fields-x))
        part-width (-> unstretched-part-width
                       (* stretch-x)
                       (* 0.7071067811865476))
        height (- (:y bottom-right)
                  (:y top-left))
        unstretched-part-height (if raw-num-fields-y
                                  (-> height
                                      (/ num-fields-y))
                                  part-width)
        part-height (-> unstretched-part-height
                        (* stretch-y))
        middle-x (/ width 2)
        middle-y (/ height 2)
        shift-x (- middle-x
                   (* middle-x stretch-x))
        shift-y (- middle-y
                   (* middle-y stretch-y))
        pattern-id-prefix (util/id "fretty")
        {pattern-width :width
         pattern-height :height
         fretty-pattern :pattern
         fretty-outline :outline} (fretty-default part-width part-height thickness gap)]
    [:g {:transform (str "rotate(" (- rotation) ")")}
     [:defs
      (when outline?
        [:pattern {:id (str pattern-id-prefix "-outline")
                   :width pattern-width
                   :height pattern-height
                   :x (+ (:x top-left)
                         (* part-width offset-x)
                         shift-x)
                   :y (+ (:y top-left)
                         (* part-height offset-y)
                         shift-y)
                   :pattern-units "userSpaceOnUse"}
         [:g (outline/style context)
          fretty-outline]])
      (for [idx (range 2)]
        ^{:key idx}
        [:pattern {:id (str pattern-id-prefix "-" idx)
                   :width pattern-width
                   :height pattern-height
                   :x (+ (:x top-left)
                         (* part-width offset-x)
                         shift-x)
                   :y (+ (:y top-left)
                         (* part-height offset-y)
                         shift-y)
                   :pattern-units "userSpaceOnUse"}
         [:rect {:x 0
                 :y 0
                 :width pattern-width
                 :height pattern-height
                 :fill (get ["#ffffff" "#000000"] idx)}]
         [:g {:fill (get ["#000000" "#ffffff"] idx)}
          fretty-pattern]])]
     (doall
      (for [idx (range 2)]
        (let [mask-id (util/id "mask")]
          ^{:key idx}
          [:<>
           [:mask {:id mask-id}
            [:rect {:x -500
                    :y -500
                    :width 1100
                    :height 1100
                    :fill (str "url(#" pattern-id-prefix "-" idx ")")}]]
           [tincture/tinctured-field
            (conj path :fields idx :tincture) context
            :mask-id mask-id]])))
     (when outline?
       [:rect {:x -500
               :y -500
               :width 1100
               :height 1100
               :fill (str "url(#" pattern-id-prefix "-outline)")}])]))

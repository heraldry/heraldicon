(ns heraldry.coat-of-arms.field.type.potenty
  (:require [heraldry.coat-of-arms.field.interface :as field-interface]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.interface :as interface]
            [heraldry.util :as util]))

(def field-type :heraldry.field.type/potenty)

(defmethod field-interface/display-name field-type [_] "Potenty")

(defmethod field-interface/part-names field-type [_] nil)

(defn units [n]
  (-> n
      (* 4)
      (- 1)))

(defn potent-default [part-width part-height]
  (let [width part-width
        height (* 2 part-height)
        middle-x (/ width 2)
        middle-y (/ height 2)
        w (/ width 4)
        h (/ middle-y 2)]
    {:width width
     :height height
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
  (let [width part-width
        height (* 2 part-height)
        middle-y (/ height 2)
        w (/ width 4)
        h (/ middle-y 2)]
    {:width width
     :height height
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
  (let [width part-width
        height (* 2 part-height)
        middle-y (/ height 2)
        w (/ width 4)
        h (/ middle-y 2)]
    {:width width
     :height height
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
  (let [width part-width
        height (* 2 part-height)
        middle-x (/ width 2)
        middle-y (/ height 2)
        w (/ width 4)
        h (/ middle-y 2)]
    {:width width
     :height height
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

(defmethod field-interface/render-field field-type
  [path environment context]
  (let [variant (interface/get-sanitized-data (conj path :variant) context)
        num-fields-x (interface/get-sanitized-data (conj path :layout :num-fields-x) context)
        num-fields-y (interface/get-sanitized-data (conj path :layout :num-fields-y) context)
        raw-num-fields-y (interface/get-raw-data (conj path :layout :num-fields-y) context)
        offset-x (interface/get-sanitized-data (conj path :layout :offset-x) context)
        offset-y (interface/get-sanitized-data (conj path :layout :offset-y) context)
        stretch-x (interface/get-sanitized-data (conj path :layout :stretch-x) context)
        stretch-y (interface/get-sanitized-data (conj path :layout :stretch-y) context)
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (conj path :outline?) context))
        points (:points environment)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        width (- (:x bottom-right)
                 (:x top-left))
        unstretched-part-width (-> width
                                   (/ (units num-fields-x))
                                   (* 4))
        part-width (-> unstretched-part-width
                       (* stretch-x))
        height (- (:y bottom-right)
                  (:y top-left))
        unstretched-part-height (if raw-num-fields-y
                                  (-> height
                                      (/ num-fields-y))
                                  (-> part-width
                                      (/ 2)))
        part-height (-> unstretched-part-height
                        (* stretch-y))
        middle-x (/ width 2)
        middle-y (/ height 2)
        shift-x (- middle-x
                   (* middle-x stretch-x))
        shift-y (- middle-y
                   (* middle-y stretch-y))
        pattern-id (util/id "potenty")
        potent-function (case variant
                          :counter potent-counter
                          :in-pale potent-in-pale
                          :en-point potent-en-point
                          potent-default)
        {pattern-width :width
         pattern-height :height
         potent-pattern :pattern
         potent-outline :outline} (potent-function part-width part-height)]
    [:g
     [:defs
      (when outline?
        [:pattern {:id (str pattern-id "-outline")
                   :width pattern-width
                   :height pattern-height
                   :x (+ (:x top-left)
                         (* part-width offset-x)
                         shift-x)
                   :y (+ (:y top-left)
                         (* part-height offset-y)
                         shift-y)
                   :pattern-units "userSpaceOnUse"}
         [:g outline/style
          potent-outline]])
      (doall
       (for [idx (range 2)]
         ^{:key idx}
         [:pattern {:id (str pattern-id "-" idx)
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
                  :fill (get ["#000000" "#ffffff"] idx)}]
          [:g {:fill (get ["#ffffff" "#000000"] idx)}
           potent-pattern]]))]
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
                    :fill (str "url(#" pattern-id "-" idx ")")}]]
           [tincture/tinctured-field
            (conj path :fields idx :tincture)
            context
            :mask-id mask-id]])))
     (when outline?
       [:rect {:x -500
               :y -500
               :width 1100
               :height 1100
               :fill (str "url(#" pattern-id "-outline)")}])]))

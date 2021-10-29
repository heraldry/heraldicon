(ns heraldry.coat-of-arms.field.type.masony
  (:require
   [heraldry.coat-of-arms.field.interface :as field-interface]
   [heraldry.coat-of-arms.outline :as outline]
   [heraldry.coat-of-arms.tincture.core :as tincture]
   [heraldry.interface :as interface]
   [heraldry.util :as util]))

(def field-type :heraldry.field.type/masony)

(defmethod field-interface/display-name field-type [_] {:en "Masony"
                                                        :de "Gemauert"})

(defmethod field-interface/part-names field-type [_] nil)

(defn masony-default [part-width part-height thickness]
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

(defmethod field-interface/render-field field-type
  [path environment context]
  (let [thickness (interface/get-sanitized-data (conj path :thickness) context)
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
                                   (/ num-fields-x))
        part-width (-> unstretched-part-width
                       (* stretch-x))
        height (- (:y bottom-right)
                  (:y top-left))
        unstretched-part-height (if raw-num-fields-y
                                  (-> height
                                      (/ num-fields-y))
                                  (/ part-width 2))
        part-height (-> unstretched-part-height
                        (* stretch-y))
        middle-x (/ width 2)
        middle-y (/ height 2)
        shift-x (- middle-x
                   (* middle-x stretch-x))
        shift-y (- middle-y
                   (* middle-y stretch-y))
        pattern-id (util/id "masony")
        {pattern-width :width
         pattern-height :height
         masony-pattern :pattern
         masony-outline :outline} (masony-default part-width part-height thickness)]
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
         [:g (outline/style context)
          masony-outline]])
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
                  :fill (get ["#ffffff" "#000000"] idx)}]
          [:g {:fill (get ["#000000" "#ffffff"] idx)}
           masony-pattern]]))]
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

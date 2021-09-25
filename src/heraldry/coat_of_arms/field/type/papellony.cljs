(ns heraldry.coat-of-arms.field.type.papellony
  (:require [heraldry.coat-of-arms.field.interface :as field-interface]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.interface :as interface]
            [heraldry.util :as util]))

(def field-type :heraldry.field.type/papellony)

(defmethod field-interface/display-name field-type [_] {:en "Papellony"
                                                        :de "Bogenschuppenfeh"})

(defmethod field-interface/part-names field-type [_] nil)

(defn papellony-default [part-width part-height thickness]
  (let [width part-width
        height (* 2 part-height)
        middle-x (/ width 2)
        middle-y (/ height 2)
        thickness (* thickness width)
        extra (-> (- 1 (/ (* thickness thickness)
                          (* middle-x middle-x)))
                  Math/sqrt
                  (* middle-y)
                  (->> (- middle-y)))]
    {:width width
     :height height
     :pattern [:<>
               [:path {:d (str "M 0,0"
                               "a" middle-x " " middle-y " 0 0 0 " width " 0"
                               "v" (- extra)
                               "h" (- thickness)
                               "v" extra
                               "a" (- middle-x thickness) " " (- middle-y thickness) " 0 0 1 "
                               (- (- width
                                     (* 2 thickness))) " 0"
                               "v" (- extra)
                               "h" (- thickness)
                               "v" extra
                               "z")}]
               [:path {:d (str "M" (- middle-x) "," middle-y
                               "a" middle-x " " middle-y " 0 0 0 " width " 0"
                               "v" (- extra)
                               "h" (- thickness)
                               "v" extra
                               "a" (- middle-x thickness) " " (- middle-y thickness) " 0 0 1 "
                               (- (- width
                                     (* 2 thickness))) " 0"
                               "v" (- extra)
                               "h" (- thickness)
                               "v" extra
                               "z")}]
               [:path {:d (str "M" middle-x "," middle-y
                               "a" middle-x " " middle-y " 0 0 0 " width " 0"
                               "v" (- extra)
                               "h" (- thickness)
                               "v" extra
                               "a" (- middle-x thickness) " " (- middle-y thickness) " 0 0 1 "
                               (- (- width
                                     (* 2 thickness))) " 0"
                               "v" (- extra)
                               "h" (- thickness)
                               "v" extra
                               "z")}]
               [:path {:d (str "M 0," height
                               "v" (- extra)
                               "h" thickness
                               "v" extra
                               "z")}]
               [:path {:d (str "M" width "," height
                               "v" (- extra)
                               "h" (- thickness)
                               "v" extra
                               "z")}]]

     :outline [:<>
               [:path {:d (str "M 0,0"
                               "a" middle-x " " middle-y " 0 0 0 " width " 0")}]
               [:path {:d (str "M" (- width thickness) ",0"
                               "a" (- middle-x thickness) " " (- middle-y thickness) " 0 0 1 "
                               (- (- width
                                     (* 2 thickness))) " 0")}]
               [:path {:d (str "M" (- middle-x) "," middle-y
                               "a" middle-x " " middle-y " 0 0 0 " width " 0")}]
               [:path {:d (str "M" (- middle-x thickness) "," middle-y
                               "a" (- middle-x thickness) " " (- middle-y thickness) " 0 0 1 "
                               (- (- width
                                     (* 2 thickness))) " 0")}]
               [:path {:d (str "M" middle-x "," middle-y
                               "a" middle-x " " middle-y " 0 0 0 " width " 0")}]
               [:path {:d (str "M" (+ middle-x
                                      width
                                      (- thickness)) "," middle-y
                               "a" (- middle-x thickness) " " (- middle-y thickness) " 0 0 1 "
                               (- (- width
                                     (* 2 thickness))) " 0")}]
               [:path {:d (str "M" (- middle-x) "," (- middle-y)
                               "a" middle-x " " middle-y " 0 0 0 " width " 0")}]
               [:path {:d (str "M" middle-x "," (- middle-y)
                               "a" middle-x " " middle-y " 0 0 0 " width " 0")}]
               [:path {:d (str "M" (- middle-x thickness) "," middle-y
                               "v" (- extra))}]
               [:path {:d (str "M" (+ middle-x thickness) "," middle-y
                               "v" (- extra))}]
               [:path {:d (str "M" thickness "," height
                               "v" (- extra))}]
               [:path {:d (str "M" (- width thickness) "," height
                               "v" (- extra))}]]}))

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
        pattern-id-prefix (util/id "papellony")
        {pattern-width :width
         pattern-height :height
         papellony-pattern :pattern
         papellony-outline :outline} (papellony-default part-width part-height thickness)]
    [:g
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
          papellony-outline]])
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
          papellony-pattern]])]
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

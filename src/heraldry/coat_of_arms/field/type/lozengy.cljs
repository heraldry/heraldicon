(ns heraldry.coat-of-arms.field.type.lozengy
  (:require [heraldry.coat-of-arms.field.interface :as field-interface]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.interface :as interface]
            [heraldry.util :as util]))

(def field-type :heraldry.field.type/lozengy)

(defmethod field-interface/display-name field-type [_] "Lozengy")

(defmethod field-interface/part-names field-type [_] nil)

(defmethod field-interface/render-field field-type
  [path environment context]
  (let [num-fields-x (interface/get-sanitized-data (conj path :layout :num-fields-x) context)
        num-fields-y (interface/get-sanitized-data (conj path :layout :num-fields-y) context)
        raw-num-fields-y (interface/get-raw-data (conj path :layout :num-fields-y) context)
        offset-x (interface/get-sanitized-data (conj path :layout :offset-x) context)
        offset-y (interface/get-sanitized-data (conj path :layout :offset-y) context)
        stretch-x (interface/get-sanitized-data (conj path :layout :stretch-x) context)
        stretch-y (interface/get-sanitized-data (conj path :layout :stretch-y) context)
        rotation (interface/get-sanitized-data (conj path :layout :rotation) context)
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
                                  part-width)
        part-height (-> unstretched-part-height
                        (* stretch-y))
        middle-x (/ width 2)
        middle-y (/ height 2)
        shift-x (- middle-x
                   (* middle-x stretch-x))
        shift-y (- middle-y
                   (* middle-y stretch-y))
        pattern-id (util/id "lozengy")
        lozenge-shape (svg/make-path ["M" [(/ part-width 2) 0]
                                      "L" [part-width (/ part-height 2)]
                                      "L" [(/ part-width 2) part-height]
                                      "L" [0 (/ part-height 2)]
                                      "z"])]
    [:g
     [:defs
      (when outline?
        [:pattern {:id (str pattern-id "-outline")
                   :width part-width
                   :height part-height
                   :x (+ (:x top-left)
                         (* part-width offset-x)
                         shift-x)
                   :y (+ (:y top-left)
                         (* part-height offset-y)
                         shift-y)
                   :pattern-units "userSpaceOnUse"}
         [:g outline/style
          [:path {:d lozenge-shape}]]])
      [:pattern {:id (str pattern-id "-0")
                 :width part-width
                 :height part-height
                 :x (+ (:x top-left)
                       (* part-width offset-x)
                       shift-x)
                 :y (+ (:y top-left)
                       (* part-height offset-y)
                       shift-y)
                 :pattern-units "userSpaceOnUse"}
       [:rect {:x 0
               :y 0
               :width part-width
               :height part-height
               :fill "#000000"}]
       [:path {:d lozenge-shape
               :fill "#ffffff"}]]
      [:pattern {:id (str pattern-id "-1")
                 :width part-width
                 :height part-height
                 :x (+ (:x top-left)
                       (* part-width offset-x)
                       shift-x)
                 :y (+ (:y top-left)
                       (* part-height offset-y)
                       shift-y)
                 :pattern-units "userSpaceOnUse"}
       [:rect {:x 0
               :y 0
               :width part-width
               :height part-height
               :fill "#ffffff"}]
       [:path {:d lozenge-shape
               :fill "#000000"}]]]
     [:g {:transform (str "rotate(" (- rotation) ")")}
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
             :mask-id mask-id
             :transform (str "rotate(" rotation ")")]])))
      (when outline?
        [:rect {:x -500
                :y -500
                :width 1100
                :height 1100
                :fill (str "url(#" pattern-id "-outline)")}])]]))

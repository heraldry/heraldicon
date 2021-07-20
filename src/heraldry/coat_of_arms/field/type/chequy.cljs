(ns heraldry.coat-of-arms.field.type.chequy
  (:require [heraldry.coat-of-arms.field.interface :as interface]
            [heraldry.options :as options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.util :as util]))

(def field-type
  :heraldry.field.type/chequy)

(defmethod interface/display-name field-type [_] "Chequy")

(defmethod interface/part-names field-type [_] nil)

(defmethod interface/render-field field-type
  [path environment context]
  (let [num-base-fields (options/sanitized-value (conj path :layout :num-base-fields) context)
        num-fields-x (options/sanitized-value (conj path :layout :num-fields-x) context)
        num-fields-y (options/sanitized-value (conj path :layout :num-fields-y) context)
        raw-num-fields-y (options/raw-value (conj path :layout :num-fields-y) context)
        offset-x (options/sanitized-value (conj path :layout :offset-x) context)
        offset-y (options/sanitized-value (conj path :layout :offset-y) context)
        stretch-x (options/sanitized-value (conj path :layout :stretch-x) context)
        stretch-y (options/sanitized-value (conj path :layout :stretch-y) context)
        outline? (or (options/render-option :outline? context)
                     (options/sanitized-value (conj path :outline?) context))
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
        origin-x (+ (:x top-left)
                    middle-x)
        pattern-id (util/id "chequy")]
    [:g
     [:defs
      (when outline?
        [:pattern {:id (str pattern-id "-outline")
                   :width part-width
                   :height part-height
                   :x (+ (* part-width offset-x)
                         (:x top-left)
                         (- middle-x
                            (* origin-x stretch-x)))
                   :y (+ (* part-height offset-y)
                         (:y top-left))
                   :pattern-units "userSpaceOnUse"}
         [:g outline/style
          [:path {:d (str "M 0,0 h " part-width)}]
          [:path {:d (str "M 0,0 v " part-height)}]
          [:path {:d (str "M 0," part-height " h " part-width)}]
          [:path {:d (str "M " part-width ",0 v " part-height)}]]])
      (doall
       (for [idx (range num-base-fields)]
         ^{:key idx}
         [:pattern {:id (str pattern-id "-" idx)
                    :width (* part-width num-base-fields)
                    :height (* part-height num-base-fields)
                    :x (+ (* part-width offset-x)
                          (:x top-left)
                          (- middle-x
                             (* origin-x stretch-x)))
                    :y (+ (* part-height offset-y)
                          (:y top-left))
                    :pattern-units "userSpaceOnUse"}
          [:rect {:x 0
                  :y 0
                  :width (* part-width num-base-fields)
                  :height (* part-height num-base-fields)
                  :fill "#000000"}]
          (for [j (range num-base-fields)
                i (range num-base-fields)]
            (when (-> i (+ j) (mod num-base-fields) (= idx))
              ^{:key [i j]}
              [:rect {:x (* i part-width)
                      :y (* j part-height)
                      :width part-width
                      :height part-height
                      :fill "#ffffff"}]))]))]
     (doall
      (for [idx (range num-base-fields)]
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

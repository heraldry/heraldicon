(ns heraldry.coat-of-arms.division.type.lozengy
  (:require [heraldry.coat-of-arms.division.options :as division-options]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.tincture :as tincture]
            [heraldry.util :as util]))

(defn render
  {:display-name "Lozengy"
   :value :lozengy
   :parts []}
  [{:keys [fields hints] :as division} environment {:keys [render-options]}]
  (let [{:keys [layout]} (options/sanitize division (division-options/options division))
        points (:points environment)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        {:keys [num-fields-x
                offset-x
                stretch-x
                num-fields-y
                offset-y
                stretch-y
                rotation]} layout
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
        pattern-id (util/id "lozengy")
        lozenge-shape (svg/make-path ["M" [(/ part-width 2) 0]
                                      "L" [part-width (/ part-height 2)]
                                      "L" [(/ part-width 2) part-height]
                                      "L" [0 (/ part-height 2)]
                                      "z"])]
    [:g
     [:defs
      (when (or (:outline? render-options)
                (:outline? hints))
        [:pattern {:id (str pattern-id "-outline")
                   :width part-width
                   :height part-height
                   :x (+ (* part-width offset-x)
                         (:x top-left)
                         (- middle-x
                            (* middle-x stretch-x)))
                   :y (+ (* part-height offset-y)
                         (:y top-left))
                   :pattern-units "userSpaceOnUse"}
         [:g outline/style
          [:path {:d lozenge-shape}]]])
      [:pattern {:id (str pattern-id "-0")
                 :width part-width
                 :height part-height
                 :x (+ (* part-width offset-x)
                       (:x top-left)
                       (- middle-x
                          (* middle-x stretch-x)))
                 :y (+ (* part-height offset-y)
                       (:y top-left))
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
                 :x (+ (* part-width offset-x)
                       (:x top-left)
                       (- middle-x
                          (* middle-x stretch-x)))
                 :y (+ (* part-height offset-y)
                       (:y top-left))
                 :pattern-units "userSpaceOnUse"}
       [:rect {:x 0
               :y 0
               :width part-width
               :height part-height
               :fill "#ffffff"}]
       [:path {:d lozenge-shape
               :fill "#000000"}]]]
     [:g {:transform (str "rotate(" (- rotation) ")")}
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
           [:g {:mask (str "url(#" mask-id ")")}
            [:rect {:x -500
                    :y -500
                    :width 1100
                    :height 1100
                    :transform (str "rotate(" rotation ")")
                    :fill (tincture/pick tincture render-options)}]]]))
      (when (or (:outline? render-options)
                (:outline? hints))
        [:rect {:x -500
                :y -500
                :width 1100
                :height 1100
                :fill (str "url(#" pattern-id "-outline)")}])]]))

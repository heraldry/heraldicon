(ns heraldry.coat-of-arms.field.type.papellony
  (:require [heraldry.coat-of-arms.field.options :as field-options]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.util :as util]))

(defn papellony-default [part-width part-height thickness]
  (let [width     part-width
        height    (* 2 part-height)
        middle-x  (/ width 2)
        middle-y  (/ height 2)
        thickness (* thickness width)
        extra     (-> (- 1 (/ (* thickness thickness)
                              (* middle-x middle-x)))
                      Math/sqrt
                      (* middle-y)
                      (->> (- middle-y)))]
    {:width   width
     :height  height
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

(defn render
  {:display-name "Papellony"
   :value         :heraldry.field.type/papellony
   :parts        []}
  [{:keys [fields hints] :as field} environment {:keys [render-options]}]
  (let [{:keys [layout thickness]}   (options/sanitize field (field-options/options field))
        points                       (:points environment)
        top-left                     (:top-left points)
        bottom-right                 (:bottom-right points)
        {:keys [num-fields-x
                offset-x
                stretch-x
                num-fields-y
                offset-y
                stretch-y]}          layout
        offset-x                     (or offset-x 0)
        stretch-x                    (or stretch-x 1)
        width                        (- (:x bottom-right)
                                        (:x top-left))
        unstretched-part-width       (-> width
                                         (/ num-fields-x))
        part-width                   (-> unstretched-part-width
                                         (* stretch-x))
        offset-y                     (or offset-y 0)
        stretch-y                    (or stretch-y 1)
        height                       (- (:y bottom-right)
                                        (:y top-left))
        unstretched-part-height      (if num-fields-y
                                       (-> height
                                           (/ num-fields-y))
                                       (/ part-width 2))
        part-height                  (-> unstretched-part-height
                                         (* stretch-y))
        middle-x                     (/ width 2)
        origin-x                     (+ (:x top-left)
                                        middle-x)
        pattern-id                   (util/id "papellony")
        {pattern-width     :width
         pattern-height    :height
         papellony-pattern :pattern
         papellony-outline :outline} (papellony-default part-width part-height thickness)]
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
          papellony-outline]])
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
                 :fill   (get ["#ffffff" "#000000"] idx)}]
         [:g {:fill (get ["#000000" "#ffffff"] idx)}
          papellony-pattern]])]
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


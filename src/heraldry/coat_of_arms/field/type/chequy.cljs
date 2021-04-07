(ns heraldry.coat-of-arms.field.type.chequy
  (:require [heraldry.coat-of-arms.field.options :as field-options]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.util :as util]))

(defn render
  {:display-name "Chequy"
   :value         :heraldry.field.type/chequy
   :parts        []}
  [{:keys [fields hints] :as field} environment {:keys [render-options]}]
  (let [{:keys [layout]}        (options/sanitize field (field-options/options field))
        points                  (:points environment)
        top-left                (:top-left points)
        bottom-right            (:bottom-right points)
        {:keys [num-base-fields
                num-fields-x
                offset-x
                stretch-x
                num-fields-y
                offset-y
                stretch-y]}     layout
        offset-x                (or offset-x 0)
        stretch-x               (or stretch-x 1)
        width                   (- (:x bottom-right)
                                   (:x top-left))
        unstretched-part-width  (-> width
                                    (/ num-fields-x))
        part-width              (-> unstretched-part-width
                                    (* stretch-x))
        offset-y                (or offset-y 0)
        stretch-y               (or stretch-y 1)
        height                  (- (:y bottom-right)
                                   (:y top-left))
        unstretched-part-height (if num-fields-y
                                  (-> height
                                      (/ num-fields-y))
                                  part-width)
        part-height             (-> unstretched-part-height
                                    (* stretch-y))
        middle-x                (/ width 2)
        origin-x                (+ (:x top-left)
                                   middle-x)
        pattern-id              (util/id "chequy")]
    [:g
     [:defs
      (when (or (:outline? render-options)
                (:outline? hints))
        [:pattern {:id            (str pattern-id "-outline")
                   :width         part-width
                   :height        part-height
                   :x             (+ (* part-width offset-x)
                                     (:x top-left)
                                     (- middle-x
                                        (* origin-x stretch-x)))
                   :y             (+ (* part-height offset-y)
                                     (:y top-left))
                   :pattern-units "userSpaceOnUse"}
         [:g outline/style
          [:path {:d (str "M 0,0 h " part-width)}]
          [:path {:d (str "M 0,0 v " part-height)}]
          [:path {:d (str "M 0," part-height " h " part-width)}]
          [:path {:d (str "M " part-width ",0 v " part-height)}]]])
      (for [idx (range num-base-fields)]
        ^{:key idx}
        [:pattern {:id            (str pattern-id "-" idx)
                   :width         (* part-width num-base-fields)
                   :height        (* part-height num-base-fields)
                   :x             (+ (* part-width offset-x)
                                     (:x top-left)
                                     (- middle-x
                                        (* origin-x stretch-x)))
                   :y             (+ (* part-height offset-y)
                                     (:y top-left))
                   :pattern-units "userSpaceOnUse"}
         [:rect {:x      0
                 :y      0
                 :width  (* part-width num-base-fields)
                 :height (* part-height num-base-fields)
                 :fill   "#000000"}]
         (for [j (range num-base-fields)
               i (range num-base-fields)]
           (when (-> i (+ j) (mod num-base-fields) (= idx))
             ^{:key [i j]}
             [:rect {:x      (* i part-width)
                     :y      (* j part-height)
                     :width  part-width
                     :height part-height
                     :fill   "#ffffff"}]))])]
     (for [idx (range num-base-fields)]
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


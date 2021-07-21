(ns heraldry.coat-of-arms.semy.core
  (:require [heraldry.coat-of-arms.charge.interface :as charge-interface]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.interface :as interface]
            [heraldry.options :as options]
            [heraldry.util :as util]))

(defn shift-environment [environment point]
  (-> environment
      (update :points (fn [points]
                        (->> points
                             (map (fn [[k v]]
                                    [k (v/+ v point)]))
                             (into {}))))))

(defmethod interface/render-component :heraldry.component/semy [path parent-path environment context]
  (let [points (:points environment)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        num-fields-x (interface/get-sanitized-data (conj path :layout :num-fields-x) context)
        num-fields-y (interface/get-sanitized-data (conj path :layout :num-fields-y) context)
        raw-num-fields-y (interface/get-raw-data (conj path :layout :num-fields-y) context)
        offset-x (interface/get-sanitized-data (conj path :layout :offset-x) context)
        offset-y (interface/get-sanitized-data (conj path :layout :offset-y) context)
        stretch-x (interface/get-sanitized-data (conj path :layout :stretch-x) context)
        stretch-y (interface/get-sanitized-data (conj path :layout :stretch-y) context)
        rotation (interface/get-sanitized-data (conj path :layout :rotation) context)
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
        unstretched-part-height (if raw-num-fields-y
                                  (-> height
                                      (/ num-fields-y))
                                  part-width)
        part-height (-> unstretched-part-height
                        (* stretch-y))
        middle-x (/ width 2)
        pattern-id (util/id "semy")
        part-width-half (/ part-width 2)
        part-height-half (/ part-height 2)
        charge-environment {:width part-width
                            :height part-height
                            :points {:top-left {:x (- part-width-half) :y (- part-height-half)}
                                     :top {:x 0 :y (- part-height-half)}
                                     :top-right {:x part-width-half :y (- part-height-half)}
                                     :left {:x (- part-width-half) :y 0}
                                     :fess {:x 0 :y 0}
                                     :right {:x part-width-half :y 0}
                                     :bottom-left {:x (- part-width-half) :y part-height-half}
                                     :bottom {:x 0 :y part-height-half}
                                     :bottom-right {:x part-width-half :y part-height-half}}}
        charge-path (conj path :charge)
        charge-context (-> context
                           (assoc :size-default 50))]
    [:g
     [:defs
      [:pattern {:id pattern-id
                 :width part-width
                 :height part-height
                 :x (+ (* part-width offset-x)
                       (:x top-left)
                       (- middle-x
                          (* middle-x stretch-x)))
                 :y (+ (* part-height offset-y)
                       (:y top-left))
                 :pattern-units "userSpaceOnUse"}
       (doall
        (for [[idx shift] (map-indexed
                           vector
                           [(v/v 0 0)
                            {:x part-width :y 0}
                            {:x 0 :y part-height}
                            {:x part-width :y part-height}
                            {:x part-width-half :y part-height-half}])]
          ^{:key idx}
          [charge-interface/render-charge
           charge-path
           path
           (shift-environment charge-environment shift)
           (assoc charge-context :origin-override shift)]))]]
     [:g {:transform (str "rotate(" (- rotation) ")")}
      [:rect {:x -500
              :y -500
              :width 1100
              :height 1100
              :fill (str "url(#" pattern-id ")")}]]]))

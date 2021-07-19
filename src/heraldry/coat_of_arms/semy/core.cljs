(ns heraldry.coat-of-arms.semy.core
  (:require [heraldry.coat-of-arms.charge.core :as charge]
            [heraldry.options :as options]
            [heraldry.coat-of-arms.semy.options :as semy-options]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(defn shift-environment [environment point]
  (-> environment
      (update :points (fn [points]
                        (->> points
                             (map (fn [[k v]]
                                    [k (v/+ v point)]))
                             (into {}))))))

(defn render
  [{:keys [charge] :as semy} environment context]
  (let [{:keys [layout]} (options/sanitize semy semy-options/default-options)
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
        raw-num-fields-y (-> semy :layout :num-fields-y)
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
        charge (-> charge
                   (assoc-in [:origin :point] :fess)
                   (update-in [:geometry :size] #(or % 50)))]
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
       [charge/render
        charge
        nil
        charge-environment
        context]
       [charge/render
        charge
        nil
        (shift-environment charge-environment {:x part-width :y 0})
        context]
       [charge/render
        charge
        nil
        (shift-environment charge-environment {:x 0 :y part-height})
        context]
       [charge/render
        charge
        nil
        (shift-environment charge-environment {:x part-width :y part-height})
        context]
       [charge/render
        charge
        nil
        (shift-environment charge-environment {:x part-width-half :y part-height-half})
        context]]]
     [:g {:transform (str "rotate(" (- rotation) ")")}
      [:rect {:x -500
              :y -500
              :width 1100
              :height 1100
              :fill (str "url(#" pattern-id ")")}]]]))

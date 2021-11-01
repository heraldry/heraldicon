(ns heraldry.coat-of-arms.semy.core
  (:require
   [heraldry.coat-of-arms.charge.interface :as charge-interface]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.math.vector :as v]
   [heraldry.util :as util]))

(defn shift-environment [environment point]
  (-> environment
      (update :points (fn [points]
                        (->> points
                             (map (fn [[k v]]
                                    [k (v/add v point)]))
                             (into {}))))))

(defmethod interface/render-component :heraldry.component/semy [context]
  (let [environment (:environment context)
        points (:points environment)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        rectangular? (interface/get-sanitized-data (c/++ context :rectangular?))
        num-fields-x (interface/get-sanitized-data (c/++ context :layout :num-fields-x))
        num-fields-y (interface/get-sanitized-data (c/++ context :layout :num-fields-y))
        raw-num-fields-y (interface/get-raw-data (c/++ context :layout :num-fields-y))
        offset-x (interface/get-sanitized-data (c/++ context :layout :offset-x))
        offset-y (interface/get-sanitized-data (c/++ context :layout :offset-y))
        stretch-x (interface/get-sanitized-data (c/++ context :layout :stretch-x))
        stretch-y (interface/get-sanitized-data (c/++ context :layout :stretch-y))
        rotation (interface/get-sanitized-data (c/++ context :layout :rotation))
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
        middle-y (/ height 2)
        shift-x (- middle-x
                   (* middle-x stretch-x))
        shift-y (- middle-y
                   (* middle-y stretch-y))
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
        charge-context (-> context
                           (c/++ :charge)
                           (assoc :size-default 50)
                           (assoc :environment charge-environment))]
    [:g
     [:defs
      [:pattern {:id pattern-id
                 :width part-width
                 :height part-height
                 :x (+ (:x top-left)
                       (* part-width offset-x)
                       shift-x)
                 :y (+ (:y top-left)
                       (* part-height offset-y)
                       shift-y)
                 :pattern-units "userSpaceOnUse"}
       (doall
        (for [[idx shift] (map-indexed
                           vector
                           (cond->
                             [(v/v 0 0)
                              {:x part-width :y 0}
                              {:x 0 :y part-height}
                              {:x part-width :y part-height}]
                             (not rectangular?) (conj {:x part-width-half :y part-height-half})))]
          ^{:key idx}
          [charge-interface/render-charge
           (-> charge-context
               (assoc :origin-override shift)
               (update :environment shift-environment shift))]))]]
     [:g {:transform (str "rotate(" (- rotation) ")")}
      [:rect {:x -500
              :y -500
              :width 1100
              :height 1100
              :fill (str "url(#" pattern-id ")")}]]]))

(defmethod interface/blazon-component :heraldry.component/semy [context]
  (util/str-tr "semy of " (interface/blazon
                           (-> context
                               (c/++ :charge)
                               (assoc-in [:blazonry :drop-article?] true)))))

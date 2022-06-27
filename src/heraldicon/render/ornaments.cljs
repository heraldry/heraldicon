(ns heraldicon.render.ornaments
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.render.motto :as motto]))

(defn- elements [context & {:keys [below-shield?]}]
  (into [:<>]
        (map (fn [[idx self-below-shield?]]
               (let [updated-context (-> context
                                         (c/++ idx)
                                         (assoc :auto-resize? false
                                                :self-below-shield? self-below-shield?
                                                :render-pass-below-shield? below-shield?))
                     motto? (interface/motto? updated-context)]
                 ^{:key idx}
                 [:<>
                  (if motto?
                    (:result (motto/render updated-context))
                    [interface/render-component updated-context])])))
        (interface/get-element-indices context)))

(defn render [context coa-bounding-box]
  (let [{bb-min-x :min-x
         bb-max-x :max-x
         bb-min-y :min-y
         bb-max-y :max-y} coa-bounding-box
        num-ornaments (count (interface/get-element-indices (c/++ context :elements)))
        width (- bb-max-x bb-min-x)
        height (- bb-max-y bb-min-y)
        ornaments-width (* 3 1.2 width)
        ornaments-height (* 2 1.2 height)
        ornaments-left (-> width
                           (/ 2)
                           (- (/ ornaments-width 2))
                           (+ bb-min-x))
        ornaments-top (-> height
                          (/ 2)
                          (- (/ ornaments-height 2))
                          (+ bb-min-y))
        environment (environment/create
                     nil
                     {:bounding-box coa-bounding-box})
        updated-context (-> context
                            (c/++ :elements)
                            (assoc :environment environment))]
    (if (pos? num-ornaments)
      {:result-below-shield [elements updated-context :below-shield? true]
       :result-above-shield [elements updated-context :below-shield? false]

       :bounding-box (bb/BoundingBox.
                      ornaments-left
                      (+ ornaments-left ornaments-width)
                      ornaments-top
                      (+ ornaments-top ornaments-height))}
      {:bounding-box (bb/BoundingBox. 0 0 0 0)})))

(ns heraldicon.heraldry.ornaments
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.shield-separator :as shield-separator]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]))

(defmethod interface/properties :heraldry/ornaments [_context]
  {:type :heraldry/ornaments})

(defmethod interface/environment :heraldry/ornaments [context _properties]
  (interface/get-parent-environment context))

(defmethod interface/bounding-box :heraldry/ornaments [context _properties]
  (let [elements-context (c/++ context :elements)
        num-elements (interface/get-list-size elements-context)]
    (->> (range num-elements)
         (map #(c/++ elements-context %))
         (map interface/get-bounding-box)
         (reduce bb/combine nil))))

(defmethod interface/render-component :heraldry/ornaments [context]
  (let [elements-context (c/++ context :elements)]
    (into [:<>]
          ;; TODO: self-below-shield? probably should be a subscription as well,
          ;; which can always be checked in the render-component function
          (map (fn [[idx self-below-shield?]]
                 ^{:key idx}
                 [interface/render-component
                  (-> (c/++ elements-context idx)
                      (assoc :auto-resize? false
                             :self-below-shield? self-below-shield?))]))
          (shield-separator/get-element-indices elements-context))))

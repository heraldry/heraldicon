(ns heraldicon.heraldry.ornaments
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.shield-separator :as shield-separator]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]))

(defmethod interface/properties :heraldry/ornaments [_context]
  {:type :heraldry/ornaments})

(defmethod interface/environment :heraldry/ornaments [context]
  (interface/get-parent-environment context))

(defmethod interface/bounding-box :heraldry/ornaments [context]
  (let [elements-context (c/++ context :elements)
        num-elements (interface/get-list-size elements-context)]
    (->> (range num-elements)
         (map #(c/++ elements-context %))
         (map interface/get-bounding-box)
         (reduce bb/combine nil))))

(defmethod interface/render-ir :heraldry/ornaments [context]
  (shield-separator/elements-ir (c/++ context :elements)))

(defmethod interface/render-component :heraldry/ornaments [context]
  [interface/render-ir-root context])

(defmethod interface/render-shape :heraldry/ornaments [_context]
  nil)

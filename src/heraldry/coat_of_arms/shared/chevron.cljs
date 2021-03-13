(ns heraldry.coat-of-arms.shared.chevron
  (:require [heraldry.coat-of-arms.vector :as v]))

(defn arm-diagonals [variant origin-point anchor-point]
  (let [direction    (-> (v/- anchor-point origin-point)
                         v/normal
                         (v/* 200))
        [left right] (case variant
                       :base     (if (-> direction :x (> 0))
                                   [(v/v -1 1) (v/v 1 1)]
                                   [(v/v 1 1) (v/v -1 1)])
                       :chief    (if (-> direction :x (< 0))
                                   [(v/v -1 1) (v/v 1 1)]
                                   [(v/v 1 1) (v/v -1 1)])
                       :dexter   (if (-> direction :y (< 0))
                                   [(v/v 1 1) (v/v 1 -1)]
                                   [(v/v 1 -1) (v/v 1 1)])
                       :sinister (if (-> direction :y (> 0))
                                   [(v/v 1 1) (v/v 1 -1)]
                                   [(v/v 1 -1) (v/v 1 1)]))]
    [(v/dot direction left)
     (v/dot direction right)]))

(defn sanitize-anchor [variant anchor]
  (let [[allowed default] (case variant
                            :base     [#{:angle :bottom-right} :bottom-left]
                            :chief    [#{:angle :top-right} :top-left]
                            :dexter   [#{:angle :bottom-left} :top-left]
                            :sinister [#{:angle :bottom-right} :top-right])]
    (update anchor :point #(or (allowed %) default))))

(defn mirror-point [variant center point]
  (-> point
      (v/- center)
      (v/dot (if (#{:base :chief} variant)
               (v/v -1 1)
               (v/v 1 -1)))
      (v/+ center)))

(def variant-choices
  [["Base" :base]
   ["Chief" :chief]
   ["Dexter" :dexter]
   ["Sinister" :sinister]])

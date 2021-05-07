(ns heraldry.coat-of-arms.shared.chevron
  (:require [heraldry.coat-of-arms.vector :as v]))

(defn arm-diagonals [chevron-angle origin-point anchor-point]
  (let [direction (-> (v/- anchor-point origin-point)
                      v/normal
                      (v/* 200))
        [left right] (cond
                       (<= 45 chevron-angle 135) (if (-> direction :x (> 0))
                                                   [(v/v -1 1) (v/v 1 1)]
                                                   [(v/v 1 1) (v/v -1 1)])
                       (<= 225 chevron-angle 315) (if (-> direction :x (< 0))
                                                    [(v/v -1 1) (v/v 1 1)]
                                                    [(v/v 1 1) (v/v -1 1)])
                       (<= 135 chevron-angle 225) (if (-> direction :y (< 0))
                                                    [(v/v 1 1) (v/v 1 -1)]
                                                    [(v/v 1 -1) (v/v 1 1)])
                       :else (if (-> direction :y (> 0))
                               [(v/v 1 1) (v/v 1 -1)]
                               [(v/v 1 -1) (v/v 1 1)]))]
    [(v/dot direction left)
     (v/dot direction right)]))

(defn sanitize-anchor [chevron-angle anchor]
  (let [[allowed default] (cond
                            (<= 45 chevron-angle 135) [#{:angle :bottom-right} :bottom-left]
                            (<= 225 chevron-angle 315) [#{:angle :top-right} :top-left]
                            (<= 135 chevron-angle 225) [#{:angle :bottom-left} :top-left]
                            :else [#{:angle :bottom-right} :top-right])]
    (update anchor :point #(or (allowed %) default))))

(defn mirror-point [chevron-angle center point]
  (-> point
      (v/- center)
      (v/dot (cond
               (<= 45 chevron-angle 135) (v/v -1 1)
               (<= 225 chevron-angle 315) (v/v -1 1)
               (<= 135 chevron-angle 225) (v/v 1 -1)
               :else (v/v 1 -1)))
      (v/+ center)))

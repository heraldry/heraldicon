(ns heraldry.coat-of-arms.line.type.potenty
  (:require [heraldry.util :as util]))

(defn pattern
  {:display-name {:en "Potenty"
                  :de "Krückenschnitt"}
   :value :potenty}
  [{:keys [height
           eccentricity
           width]}
   _line-options]
  (let [l (-> width (/ 4) (* (util/map-to-interval eccentricity 0.6 1.4)))
        t (-> width (/ 2) (- l))]
    {:pattern ["l"
               [(+ l (/ t 2)) 0]
               [0 (- (* t height))]
               [(- l) 0]
               [0 (- (* t height))]
               [(+ l t l) 0]
               [0 (* t height)]
               [(- l) 0]
               [0 (* t height)]
               [(+ l (/ t 2)) 0]]
     :min (+ (- (* t height))
             (- (* t height)))
     :max 0}))

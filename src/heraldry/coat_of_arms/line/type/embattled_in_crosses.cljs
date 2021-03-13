(ns heraldry.coat-of-arms.line.type.embattled-in-crosses
  (:require [heraldry.util :as util]))

(defn pattern
  {:display-name "Embattled in crosses"
   :value :embattled-in-crosses}
  [{:keys [height
           eccentricity
           width]}
   _line-options]
  (let [l (-> width (/ 4) (* (util/map-to-interval eccentricity 0.6 1.4)))
        t (-> width (/ 2) (- l))]
    ["l"
     [(- (/ l 2)) 0]
     [0 (- (* t height))]
     [l 0]
     [0 (- (* l height))]
     [t 0]
     [0 (* l height)]
     [l 0]
     [0 (* t height)]
     [(- l) 0]
     [0 (* t height)]
     [l 0]
     [0 (* l height)]
     [t 0]
     [0 (- (* l height))]
     [l 0]
     [0 (- (* t height))]
     [(- (/ l 2)) 0]]))

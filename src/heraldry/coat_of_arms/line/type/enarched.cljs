(ns heraldry.coat-of-arms.line.type.enarched
  (:require [heraldry.util :as util]))

(defn full
  {:display-name "Enarched / Embowed"
   :value :enarched
   :full? true}
  [{:keys [height width
           eccentricity] :as _line-data}
   length
   {:keys [real-start real-end] :as _line-options}]
  (let [real-start (or real-start 0)
        real-end (or real-end length)
        relevant-length (- real-end real-start)
        height (* relevant-length
                  (util/map-to-interval height 0 0.4))
        pos-x (-> relevant-length
                  (* eccentricity))
        anchor-1 (- 1 (/ width 100))
        anchor-2 (- 1 anchor-1)]
    ["h" real-start
     "c"
     (-> pos-x
         (* anchor-1)) "," (- height) " "
     (-> relevant-length
         (- pos-x)
         (* anchor-2)
         (+ pos-x)) "," (- height) " "
     relevant-length "," 0
     "h" (- length real-end)]))

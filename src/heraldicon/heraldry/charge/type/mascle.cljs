(ns heraldicon.heraldry.charge.type.mascle
  (:require
   [heraldicon.heraldry.charge.interface :as charge.interface]
   [heraldicon.heraldry.charge.shared :as charge.shared]
   [heraldicon.interface :as interface]))

(def charge-type :heraldry.charge.type/mascle)

(defmethod charge.interface/display-name charge-type [_] :string.charge.type/mascle)

(defmethod charge.interface/options charge-type [context]
  (-> (charge.shared/options context)
      (update :geometry dissoc :mirrored?)
      (update :geometry dissoc :reversed?)))

(def ^:private base
  (let [height 100
        width (/ height 1.3)
        width-half (/ width 2)
        height-half (/ height 2)
        hole-width (* width 0.55)
        hole-height (* height 0.55)
        hole-width-half (/ hole-width 2)
        hole-height-half (/ hole-height 2)]
    {:base-shape [["M" 0 (- height-half)
                   "l" width-half height-half
                   "l " (- width-half) height-half
                   "l" (- width-half) (- height-half)
                   "z"]
                  ["M" 0 (- hole-height-half)
                   "l" hole-width-half hole-height-half
                   "l " (- hole-width-half) hole-height-half
                   "l" (- hole-width-half) (- hole-height-half)
                   "z"]]
     :base-width width
     :base-height height}))

(defmethod interface/properties charge-type [context]
  (charge.shared/process-shape context base))

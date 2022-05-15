(ns heraldicon.heraldry.charge.type.billet
  (:require
   [heraldicon.heraldry.charge.interface :as charge.interface]
   [heraldicon.heraldry.charge.shared :as charge.shared]
   [heraldicon.math.vector :as v]))

(def charge-type :heraldry.charge.type/billet)

(defmethod charge.interface/display-name charge-type [_] :string.charge.type/billet)

(defmethod charge.interface/options charge-type [context]
  (-> (charge.shared/options context)
      (update :geometry dissoc :mirrored?)
      (update :geometry dissoc :reversed?)))

(defmethod charge.interface/render-charge charge-type
  [context]
  (charge.shared/make-charge
   context
   :height
   (fn [height]
     (let [width (/ height 2)
           width-half (/ width 2)
           height-half (/ height 2)]
       {:shape ["m" (v/Vector. (- width-half) (- height-half))
                "h" width
                "v" height
                "h" (- width)
                "z"]
        :charge-width width
        :charge-height height}))))

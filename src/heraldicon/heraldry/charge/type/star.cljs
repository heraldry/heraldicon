(ns heraldicon.heraldry.charge.type.star
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.charge.interface :as charge.interface]
   [heraldicon.heraldry.charge.shared :as charge.shared]
   [heraldicon.heraldry.line.type.rayonny-flaming :as rayonny-flaming]
   [heraldicon.interface :as interface]
   [heraldicon.math.angle :as angle]
   [heraldicon.math.vector :as v]))

(def charge-type :heraldry.charge.type/star)

(defmethod charge.interface/display-name charge-type [_] :string.charge.type/star)

(defmethod charge.interface/options charge-type [context]
  (let [default-num-points 5
        num-points (or (interface/get-raw-data (c/++ context :num-points))
                       default-num-points)]
    (-> (charge.shared/options context)
        (assoc :num-points {:type :option.type/range
                            :default default-num-points
                            :min 3
                            :max 32
                            :integer? true
                            :ui/label :string.option/number-of-points})
        (assoc :eccentricity {:type :option.type/range
                              :default (- 1
                                          (cond
                                            (< num-points 5) 0.25
                                            (> num-points 6) 0.4
                                            :else (let [angle-step (-> (/ 360 num-points)
                                                                       angle/to-rad)
                                                        angle-half-step (/ angle-step 2)]
                                                    (/ (Math/cos angle-step)
                                                       (Math/cos angle-half-step)))))
                              :min 0
                              :max 0.95
                              :ui/label :string.option/eccentricity
                              :ui/step 0.01})
        (assoc :wavy-rays? {:type :option.type/boolean
                            :default false
                            :ui/label :string.option/wavy-rays?}))))

(defmethod interface/properties charge-type [context]
  (let [eccentricity (interface/get-sanitized-data (c/++ context :eccentricity))
        num-points (interface/get-sanitized-data (c/++ context :num-points))
        wavy-rays? (interface/get-sanitized-data (c/++ context :wavy-rays?))
        angle-step (/ 360 num-points)
        rayonny-eccentricity 0.3
        width 100
        radius (/ width 2)
        small-radius (* radius (- 1 eccentricity))
        first-point (v/Vector. 0 (- radius))
        first-valley (v/rotate (v/Vector. 0 (- small-radius))
                               (/ angle-step 2))]
    (charge.shared/process-shape
     context
     {:base-shape [(conj (into ["M" first-point]
                               (mapcat (fn [i]
                                         (let [point (v/rotate first-point (* angle-step i))
                                               next-point (v/rotate first-point (* angle-step (inc i)))
                                               valley (v/rotate first-valley (* angle-step i))]
                                           (if wavy-rays?
                                             [(rayonny-flaming/curvy-line
                                               (v/sub valley point) rayonny-eccentricity false)
                                              (rayonny-flaming/curvy-line
                                               (v/sub next-point valley) rayonny-eccentricity true)]
                                             ["L" valley
                                              "L" next-point]))))
                               (range num-points))
                         "z")]
      :base-width width
      :base-height width})))

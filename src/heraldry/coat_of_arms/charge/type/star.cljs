(ns heraldry.coat-of-arms.charge.type.star
  (:require
   [heraldry.coat-of-arms.charge.interface :as charge-interface]
   [heraldry.coat-of-arms.charge.shared :as charge-shared]
   [heraldry.coat-of-arms.line.type.rayonny-flaming :as rayonny-flaming]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.math.core :as math]
   [heraldry.math.vector :as v]))

(def charge-type :heraldry.charge.type/star)

(defmethod charge-interface/display-name charge-type [_] :string.charge.type/star)

(defmethod interface/options charge-type [context]
  (let [default-num-points 5
        num-points (or (interface/get-raw-data (c/++ context :num-points))
                       default-num-points)]
    (-> (charge-shared/options context)
        (assoc :num-points {:type :range
                            :default default-num-points
                            :min 3
                            :max 32
                            :integer? true
                            :ui {:label :string.option/number-of-points}})
        (assoc :eccentricity {:type :range
                              :default (- 1
                                          (cond
                                            (< num-points 5) 0.25
                                            (> num-points 6) 0.4
                                            :else (let [angle-step (-> (/ 360 num-points)
                                                                       math/to-rad)
                                                        angle-half-step (/ angle-step 2)]
                                                    (/ (Math/cos angle-step)
                                                       (Math/cos angle-half-step)))))
                              :min 0
                              :max 0.95
                              :ui {:label :string.option/eccentricity
                                   :step 0.01}})
        (assoc :wavy-rays? {:type :boolean
                            :default false
                            :ui {:label :string.option/wavy-rays?}}))))

(defmethod charge-interface/render-charge charge-type
  [context]
  (let [eccentricity (interface/get-sanitized-data (c/++ context :eccentricity))
        num-points (interface/get-sanitized-data (c/++ context :num-points))
        wavy-rays? (interface/get-sanitized-data (c/++ context :wavy-rays?))
        angle-step (/ 360 num-points)
        rayonny-eccentricity 0.3]
    (charge-shared/make-charge
     context
     :width
     (fn [width]
       (let [radius (/ width 2)
             small-radius (* radius (- 1 eccentricity))
             first-point (v/v 0 (- radius))
             first-valley (-> (v/v 0 (- small-radius))
                              (v/rotate (/ angle-step 2)))]
         {:shape (-> (into ["M" first-point]
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
                     (conj "z"))
          :charge-width width
          :charge-height width})))))

(ns heraldry.coat-of-arms.charge-group.core
  (:require [heraldry.coat-of-arms.charge-group.options :as charge-group-options]
            [heraldry.coat-of-arms.charge.core :as charge]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(defn calculate-strip-slot-positions [{:keys [slots] :as strip} spacing]
  (let [{:keys [stretch
                offset]} (options/sanitize strip charge-group-options/strip-options)
        spacing (* spacing
                   stretch)
        offset (* spacing
                  offset)
        length (+ (* (-> slots
                         count
                         dec
                         (max 0))
                     spacing)
                  offset)]
    (map-indexed (fn [idx charge-index]
                   {:position (-> idx
                                  (* spacing)
                                  (- (/ length 2))
                                  (+ offset))
                    :slot-index idx
                    :charge-index charge-index}) slots)))

(defmulti calculate-points
  (fn [{:keys [type]} _ _]
    (case type
      :heraldry.charge-group.type/rows :strips
      :heraldry.charge-group.type/columns :strips
      :heraldry.charge-group.type/arc :arc)))

(defmethod calculate-points :strips [{:keys [type strips charges] :as charge-group} environment
                                     {:keys [db-path]}]
  (let [reference-length (case type
                           :heraldry.charge-group.type/rows (:width environment)
                           :heraldry.charge-group.type/columns (:height environment))
        options (charge-group-options/options charge-group)
        {:keys [spacing
                stretch
                strip-angle]} (options/sanitize charge-group options)
        spacing ((util/percent-of reference-length) spacing)
        strip-spacing (* spacing
                         stretch)
        length (* (-> strips count dec (max 0))
                  strip-spacing)
        num-charges (count charges)
        make-point-fn (case type
                        :heraldry.charge-group.type/rows v/v
                        :heraldry.charge-group.type/columns (fn [y x] (v/v x y)))]
    {:slot-positions (->> strips
                          (map-indexed (fn [idx strip]
                                         (let [slot-positions (calculate-strip-slot-positions strip spacing)
                                               strip-position (-> idx
                                                                  (* strip-spacing)
                                                                  (- (/ length 2)))]
                                           (map (fn [{:keys [position charge-index slot-index]}]
                                                  {:point (v/rotate (make-point-fn position
                                                                                   strip-position)
                                                                    strip-angle)
                                                   :slot-path (conj db-path :strips idx :slots slot-index)
                                                   :charge-index (if (and (int? charge-index)
                                                                          (< -1 charge-index num-charges))
                                                                   charge-index
                                                                   nil)})
                                                slot-positions))))
                          (apply concat)
                          vec)
     :slot-spacing (case type
                     :heraldry.charge-group.type/rows {:width spacing
                                                       :height strip-spacing}
                     :heraldry.charge-group.type/columns {:width strip-spacing
                                                          :height spacing})}))

(defmethod calculate-points :arc [{:keys [slots charges] :as charge-group} environment
                                  {:keys [db-path]}]
  (let [options (charge-group-options/options charge-group)
        {:keys [radius
                arc-angle
                start-angle
                arc-stretch]} (options/sanitize charge-group options)
        radius ((util/percent-of (:width environment)) radius)
        stretch-vector (if (> arc-stretch 1)
                         (v/v (/ 1 arc-stretch) 1)
                         (v/v 1 arc-stretch))
        num-charges (count charges)
        num-slots (count slots)
        angle-step (/ arc-angle (max (if (= arc-angle 360)
                                       num-slots
                                       (dec num-slots)) 1))
        distance (if (<= num-slots 1)
                   50
                   (-> (v/v radius 0)
                       (v/- (v/rotate (v/v radius 0) angle-step))
                       v/abs))]
    {:slot-positions (->> slots
                          (map-indexed (fn [slot-index charge-index]
                                         (let [slot-angle (-> slot-index
                                                              (* angle-step)
                                                              (+ start-angle)
                                                              (- 90))]
                                           {:point (-> (v/v radius 0)
                                                       (v/rotate slot-angle)
                                                       (v/dot stretch-vector))
                                            :slot-path (conj db-path :slots slot-index)
                                            :charge-index (if (and (int? charge-index)
                                                                   (< -1 charge-index num-charges))
                                                            charge-index
                                                            nil)
                                            :angle (+ slot-angle 90)})))
                          vec)
     :slot-spacing {:width (/ distance 1.2)
                    :height (/ distance 1.2)}}))

(defn render [{:keys [charges rotate-charges?] :as charge-group} parent environment context]
  [:g
   (let [options (charge-group-options/options charge-group)
         {:keys [origin]} (options/sanitize charge-group options)
         real-origin (position/calculate origin environment)
         {:keys [slot-positions
                 slot-spacing]} (calculate-points charge-group environment context)]
     (for [[idx {:keys [point charge-index angle]}] (map-indexed vector slot-positions)
           :when (and charge-index
                      (< charge-index (count charges)))]
       (let [charge (-> charges
                        (get charge-index)
                        (assoc :origin {:point :special}))]
         ^{:key idx}
         [charge/render charge parent
          (assoc-in environment [:points :special] (v/+ real-origin point))
          (-> context
              (update :db-path conj :charges charge-index)
              (assoc :charge-group {:charge-group charge-group
                                    :slot-spacing slot-spacing
                                    :slot-angle (when rotate-charges?
                                                  angle)}))])))])

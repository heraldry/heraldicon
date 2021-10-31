(ns heraldry.coat-of-arms.charge-group.core
  (:require
   [heraldry.coat-of-arms.charge.interface :as charge-interface]
   [heraldry.coat-of-arms.escutcheon :as escutcheon]
   [heraldry.coat-of-arms.position :as position]
   [heraldry.interface :as interface]
   [heraldry.math.vector :as v]
   [heraldry.util :as util]))

(defn calculate-strip-slot-positions [context spacing]
  (let [stretch (interface/get-sanitized-data (update context :path conj :stretch))
        offset (interface/get-sanitized-data (update context :path conj :offset))
        slots (interface/get-raw-data (update context :path conj :slots))
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
  (fn [context]
    (case (interface/get-sanitized-data (update context :path conj :type))
      :heraldry.charge-group.type/rows :strips
      :heraldry.charge-group.type/columns :strips
      :heraldry.charge-group.type/arc :arc)))

(defmethod calculate-points :strips [{:keys [environment] :as context}]
  (let [charge-group-type (interface/get-sanitized-data (update context :path conj :type))
        reference-length (case charge-group-type
                           :heraldry.charge-group.type/rows (:width environment)
                           :heraldry.charge-group.type/columns (:height environment))
        spacing (interface/get-sanitized-data (update context :path conj :spacing))
        stretch (interface/get-sanitized-data (update context :path conj :stretch))
        strip-angle (interface/get-sanitized-data (update context :path conj :strip-angle))
        num-charges (interface/get-list-size (update context :path conj :charges))
        num-strips (interface/get-list-size (update context :path conj :strips))
        spacing ((util/percent-of reference-length) spacing)
        strip-spacing (* spacing
                         stretch)
        length (* (-> num-strips dec (max 0))
                  strip-spacing)
        make-point-fn (case charge-group-type
                        :heraldry.charge-group.type/rows v/v
                        :heraldry.charge-group.type/columns (fn [y x] (v/v x y)))]
    {:slot-positions (->> (range num-strips)
                          (map (fn [idx]
                                 (let [slot-positions (calculate-strip-slot-positions
                                                       (update context :path conj :strips idx) spacing)
                                       strip-position (-> idx
                                                          (* strip-spacing)
                                                          (- (/ length 2)))]
                                   (map (fn [{:keys [position charge-index slot-index]}]
                                          {:point (v/rotate (make-point-fn position
                                                                           strip-position)
                                                            strip-angle)
                                           :slot-path (-> context :path (conj :strips idx :slots slot-index))
                                           :charge-index (if (and (int? charge-index)
                                                                  (< -1 charge-index num-charges))
                                                           charge-index
                                                           nil)})
                                        slot-positions))))
                          (apply concat)
                          vec)
     :slot-spacing (case charge-group-type
                     :heraldry.charge-group.type/rows {:width spacing
                                                       :height strip-spacing}
                     :heraldry.charge-group.type/columns {:width strip-spacing
                                                          :height spacing})}))

(defmethod calculate-points :arc [{:keys [environment] :as context}]
  (let [radius (interface/get-sanitized-data (update context :path conj :radius))
        start-angle (interface/get-sanitized-data (update context :path conj :start-angle))
        arc-angle (interface/get-sanitized-data (update context :path conj :arc-angle))
        arc-stretch (interface/get-sanitized-data (update context :path conj :arc-stretch))
        slots (interface/get-raw-data (update context :path conj :slots))
        num-charges (interface/get-list-size (update context :path conj :charges))
        num-slots (interface/get-list-size (update context :path conj :slots))
        radius ((util/percent-of (:width environment)) radius)
        stretch-vector (if (> arc-stretch 1)
                         (v/v (/ 1 arc-stretch) 1)
                         (v/v 1 arc-stretch))
        angle-step (/ arc-angle (max (if (= arc-angle 360)
                                       num-slots
                                       (dec num-slots)) 1))
        distance (if (<= num-slots 1)
                   50
                   (-> (v/v radius 0)
                       (v/sub (v/rotate (v/v radius 0) angle-step))
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
                                            :slot-path (-> context :path (conj :slots slot-index))
                                            :charge-index (if (and (int? charge-index)
                                                                   (< -1 charge-index num-charges))
                                                            charge-index
                                                            nil)
                                            :angle (+ slot-angle 90)})))
                          vec)
     :slot-spacing {:width (/ distance 1.2)
                    :height (/ distance 1.2)}}))

(defmethod interface/render-component :heraldry.component/charge-group [{:keys [path environment] :as context}]
  (let [origin (interface/get-sanitized-data (update context :path conj :origin))
        rotate-charges? (interface/get-sanitized-data (update context :path conj :rotate-charges?))
        origin-point (position/calculate origin environment)
        {:keys [slot-positions
                slot-spacing]} (calculate-points context)
        num-charges (interface/get-list-size (update context :path conj :charges))]

    [:g
     (for [[idx {:keys [point charge-index angle]}] (map-indexed vector slot-positions)
           :when (and charge-index
                      (< charge-index num-charges))]
       ^{:key idx}
       [charge-interface/render-charge
        (-> context
            (update :path conj :charges charge-index)
            (assoc :origin-override (v/add origin-point point))
            (assoc :charge-group {:charge-group-path path
                                  :slot-spacing slot-spacing
                                  :slot-angle (when rotate-charges?
                                                angle)}))])]))

(defmethod interface/blazon-component :heraldry.component/charge-group [path context]
  (let [{:keys [slot-positions]} (calculate-points (-> context
                                                       (assoc :path path)
                                                       (assoc :environment escutcheon/flag-3-2)))
        used-charges (->> (group-by :charge-index slot-positions)
                          (map (fn [[k v]]
                                 [k (count v)]))
                          (filter (fn [[k v]]
                                    (and k (pos? v))))
                          (sort-by second))
        context (assoc-in context [:blazonry :part-of-charge-group?] true)]
    (util/str-tr (util/combine
                  " and "
                  (map (fn [[charge-index number]]
                         (util/str-tr number " "
                                      (interface/blazon (conj path :charges charge-index)
                                                        (cond-> context
                                                          (> number 1) (assoc-in [:blazonry :pluralize?] true)))))
                       used-charges)))))

(ns heraldry.coat-of-arms.charge-group.core
  (:require
   [heraldry.coat-of-arms.charge.interface :as charge-interface]
   [heraldry.coat-of-arms.field.environment :as environment]
   [heraldry.coat-of-arms.position :as position]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.math.svg.path :as path]
   [heraldry.math.vector :as v]
   [heraldry.util :as util]))

(defn calculate-strip-slot-positions [context spacing]
  (let [stretch (interface/get-sanitized-data (c/++ context :stretch))
        offset (interface/get-sanitized-data (c/++ context :offset))
        slots (interface/get-raw-data (c/++ context :slots))
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
    (case (interface/get-sanitized-data (c/++ context :type))
      :heraldry.charge-group.type/rows :strips
      :heraldry.charge-group.type/columns :strips
      :heraldry.charge-group.type/arc :arc
      :heraldry.charge-group.type/in-orle :in-orle)))

(defmethod calculate-points :strips [{:keys [environment] :as context}]
  (let [charge-group-type (interface/get-sanitized-data (c/++ context :type))
        reference-length (case charge-group-type
                           :heraldry.charge-group.type/rows (:width environment)
                           :heraldry.charge-group.type/columns (:height environment))
        spacing (interface/get-sanitized-data (c/++ context :spacing))
        stretch (interface/get-sanitized-data (c/++ context :stretch))
        strip-angle (interface/get-sanitized-data (c/++ context :strip-angle))
        num-charges (interface/get-list-size (c/++ context :charges))
        num-strips (interface/get-list-size (c/++ context :strips))
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
                                                       (c/++ context :strips idx) spacing)
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
  (let [radius (interface/get-sanitized-data (c/++ context :radius))
        start-angle (interface/get-sanitized-data (c/++ context :start-angle))
        arc-angle (interface/get-sanitized-data (c/++ context :arc-angle))
        arc-stretch (interface/get-sanitized-data (c/++ context :arc-stretch))
        slots (interface/get-raw-data (c/++ context :slots))
        num-charges (interface/get-list-size (c/++ context :charges))
        num-slots (interface/get-list-size (c/++ context :slots))
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

(defmethod calculate-points :in-orle [{:keys [environment] :as context}]
  (let [distance (interface/get-sanitized-data (c/++ context :distance))
        offset (interface/get-sanitized-data (c/++ context :offset))
        slots (interface/get-raw-data (c/++ context :slots))
        num-charges (interface/get-list-size (c/++ context :charges))
        num-slots (interface/get-list-size (c/++ context :slots))
        environment-shape (-> environment
                              (update-in [:shape :paths] (partial take 1))
                              environment/effective-shape)
        width (:width environment)
        distance ((util/percent-of width) distance)
        bordure-shape (environment/shrink-shape environment-shape distance :round)
        points (:points environment)
        shape-path (path/parse-path bordure-shape)
        path-length (.-length shape-path)
        step-t (/ path-length (max num-slots 1))
        offset-t (* offset step-t)
        fess (:fess points)
        top (:top points)
        intersection (v/find-first-intersection-of-ray
                      fess top
                      {:shape {:paths [bordure-shape]}})
        start-t (-> intersection
                    :t2
                    (* path-length)
                    (+ offset-t))
        charge-space (min (* distance 2) step-t)]
    {:slot-positions (->> slots
                          (map-indexed (fn [slot-index charge-index]
                                         (let [slot-t (-> slot-index
                                                          (* step-t)
                                                          (+ start-t)
                                                          (mod path-length))]
                                           {:point (-> shape-path
                                                       (.getPointAt slot-t)
                                                       (as-> p
                                                         (v/v (.-x p) (.-y p))))
                                            :slot-path (-> context :path (conj :slots slot-index))
                                            :charge-index (if (and (int? charge-index)
                                                                   (< -1 charge-index num-charges))
                                                            charge-index
                                                            nil)
                                            :angle 0})))
                          vec)
     :slot-spacing {:width (/ charge-space 1.2)
                    :height (/ charge-space 1.2)}}))

(defmethod interface/render-component :heraldry.component/charge-group [{:keys [path environment] :as context}]
  (let [origin (interface/get-sanitized-data (c/++ context :origin))
        rotate-charges? (interface/get-sanitized-data (c/++ context :rotate-charges?))
        origin-point (position/calculate origin environment)
        {:keys [slot-positions
                slot-spacing]} (calculate-points context)
        num-charges (interface/get-list-size (c/++ context :charges))]

    [:g
     (for [[idx {:keys [point charge-index angle]}] (map-indexed vector slot-positions)
           :when (and charge-index
                      (< charge-index num-charges))]
       ^{:key idx}
       [charge-interface/render-charge
        (-> context
            (c/++ :charges charge-index)
            (assoc :origin-override (v/add origin-point point))
            (assoc :charge-group {:charge-group-path path
                                  :slot-spacing slot-spacing
                                  :slot-angle (when rotate-charges?
                                                angle)}))])]))

(defmethod interface/blazon-component :heraldry.component/charge-group [context]
  ;; TODO: no need to calculate all positions here
  (let [{:keys [slot-positions]} (calculate-points
                                  (-> context
                                      (assoc :environment
                                             {:width 200
                                              :height 200
                                              :shape {:paths ["M-100,-100 h200 v200 h-200 z"]}})))
        charge-group-type (interface/get-raw-data (c/++ context :type))
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
                                      (interface/blazon (-> context
                                                            (c/++ :charges charge-index)
                                                            (cond->
                                                              (> number 1) (assoc-in [:blazonry :pluralize?] true))))))
                       used-charges))
                 (when (= charge-group-type :heraldry.charge-group.type/in-orle)
                   " in orle"))))

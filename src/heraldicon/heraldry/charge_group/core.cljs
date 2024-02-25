(ns heraldicon.heraldry.charge-group.core
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]
   [heraldicon.svg.path :as path]))

(defn- calculate-strip-slot-positions [context spacing]
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

(derive :heraldry.charge-group.type/rows :heraldry.charge-group/type)
(derive :heraldry.charge-group.type/columns :heraldry.charge-group/type)
(derive :heraldry.charge-group.type/arc :heraldry.charge-group/type)
(derive :heraldry.charge-group.type/in-orle :heraldry.charge-group/type)
(derive :heraldry.charge-group/type :heraldry/charge-group)

(defmulti calculate-points
  (fn [context]
    (case (interface/get-sanitized-data (c/++ context :type))
      :heraldry.charge-group.type/rows :strips
      :heraldry.charge-group.type/columns :strips
      :heraldry.charge-group.type/arc :arc
      :heraldry.charge-group.type/in-orle :in-orle)))

(defmethod calculate-points :strips [context]
  (let [{:keys [width height]} (interface/get-parent-field-environment context)
        charge-group-type (interface/get-sanitized-data (c/++ context :type))
        reference-length (case charge-group-type
                           :heraldry.charge-group.type/rows width
                           :heraldry.charge-group.type/columns height)
        spacing (interface/get-sanitized-data (c/++ context :spacing))
        stretch (interface/get-sanitized-data (c/++ context :stretch))
        strip-angle (interface/get-sanitized-data (c/++ context :strip-angle))
        num-charges (interface/get-list-size (c/++ context :charges))
        num-strips (interface/get-list-size (c/++ context :strips))
        spacing (math/percent-of reference-length spacing)
        strip-spacing (* spacing
                         stretch)
        length (* (-> num-strips dec (max 0))
                  strip-spacing)
        make-point-fn (case charge-group-type
                        :heraldry.charge-group.type/rows (fn [x y] (v/Vector. x y))
                        :heraldry.charge-group.type/columns (fn [y x] (v/Vector. x y)))]
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

(defmethod calculate-points :arc [context]
  (let [{:keys [width]} (interface/get-parent-field-environment context)
        radius (interface/get-sanitized-data (c/++ context :radius))
        start-angle (interface/get-sanitized-data (c/++ context :start-angle))
        arc-angle (interface/get-sanitized-data (c/++ context :arc-angle))
        arc-stretch (interface/get-sanitized-data (c/++ context :arc-stretch))
        slots (interface/get-raw-data (c/++ context :slots))
        num-charges (interface/get-list-size (c/++ context :charges))
        num-slots (interface/get-list-size (c/++ context :slots))
        radius (math/percent-of width radius)
        stretch-vector (if (> arc-stretch 1)
                         (v/Vector. (/ 1 arc-stretch) 1)
                         (v/Vector. 1 arc-stretch))
        angle-step (/ arc-angle (max (if (= arc-angle 360)
                                       num-slots
                                       (dec num-slots)) 1))
        distance (if (<= num-slots 1)
                   50
                   (-> (v/Vector. radius 0)
                       (v/sub (v/rotate (v/Vector. radius 0) angle-step))
                       v/abs))]
    {:slot-positions (->> slots
                          (map-indexed (fn [slot-index charge-index]
                                         (let [slot-angle (-> slot-index
                                                              (* angle-step)
                                                              (+ start-angle)
                                                              (- 90))]
                                           {:point (-> (v/Vector. radius 0)
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

(defmethod calculate-points :in-orle [context]
  ;; use get-environment and get-exact-shape for the parent context here deliberately,
  ;; so the orle arrangement is not inside the impacted field
  (let [{:keys [width height points]} (interface/get-environment (interface/parent context))
        percentage-base (min width height)
        distance (interface/get-sanitized-data (c/++ context :distance))
        offset (interface/get-sanitized-data (c/++ context :offset))
        slots (interface/get-raw-data (c/++ context :slots))
        num-charges (interface/get-list-size (c/++ context :charges))
        num-slots (interface/get-list-size (c/++ context :slots))
        parent-shape (interface/get-exact-shape (interface/parent context))
        distance (math/percent-of percentage-base distance)
        bordure-shape (environment/shrink-shape parent-shape distance :round)
        shape-path (path/parse-path bordure-shape)
        path-length (.-length shape-path)
        step-t (/ path-length (max num-slots 1))
        offset-t (* offset step-t)
        fess (:fess points)
        top (:top points)
        intersection (v/last-intersection-with-shape fess top bordure-shape :default? true)
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
                                                             (v/Vector. (.-x p) (.-y p))))
                                            :slot-path (-> context :path (conj :slots slot-index))
                                            :charge-index (if (and (int? charge-index)
                                                                   (< -1 charge-index num-charges))
                                                            charge-index
                                                            nil)
                                            :angle 0})))
                          vec)
     :slot-spacing {:width (/ charge-space 1.2)
                    :height (/ charge-space 1.2)}}))

(defmethod interface/properties :heraldry/charge-group [context]
  (let [parent-environment (interface/get-parent-field-environment context)
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        anchor-point (position/calculate anchor parent-environment)
        rotate-charges? (interface/get-sanitized-data (c/++ context :rotate-charges?))
        {:keys [slot-positions
                slot-spacing]} (calculate-points context)
        num-charges (interface/get-list-size (c/++ context :charges))]
    {:type :heraldry/charge-group
     :anchor-point anchor-point
     :rotate-charges? rotate-charges?
     :num-charges num-charges
     :slot-positions slot-positions
     :slot-spacing slot-spacing}))

(defn- iterate-charge-contexts [context {:keys [anchor-point
                                                slot-positions slot-spacing
                                                rotate-charges?
                                                num-charges]}]
  (for [{:keys [point charge-index angle]} slot-positions
        :when (and charge-index
                   (< charge-index num-charges))]
    (-> context
        (c/++ :charges charge-index)
        (c/set-key :anchor-override (v/add anchor-point point))
        (c/set-key :charge-group {:slot-spacing slot-spacing
                                  :slot-angle (when rotate-charges?
                                                angle)}))))

(defmethod interface/bounding-box :heraldry/charge-group [context properties]
  (->> (iterate-charge-contexts context properties)
       (map interface/get-bounding-box)
       (reduce bb/combine)))

(defmethod interface/render-component :heraldry/charge-group [context]
  (let [properties (interface/get-properties context)]
    (into [:g]
          (for [[idx charge-context] (map-indexed vector (iterate-charge-contexts context properties))]
            ^{:key idx}
            [interface/render-component charge-context]))))

(defmethod interface/blazon-component :heraldry/charge-group [context]
  ;; TODO: no need to calculate all positions here
  (let [{:keys [slot-positions]} (calculate-points
                                  (-> context
                                      (c/set-key :parent-environment-override {:width 200
                                                                               :height 200})
                                      (c/set-key :parent-shape "M-100,-100 h200 v200 h-200 z")))
        charge-group-type (interface/get-raw-data (c/++ context :type))
        used-charges (->> (group-by :charge-index slot-positions)
                          (map (fn [[k v]]
                                 [k (count v)]))
                          (filter (fn [[k v]]
                                    (and k (pos? v))))
                          (sort-by second))
        context (assoc-in context [:blazonry :part-of-charge-group?] true)]
    (string/str-tr (string/combine
                    " and "
                    (map (fn [[charge-index number]]
                           (string/str-tr number " "
                                          (interface/blazon (-> context
                                                                (c/++ :charges charge-index)
                                                                (cond->
                                                                  (> number 1) (assoc-in [:blazonry :pluralize?] true))))))
                         used-charges))
                   (when (= charge-group-type :heraldry.charge-group.type/in-orle)
                     " in orle"))))

(defn update-missing-charge-indices
  [db path]
  (let [parent-path (vec (drop-last 2 path))
        strips-context (conj parent-path :strips)
        slots-path (conj parent-path :slots)
        charges (get-in db (conj parent-path :charges))
        num-charges (count charges)]
    (-> db
        (update-in strips-context (fn [strips]
                                    (mapv (fn [strip]
                                            (update strip :slots (fn [slots]
                                                                   (mapv (fn [charge-index]
                                                                           (if (<= num-charges charge-index)
                                                                             0
                                                                             charge-index))
                                                                         slots))))
                                          strips)))
        (update-in slots-path (fn [slots]
                                (mapv (fn [charge-index]
                                        (if (<= num-charges charge-index)
                                          0
                                          charge-index))
                                      slots))))))

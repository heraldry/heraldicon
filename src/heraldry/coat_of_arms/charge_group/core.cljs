(ns heraldry.coat-of-arms.charge-group.core
  (:require [heraldry.coat-of-arms.charge.interface :as charge-interface]
            [heraldry.coat-of-arms.escutcheon :as escutcheon]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.interface :as interface]
            [heraldry.util :as util]))

(def presets
  [["Ordinaries"
    ["Fesswise" :fesswise
     {:type :heraldry.charge-group.type/rows
      :strip-angle 0
      :spacing 25
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:slots [0 0 0]}]}]
    ["Palewise" :palewise
     {:type :heraldry.charge-group.type/columns
      :strip-angle 0
      :spacing 20
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:slots [0 0 0]}]}]
    ["Bendwise" :bendwise
     {:type :heraldry.charge-group.type/rows
      :strip-angle 45
      :spacing 25
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:slots [0 0 0]}]}]
    ["Bendwise (sinister)" :bendwise-sinister
     {:type :heraldry.charge-group.type/rows
      :strip-angle -45
      :spacing 25
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:slots [0 0 0]}]}]
    ["Chevronwise" :chevronwise
     {:type :heraldry.charge-group.type/rows
      :strip-angle 0
      :spacing 30
      :stretch 0.6
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:slots [0]}
               {:slots [0 0]}
               {:slots [0 nil 0]}]}]
    ["In cross" :in-cross
     {:type :heraldry.charge-group.type/rows
      :strip-angle 0
      :spacing 20
      :stretch 1
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:slots [nil nil 0 nil nil]}
               {:slots [nil nil 0 nil nil]}
               {:slots [0 0 0 0 0]}
               {:slots [nil nil 0 nil nil]}
               {:slots [nil nil 0 nil nil]}]}]
    ["In saltire" :in-saltire
     {:type :heraldry.charge-group.type/rows
      :strip-angle 45
      :spacing 20
      :stretch 1
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:slots [nil nil 0 nil nil]}
               {:slots [nil nil 0 nil nil]}
               {:slots [0 0 0 0 0]}
               {:slots [nil nil 0 nil nil]}
               {:slots [nil nil 0 nil nil]}]}]
    ["In pall" :in-pall
     {:type :heraldry.charge-group.type/columns
      :strip-angle 0
      :spacing 14
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}
                 :geometry {:size 18}}]
      :strips [{:slots [0 nil nil nil nil]}
               {:slots [nil 0 nil nil nil]}
               {:slots [nil nil 0 0 0]
                :stretch 1.25}
               {:slots [nil 0 nil nil nil]}
               {:slots [0 nil nil nil nil]}]}]
    ["Three in above bend" :three-in-above-bend
     {:type :heraldry.charge-group.type/rows
      :strip-angle 0
      :spacing 30
      :stretch 1
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}
                 :geometry {:size 20}}]
      :strips [{:slots [0 0]}
               {:slots [nil 0]}]}]
    ["Three below bend" :three-below-bend
     {:type :heraldry.charge-group.type/rows
      :strip-angle 45
      :spacing 50
      :stretch 0.3
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}
                 :geometry {:size 20}}]
      :strips [{:slots [0 0]}
               {:slots [0]}]}]]
   ["Triangular"
    ["Three" :three
     {:type :heraldry.charge-group.type/rows
      :strip-angle 0
      :spacing 40
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:slots [0 0]}
               {:slots [0]}]}]
    ["Three (inverted)" :three-inverted
     {:type :heraldry.charge-group.type/rows
      :strip-angle 0
      :spacing 40
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:slots [0]}
               {:slots [0 0]}]}]

    ["Three (columns)" :three-columns
     {:type :heraldry.charge-group.type/columns
      :strip-angle 0
      :spacing 30
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:slots [0 0]}
               {:slots [0]}]}]
    ["Three (columns, inverted)" :three-columns-inverted
     {:type :heraldry.charge-group.type/columns
      :strip-angle 0
      :spacing 30
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:slots [0]}
               {:slots [0 0]}]}]
    ["Six" :six
     {:type :heraldry.charge-group.type/rows
      :strip-angle 0
      :spacing 30
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:slots [0 0 0]}
               {:slots [0 0]}
               {:slots [0]}]}]]
   ["Grid"
    ["Square" :square
     {:type :heraldry.charge-group.type/rows
      :strip-angle 0
      :spacing 25
      :stretch 1
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:slots [0 0 0]}
               {:slots [0 0 0]}
               {:slots [0 0 0]}]}]
    ["Diamond" :diamond
     {:type :heraldry.charge-group.type/rows
      :strip-angle 45
      :spacing 25
      :stretch 1
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:slots [0 0 0]}
               {:slots [0 0 0]}
               {:slots [0 0 0]}]}]
    ["Semy rows" :semy-rows
     {:type :heraldry.charge-group.type/rows
      :strip-angle 0
      :spacing 20
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:slots [0 0 0 0]}
               {:slots [0 0 0]}
               {:slots [0 0 0 0]}
               {:slots [0 0 0]}
               {:slots [0 0 0 0]}]}]
    ["Semy columns" :semy-columns
     {:type :heraldry.charge-group.type/columns
      :strip-angle 0
      :spacing 17
      :stretch 0.866
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:slots [0 0 0 0]}
               {:slots [0 0 0]}
               {:slots [0 0 0 0]}
               {:slots [0 0 0]}
               {:slots [0 0 0 0]}]}]
    ["Frame" :frame
     {:type :heraldry.charge-group.type/rows
      :strip-angle 0
      :spacing 17
      :stretch 1
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :strips [{:slots [0 0 0 0 0]}
               {:slots [0 nil nil nil 0]}
               {:slots [0 nil nil nil 0]}
               {:slots [0 nil nil nil 0]}
               {:slots [0 0 0 0 0]}]}]]
   ["Arc"
    ["In annullo" :in-annullo
     {:type :heraldry.charge-group.type/arc
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :slots [0 0 0 0 0 0 0]}]
    ["Arc" :arc
     {:type :heraldry.charge-group.type/arc
      :start-angle 10
      :arc-angle 180
      :charges [{:type :heraldry.charge.type/roundel
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}}]
      :slots [0 0 0 0 0]}]
    ["In annullo (point to center)" :arc-point-to-center
     {:type :heraldry.charge-group.type/arc
      :rotate-charges? true
      :charges [{:type :heraldry.charge.type/billet
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}
                 :anchor {:point :angle
                          :angle -180}}]
      :slots [0 0 0 0 0 0 0]}
     {[:charges 0 :anchor :point] :angle
      [:charges 0 :anchor :angle] -180}]
    ["In annullo (follow)" :in-annullo-follow
     {:type :heraldry.charge-group.type/arc
      :rotate-charges? true
      :charges [{:type :heraldry.charge.type/billet
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}
                 :anchor {:point :angle
                          :angle -90}}]
      :slots [0 0 0 0 0 0 0]}
     {[:charges 0 :anchor :point] :angle
      [:charges 0 :anchor :angle] -90}]
    ["Sheaf of" :sheaf-of
     {:type :heraldry.charge-group.type/arc
      :start-angle -45
      :arc-angle 90
      :radius 0
      :rotate-charges? true
      :charges [{:type :heraldry.charge.type/billet
                 :field {:type :heraldry.field.type/plain
                         :tincture :azure}
                 :geometry {:size 50
                            :stretch 3}}]
      :slots [0 0 0]}
     {[:charges 0 :anchor :point] :angle
      [:charges 0 :anchor :angle] 0
      [:charges 0 :geometry :size] 50}]]])

(defn calculate-strip-slot-positions [path spacing context]
  (let [stretch (interface/get-sanitized-data (conj path :stretch) context)
        offset (interface/get-sanitized-data (conj path :offset) context)
        slots (interface/get-raw-data (conj path :slots) context)
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
  (fn [path _environment context]
    (case (interface/get-sanitized-data (conj path :type) context)
      :heraldry.charge-group.type/rows :strips
      :heraldry.charge-group.type/columns :strips
      :heraldry.charge-group.type/arc :arc)))

(defmethod calculate-points :strips [path environment context]
  (let [charge-group-type (interface/get-sanitized-data (conj path :type) context)
        reference-length (case charge-group-type
                           :heraldry.charge-group.type/rows (:width environment)
                           :heraldry.charge-group.type/columns (:height environment))
        spacing (interface/get-sanitized-data (conj path :spacing) context)
        stretch (interface/get-sanitized-data (conj path :stretch) context)
        strip-angle (interface/get-sanitized-data (conj path :strip-angle) context)
        num-charges (interface/get-list-size (conj path :charges) context)
        num-strips (interface/get-list-size (conj path :strips) context)
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
                                                       (conj path :strips idx) spacing context)
                                       strip-position (-> idx
                                                          (* strip-spacing)
                                                          (- (/ length 2)))]
                                   (map (fn [{:keys [position charge-index slot-index]}]
                                          {:point (v/rotate (make-point-fn position
                                                                           strip-position)
                                                            strip-angle)
                                           :slot-path (conj path :strips idx :slots slot-index)
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

(defmethod calculate-points :arc [path environment context]
  (let [radius (interface/get-sanitized-data (conj path :radius) context)
        start-angle (interface/get-sanitized-data (conj path :start-angle) context)
        arc-angle (interface/get-sanitized-data (conj path :arc-angle) context)
        arc-stretch (interface/get-sanitized-data (conj path :arc-stretch) context)
        slots (interface/get-raw-data (conj path :slots) context)
        num-charges (interface/get-list-size (conj path :charges) context)
        num-slots (interface/get-list-size (conj path :slots) context)
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
                                            :slot-path (conj path :slots slot-index)
                                            :charge-index (if (and (int? charge-index)
                                                                   (< -1 charge-index num-charges))
                                                            charge-index
                                                            nil)
                                            :angle (+ slot-angle 90)})))
                          vec)
     :slot-spacing {:width (/ distance 1.2)
                    :height (/ distance 1.2)}}))

(defmethod interface/render-component :heraldry.component/charge-group [path parent-path environment context]
  (let [origin (interface/get-sanitized-data (conj path :origin) context)
        rotate-charges? (interface/get-sanitized-data (conj path :rotate-charges?) context)
        origin-point (position/calculate origin environment)
        {:keys [slot-positions
                slot-spacing]} (calculate-points path environment context)
        num-charges (interface/get-list-size (conj path :charges) context)]

    [:g
     (for [[idx {:keys [point charge-index angle]}] (map-indexed vector slot-positions)
           :when (and charge-index
                      (< charge-index num-charges))]
       (let [charge-path (conj path :charges charge-index)]
         ^{:key idx}
         [charge-interface/render-charge charge-path parent-path
          environment
          (-> context
              (assoc :origin-override (v/+ origin-point point))
              (assoc :charge-group {:charge-group-path path
                                    :slot-spacing slot-spacing
                                    :slot-angle (when rotate-charges?
                                                  angle)}))]))]))

(defmethod interface/blazon-component :heraldry.component/charge-group [path context]
  (let [{:keys [slot-positions]} (calculate-points path escutcheon/flag context)
        used-charges (->> (group-by :charge-index slot-positions)
                          (map (fn [[k v]]
                                 [k (count v)]))
                          (filter (fn [[k v]]
                                    (and k (pos? v))))
                          (sort-by second))
        context (assoc-in context [:blazonry :part-of-charge-group?] true)]
    (str "a group of "
         (util/combine
          " and "
          (map (fn [[charge-index number]]
                 (str number " "
                      (interface/blazon (conj path :charges charge-index)
                                        (cond-> context
                                          (> number 1) (assoc-in [:blazonry :pluralize?] true)))))
               used-charges)))))

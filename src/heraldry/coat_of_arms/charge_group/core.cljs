(ns heraldry.coat-of-arms.charge-group.core
  (:require [heraldry.coat-of-arms.charge-group.options :as charge-group-options]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.util :as util]
            [heraldry.coat-of-arms.vector :as v]))

(defn render [& opts]
  [:<>])

(defmulti calculate-points
  (fn [{:keys [type]} _]
    (case type
      :heraldry.charge-group.type/rows :strips
      :heraldry.charge-group.type/columns :strips)))

(defn calculate-slot-positions [{:keys [slots] :as strip} spacing]
  (let [{:keys [size
                stretch
                offset]} (options/sanitize strip charge-group-options/strip-options)
        slots (concat slots (repeat (-> size (- (count slots))) nil))
        spacing (* spacing
                   stretch)
        offset (* spacing
                  offset)
        length (+ (* (max 0 (dec size))
                     spacing)
                  offset)]
    (map-indexed (fn [idx charge-index]
                   {:position (-> idx
                                  (* spacing)
                                  (- (/ length 2))
                                  (+ offset))
                    :charge-index charge-index}) slots)))

(defmethod calculate-points :strips [{:keys [type strips charges] :as charge-group} environment]
  (let [reference-length (case type
                           :heraldry.charge-group.type/rows (:width environment)
                           :heraldry.charge-group.type/columns (:height environment))
        {:keys [spacing
                stretch
                strip-angle]} (options/sanitize charge-group charge-group-options/default-options)
        spacing ((util/percent-of reference-length) spacing)
        strip-spacing (* spacing
                         stretch)
        length (* (-> strips count dec (max 0))
                  strip-spacing)
        num-charges (count charges)
        make-point-fn (case type
                        :heraldry.charge-group.type/rows v/v
                        :heraldry.charge-group.type/columns (fn [y x] (v/v x y)))]
    (->> strips
         (map-indexed (fn [idx strip]
                        (let [slot-positions (calculate-slot-positions strip spacing)
                              strip-position (-> idx
                                                 (* strip-spacing)
                                                 (- (/ length 2)))]
                          (map (fn [{:keys [position charge-index]}]
                                 {:point (make-point-fn position
                                                        strip-position)
                                  :charge-index (if (and (int? charge-index)
                                                         (< -1 charge-index num-charges))
                                                  charge-index
                                                  nil)})
                               slot-positions))))
         (apply concat)
         vec)))

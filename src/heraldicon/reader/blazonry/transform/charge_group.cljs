(ns heraldicon.reader.blazonry.transform.charge-group
  (:require
   [heraldicon.reader.blazonry.transform.fimbriation :refer [add-fimbriation]]
   [heraldicon.reader.blazonry.transform.shared :refer [ast->hdn transform-first get-child filter-nodes]]
   [heraldicon.reader.blazonry.transform.tincture-modifier :refer [add-tincture-modifiers]]))

(def ^:private max-charge-group-columns 20)

(def ^:private max-charge-group-rows 20)

(def ^:private charge-locations
  {:point/DEXTER :dexter
   :point/SINISTER :sinister
   :point/CHIEF :chief
   :point/BASE :base
   :point/FESS :fess
   :point/HONOUR :honour
   :point/NOMBRIL :nombril
   :point/HOIST :hoist
   :point/FLY :fly})

(defmethod ast->hdn :charge-location [[_ & nodes]]
  (some-> (get-child charge-locations nodes)
          first
          charge-locations))

(defn- charge-group [charge amount nodes]
  (let [[arrangement-type
         & arrangement-nodes] (second (get-child #{:charge-arrangement} nodes))]
    (cond-> {:charges [charge]}
      (nil? arrangement-type) (merge
                               {::default-charge-group-amount amount})
      (= :arrangement/FESSWISE
         arrangement-type) (merge
                            {:type :heraldry.charge-group.type/rows
                             :spacing (/ 95 amount)
                             :strips [{:type :heraldry.charge-group.element.type/strip
                                       :slots (vec (repeat amount 0))}]})
      (= :arrangement/PALEWISE
         arrangement-type) (merge
                            {:type :heraldry.charge-group.type/columns
                             :anchor {:point :center}
                             :spacing (/ 95 amount)
                             :strips [{:type :heraldry.charge-group.element.type/strip
                                       :slots (vec (repeat amount 0))}]})
      (= :arrangement/BENDWISE
         arrangement-type) (merge
                            {:type :heraldry.charge-group.type/rows
                             :strip-angle 45
                             :spacing (/ 120 amount)
                             :strips [{:type :heraldry.charge-group.element.type/strip
                                       :slots (vec (repeat amount 0))}]})
      (= :arrangement/BENDWISE-SINISTER
         arrangement-type) (merge
                            {:type :heraldry.charge-group.type/rows
                             :strip-angle -45
                             :spacing (/ 120 amount)
                             :strips [{:type :heraldry.charge-group.element.type/strip
                                       :slots (vec (repeat amount 0))}]})
      (= :arrangement/CHEVRONWISE
         arrangement-type) (merge
                            {:type :heraldry.charge-group.type/rows
                             :spacing (/ 90 amount)
                             :strips (->> (range (-> amount
                                                     inc
                                                     (/ 2)))
                                          (map (fn [index]
                                                 {:type :heraldry.charge-group.element.type/strip
                                                  :stretch (if (and (zero? index)
                                                                    (even? amount))
                                                             1
                                                             (if (and (pos? index)
                                                                      (even? amount))
                                                               (+ 1 (/ (inc index)
                                                                       index))
                                                               2))
                                                  :slots (if (zero? index)
                                                           (if (odd? amount)
                                                             [0]
                                                             [0 0])
                                                           (-> (concat [0]
                                                                       (repeat (dec index) nil)
                                                                       [0])
                                                               vec))}))
                                          vec)})
      (= :arrangement/IN-ORLE
         arrangement-type) (merge
                            {:type :heraldry.charge-group.type/in-orle
                             :slots (vec (repeat amount 0))})
      (= :arrangement/IN-ANNULLO
         arrangement-type) (merge
                            {:type :heraldry.charge-group.type/arc
                             :slots (vec (repeat amount 0))})
      (= :charge-grid
         arrangement-type) (merge
                            (let [amounts (->> arrangement-nodes
                                               (filter-nodes #{:amount})
                                               (map #(-> %
                                                         ast->hdn
                                                         (min max-charge-group-columns)))
                                               (take max-charge-group-rows))
                                  width (apply max amounts)
                                  height (count amounts)]
                              {:type :heraldry.charge-group.type/rows
                               :anchor {:point :center}
                               :spacing (/ 95 (max width height))
                               :strips (mapv (fn [amount]
                                               {:type :heraldry.charge-group.element.type/strip
                                                :slots (vec (repeat amount 0))})
                                             amounts)})))))

(def ^:private max-charge-group-amount 64)

(defmethod ast->hdn :charge-group [[_ & nodes]]
  (let [amount (-> (transform-first #{:amount} nodes)
                   (or 1)
                   (max 1)
                   (min max-charge-group-amount))
        anchor-point (transform-first #{:charge-location} nodes)
        field (transform-first #{:field} nodes)
        charge (-> (transform-first #{:charge-without-fimbriation} nodes)
                   (assoc :field field)
                   (add-tincture-modifiers nodes)
                   (add-fimbriation nodes))]
    [(cond-> (if (= amount 1)
               charge
               (charge-group charge amount nodes))
       anchor-point (assoc-in [:anchor :point] anchor-point))]))

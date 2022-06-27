(ns heraldicon.reader.blazonry.transform
  (:require
   [clojure.string :as s]
   [heraldicon.heraldry.charge.options :as charge.options]
   [heraldicon.heraldry.option.attributes :as attributes]
   [heraldicon.reader.blazonry.transform.amount] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.cottising] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.field] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.field.partition] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.field.partition.field] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.field.plain] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.fimbriation :refer [add-fimbriation]]
   [heraldicon.reader.blazonry.transform.line] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.ordinal] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.ordinary] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.shared :refer [ast->hdn get-child type?]]
   [heraldicon.reader.blazonry.transform.tincture] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.tincture-modifier :refer [add-tincture-modifiers]]))

(def ^:private max-layout-amount 50)

(defmethod ast->hdn :horizontal-layout [[_ & nodes]]
  (let [amount (ast->hdn (get-child #{:amount} nodes))]
    {:num-fields-x (min max-layout-amount amount)}))

(defmethod ast->hdn :vertical-layout-implicit [[_ & nodes]]
  (let [amount (ast->hdn (get-child #{:amount} nodes))]
    {:num-fields-y (min max-layout-amount amount)}))

(defmethod ast->hdn :vertical-layout-explicit [[_ & nodes]]
  (let [amount (ast->hdn (get-child #{:amount} nodes))]
    {:num-fields-y (min max-layout-amount amount)}))

(defmethod ast->hdn :vertical-layout [[_ node]]
  (ast->hdn node))

(defmethod ast->hdn :layout [[_ & nodes]]
  (let [layouts (->> nodes
                     (filter (type? #{:horizontal-layout
                                      :vertical-layout
                                      :vertical-layout-explicit
                                      :vertical-layout-implicit}))
                     (map ast->hdn))]
    (apply merge layouts)))

(def ^:private max-ordinary-group-amount 20)

(defmethod ast->hdn :ordinary-group [[_ & nodes]]
  (let [amount-node (get-child #{:amount} nodes)
        amount (if amount-node
                 (ast->hdn amount-node)
                 1)
        ordinary (ast->hdn (get-child #{:ordinary} nodes))]
    (vec (repeat (-> amount
                     (max 1)
                     (min max-ordinary-group-amount)) ordinary))))

(def ^:private attitude-map
  (->> attributes/attitude-map
       (map (fn [[key _]]
              [(->> key
                    name
                    s/upper-case
                    (keyword "attitude"))
               key]))
       (into {})))

(defmethod ast->hdn :attitude [[_ & nodes]]
  (some->> nodes
           (get-child attitude-map)
           first
           (get attitude-map)))

(def ^:private facing-map
  (into {}
        (map (fn [[key _]]
               [(keyword "facing" (-> key name s/upper-case))
                key]))
        attributes/facing-map))

(defmethod ast->hdn :facing [[_ & nodes]]
  (some->> nodes
           (get-child facing-map)
           first
           (get facing-map)))

(def ^:private max-star-points 100)

(defn- add-charge-options [hdn nodes]
  (let [charge-options (some->> nodes
                                (filter (type? #{:MIRRORED
                                                 :REVERSED
                                                 :star-points}))
                                (map (fn [[key & nodes]]
                                       [key nodes]))
                                (into {}))
        attitude (some-> (get-child #{:attitude} nodes)
                         ast->hdn)
        facing (some-> (get-child #{:facing} nodes)
                       ast->hdn)
        charge-type (-> hdn :type name keyword)]
    (cond-> hdn
      (get charge-options :MIRRORED) (assoc-in [:geometry :mirrored?] true)
      (get charge-options :REVERSED) (assoc-in [:geometry :reversed?] true)
      (get charge-options :star-points) (cond->
                                          (= :star
                                             charge-type) (assoc :num-points
                                                                 (->> (get charge-options :star-points)
                                                                      (get-child #{:amount})
                                                                      ast->hdn
                                                                      (min max-star-points))))
      attitude (assoc :attitude attitude)
      facing (assoc :facing facing))))

(def ^:private charge-type-map
  (into {:charge/ESTOILE :heraldry.charge.type/star}
        (map (fn [[key _]]
               [(keyword "charge" (-> key name s/upper-case))
                key]))
        charge.options/charge-map))

(defmethod ast->hdn :charge-standard [[_ & nodes]]
  (let [charge-type-node-kind (first (get-child charge-type-map nodes))
        charge-type (get charge-type-map charge-type-node-kind)]
    (-> {:type charge-type}
        (add-charge-options nodes)
        (cond->
          (= charge-type-node-kind :charge/ESTOILE) (assoc :wavy-rays? true)))))

(defmethod ast->hdn :charge-other-type [[_ charge-other-type-node]]
  (some->> charge-other-type-node
           first
           name
           (keyword "heraldry.charge.type")))

(defmethod ast->hdn :charge-other [[_ & nodes]]
  (let [charge-type (ast->hdn (get-child #{:charge-other-type} nodes))]
    (add-charge-options
     {:type charge-type
      :variant {:id "charge:N87wec"
                :version 0}}
     nodes)))

(defmethod ast->hdn :charge-without-fimbriation [[_ & nodes]]
  (-> (get-child #{:charge-standard
                   :charge-other} nodes)
      ast->hdn))

(defmethod ast->hdn :charge [[_ & nodes]]
  (-> (get-child #{:charge-without-fimbriation} nodes)
      ast->hdn
      (add-fimbriation nodes)))

(def ^:private max-charge-group-columns 20)

(def ^:private max-charge-group-rows 20)

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
                                               (filter (type? #{:amount}))
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

(def ^:private max-charge-group-amount 64)

(defmethod ast->hdn :charge-group [[_ & nodes]]
  (let [amount-node (get-child #{:amount} nodes)
        amount (if amount-node
                 (ast->hdn amount-node)
                 1)
        amount (-> amount
                   (max 1)
                   (min max-charge-group-amount))
        anchor-point (some-> (get-child #{:charge-location} nodes)
                             ast->hdn)
        field (ast->hdn (get-child #{:field} nodes))
        charge (-> #{:charge-without-fimbriation}
                   (get-child nodes)
                   ast->hdn
                   (assoc :field field)
                   (add-tincture-modifiers nodes)
                   (add-fimbriation nodes))]
    [(cond-> (if (= amount 1)
               charge
               (charge-group charge amount nodes))
       anchor-point (assoc-in [:anchor :point] anchor-point))]))

(defn- semy [charge nodes]
  (let [layout (some-> (get-child #{:layout
                                    :horizontal-layout
                                    :vertical-layout} nodes)
                       ast->hdn)]
    (cond-> {:type :heraldry/semy
             :charge charge}
      layout (assoc :layout layout))))

(defmethod ast->hdn :semy [[_ & nodes]]
  (let [field (ast->hdn (get-child #{:field} nodes))
        charge (-> #{:charge-without-fimbriation}
                   (get-child nodes)
                   ast->hdn
                   (assoc :field field)
                   (add-tincture-modifiers nodes)
                   (add-fimbriation nodes))]
    [(semy charge nodes)]))

(defn transform [ast]
  (ast->hdn ast))

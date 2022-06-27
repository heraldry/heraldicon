(ns heraldicon.reader.blazonry.transform
  (:require
   [clojure.string :as s]
   [heraldicon.heraldry.charge.options :as charge.options]
   [heraldicon.heraldry.option.attributes :as attributes]
   [heraldicon.reader.blazonry.transform.amount] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.charge-group] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.cottising] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.field] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.field.partition] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.field.partition.field] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.field.plain] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.fimbriation :refer [add-fimbriation]]
   [heraldicon.reader.blazonry.transform.layout] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.line] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.ordinal] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.ordinary] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.ordinary-group] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.shared :refer [ast->hdn get-child type?]]
   [heraldicon.reader.blazonry.transform.tincture] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.tincture-modifier :refer [add-tincture-modifiers]]))

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

(ns heraldicon.reader.blazonry.transform.charge
  (:require
   [clojure.string :as s]
   [heraldicon.heraldry.charge.options :as charge.options]
   [heraldicon.heraldry.option.attributes :as attributes]
   [heraldicon.reader.blazonry.transform.fimbriation :refer [add-fimbriation]]
   [heraldicon.reader.blazonry.transform.shared :refer [ast->hdn get-child filter-nodes]]))

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
                                (filter-nodes #{:MIRRORED
                                                :REVERSED
                                                :star-points})
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

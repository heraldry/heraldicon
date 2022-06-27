(ns heraldicon.reader.blazonry.transform.tincture-modifier
  (:require
   [clojure.string :as s]
   [heraldicon.heraldry.option.attributes :as attributes]
   [heraldicon.reader.blazonry.transform.shared :refer [ast->hdn get-child filter-nodes]]))

(def ^:private tincture-modifier-type-map
  (->> attributes/tincture-modifier-map
       (map (fn [[key _]]
              [(->> key
                    name
                    s/upper-case
                    (keyword "tincture-modifier"))
               key]))
       (into {})))

(defn- get-tincture-modifier-type [nodes]
  (let [node-type (first (get-child tincture-modifier-type-map nodes))]
    (get tincture-modifier-type-map node-type)))

(defmethod ast->hdn :tincture-modifier-type [[_ & nodes]]
  (get-tincture-modifier-type nodes))

(defmethod ast->hdn :tincture-modifier [[_ & nodes]]
  (let [modifier-type (ast->hdn (get-child #{:tincture-modifier-type} nodes))
        tincture (ast->hdn (get-child #{:tincture} nodes))]
    [modifier-type tincture]))

(defn add-tincture-modifiers [hdn nodes]
  (let [modifiers (->> nodes
                       (filter-nodes #{:tincture-modifier})
                       (map ast->hdn)
                       (into {}))]
    (cond-> hdn
      (seq modifiers) (assoc :tincture modifiers))))

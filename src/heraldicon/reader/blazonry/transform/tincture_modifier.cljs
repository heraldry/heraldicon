(ns heraldicon.reader.blazonry.transform.tincture-modifier
  (:require
   [clojure.string :as str]
   [heraldicon.heraldry.option.attributes :as attributes]
   [heraldicon.reader.blazonry.transform.shared :refer [ast->hdn get-child transform-first transform-all]]))

(def ^:private tincture-modifier-type-map
  (->> attributes/tincture-modifier-map
       (map (fn [[key _]]
              [(->> key
                    name
                    str/upper-case
                    (keyword "tincture-modifier"))
               key]))
       (into {})))

(defn- get-tincture-modifier-type [nodes]
  (let [node-type (first (get-child tincture-modifier-type-map nodes))]
    (get tincture-modifier-type-map node-type)))

(defmethod ast->hdn :tincture-modifier-type [[_ & nodes]]
  (get-tincture-modifier-type nodes))

(defmethod ast->hdn :tincture-modifier [[_ & nodes]]
  (let [modifier-types (transform-all #{:tincture-modifier-type} nodes)
        tincture (transform-first #{:tincture} nodes)]
    (for [modifier-type modifier-types]
      [modifier-type tincture])))

(defn add-tincture-modifiers [hdn nodes]
  (let [modifiers (into {} (apply concat (transform-all #{:tincture-modifier} nodes)))]
    (cond-> hdn
      (seq modifiers) (assoc :tincture modifiers))))

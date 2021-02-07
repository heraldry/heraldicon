(ns heraldry.frontend.charge-map
  (:require [clojure.set :as set]
            [clojure.walk :as walk]))

(def group-map
  {:node-type :_root
   :groups {:beasts {:node-type :group
                     :name "beasts"
                     :groups {:predators {:node-type :group
                                          :name "predators"
                                          :charges #{:lion :wolf :bear}}
                              :ungulates {:node-type :group
                                          :name "ungulates"
                                          :charges #{:antelope :deer :boar :horse}}
                              :reptiles {:node-type :group
                                         :name "reptiles"
                                         :charges #{:lizard :serpent}}
                              :insects {:node-type :group
                                        :name "insects"
                                        :charges #{:bee :butterfly}}
                              :hybrids {:node-type :group
                                        :name "hybrids"
                                        :charges #{:sphinx :griffin :unicorn}}
                              :birds {:node-type :group
                                      :name "birds"
                                      :charges #{:dove :crane :eagle :owl :corvus}}}}}})

(def known-charge-types
  (walk/postwalk (fn [data]
                   (cond
                     (:charges data) (:charges data)
                     (:groups data) (:groups data)
                     (map? data) (->> data
                                      (map second)
                                      (apply set/union))
                     :else data)) group-map))

(defn count-variants [node]
  (cond
    (-> node nil?) 0
    (-> node :node-type (= :variant)) 1
    :else (->> [:groups :charges :attitudes :variants]
               (map (fn [key]
                      (-> node
                          (get key)
                          (->> (map (fn [[_ v]]
                                      (count-variants v)))
                               (reduce +)))))
               (reduce +))))

(defn build-charge-variants [node groupable-types charges]
  (let [type (first groupable-types)
        remaining-types (drop 1 groupable-types)
        groups (if type
                 (group-by type charges)
                 {})
        nil-group (if type
                    (get groups nil)
                    charges)
        non-nil-groups (when type
                         (into {}
                               (filter first groups)))]
    (-> node
        (cond->
         non-nil-groups (assoc (-> type
                                   name
                                   (str "s")
                                   keyword)
                               (->> non-nil-groups
                                    (map (fn [[value grouped-charges]]
                                           [value (build-charge-variants
                                                   {:node-type type
                                                    :type value
                                                    :name (name value)}
                                                   remaining-types
                                                   grouped-charges)]))
                                    (into {})))
         (and nil-group
              (-> remaining-types
                  seq)) (build-charge-variants remaining-types nil-group)
         (and nil-group
              (-> remaining-types
                  empty?)) (assoc :variants (->> nil-group
                                                 (map (fn [charge]
                                                        [(:id charge) {:node-type :variant
                                                                       :name (:name charge)
                                                                       :data charge}]))
                                                 (into {})))))))

(defn remove-empty-groups [charge-map]
  (walk/postwalk (fn [data]
                   (cond
                     (:node-type data) (let [c (count-variants data)]
                                         (if (zero? c)
                                           nil
                                           data))
                     (map? data) (->> data
                                      (filter second)
                                      (into {}))
                     :else data)) charge-map))

(defn build-map [charges-by-type group-map & {:keys [remove-empty-groups?]}]
  (let [charge-map (walk/postwalk
                    (fn [data]
                      (if (and (-> data seqable?)
                               (-> data first (= :charges)))
                        (let [charge-types (second data)]
                          [:charges (->> charge-types
                                         (map (fn [type]
                                                [type (build-charge-variants
                                                       {:node-type :charge
                                                        :type type
                                                        :name (name type)}
                                                       [:attitude]
                                                       (get charges-by-type type))]))

                                         (into {}))])
                        data)) group-map)
        ungrouped-types (-> charges-by-type
                            keys
                            set
                            (set/difference known-charge-types))]
    (cond-> charge-map
      (seq ungrouped-types) (assoc-in [:groups :ungrouped]
                                      {:node-type :group
                                       :name "ungrouped"
                                       :charges (->> ungrouped-types
                                                     (map (fn [type]
                                                            [type (build-charge-variants
                                                                   {:node-type :charge
                                                                    :type type
                                                                    :name (name type)}
                                                                   [:attitude]
                                                                   (get charges-by-type type))]))
                                                     (into {}))})
      remove-empty-groups? remove-empty-groups)))

(defn build-charge-map [charges & {:keys [remove-empty-groups?]}]
  (-> charges
      (->> (group-by :type))
      (build-map group-map :remove-empty-groups? remove-empty-groups?)))

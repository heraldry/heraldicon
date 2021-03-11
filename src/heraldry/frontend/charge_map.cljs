(ns heraldry.frontend.charge-map
  (:require [clojure.set :as set]
            [clojure.walk :as walk]))

(def group-map
  {:node-type :_root
   :groups {:animals {:node-type :group
                      :name "animals"
                      :groups {:beasts {:node-type :group
                                        :name "beasts"
                                        :groups {:predators {:node-type :group
                                                             :name "predators"
                                                             :charges #{:bear
                                                                        :lion
                                                                        :wolf}}
                                                 :ungulates {:node-type :group
                                                             :name "ungulates"
                                                             :charges #{:antelope
                                                                        :boar
                                                                        :deer
                                                                        :horse}}
                                                 :reptiles {:node-type :group
                                                            :name "reptiles"
                                                            :charges #{:lizard
                                                                       :serpent}}
                                                 :hybrids {:node-type :group
                                                           :name "hybrids"
                                                           :charges #{:griffin
                                                                      :sphinx
                                                                      :unicorn}}}}

                               :insects {:node-type :group
                                         :name "insects"
                                         :charges #{:bee
                                                    :butterfly}}
                               :marine {:node-type :group
                                        :name "marine"
                                        :charges #{:dolphin
                                                   :fish
                                                   :pike}}
                               :birds {:node-type :group
                                       :name "birds"
                                       :charges #{:crane
                                                  :dove
                                                  :eagle
                                                  :owl
                                                  :raven}}}}
            :attire {:node-type :group
                     :name "attire"
                     :groups {:head {:node-type :group
                                     :name "head"
                                     :charges #{:cap
                                                :crown
                                                :hat
                                                :helmet
                                                :mitre}}}
                     :charges #{:buckle
                                :maunch
                                :spurs}}
            :symbols {:node-type :group
                      :name "symbols"
                      :charges #{:feather
                                 :heart
                                 :horseshoe
                                 :torch}}
            :tools {:node-type :group
                    :name "tools"
                    :charges #{:anvil
                               :bucket
                               :drinking-horn
                               :ferule
                               :firesteel
                               :hammer
                               :handmill
                               :harrow
                               :knife
                               :hunting-horn
                               :ladder
                               :millrind
                               :millwheel
                               :nail
                               :rake
                               :scales
                               :scissors
                               :scythe
                               :shears
                               :shoemaker-tool
                               :sickle
                               :tassle
                               :tongs
                               :trivet
                               :water-bouget
                               :wolfseisen}}
            :weapons {:node-type :group
                      :name "weapons"
                      :charges #{:arrow
                                 :axe
                                 :bow
                                 :caltrop
                                 :crossbow
                                 :cutlass
                                 :dagger
                                 :falchion
                                 :lance
                                 :mace
                                 :scimitar
                                 :spear
                                 :sword
                                 :trident}}}})

(def known-charge-types
  (->> group-map
       (tree-seq map? vals)
       (filter #(and (map? %)
                     (:charges %)))
       (map :charges)
       (apply set/union)))

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

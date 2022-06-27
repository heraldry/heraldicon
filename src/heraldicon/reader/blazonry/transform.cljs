(ns heraldicon.reader.blazonry.transform
  (:require
   [clojure.set :as set]
   [clojure.string :as s]
   [clojure.walk :as walk]
   [heraldicon.heraldry.charge.options :as charge.options]
   [heraldicon.heraldry.default :as default]
   [heraldicon.heraldry.field.core :as field]
   [heraldicon.heraldry.field.options :as field.options]
   [heraldicon.heraldry.option.attributes :as attributes]
   [heraldicon.heraldry.ordinary.options :as ordinary.options]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.reader.blazonry.transform.field] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.shared :refer [ast->hdn get-child type?]]
   [heraldicon.util.number :as number]))

(def ^:private tincture-map
  (into {:tincture/PROPER :void}
        (map (fn [[key _]]
               [(keyword "tincture" (-> key name s/upper-case))
                key]))
        tincture/tincture-map))

(defmethod ast->hdn :ordinal [[_ & nodes]]
  (->> nodes
       (tree-seq (some-fn map? vector? seq?) seq)
       (filter string?)
       first
       number/ordinal-from-string))

(defmethod ast->hdn :tincture [[_ & nodes]]
  (let [ordinal (some-> (get-child #{:ordinal} nodes)
                        ast->hdn)
        field (get-child #{:FIELD} nodes)
        same (get-child #{:SAME} nodes)]
    (cond
      field {::tincture-field-reference true}
      ordinal {::tincture-ordinal-reference ordinal}
      same (->> same
                rest
                (filter map?)
                first)
      :else (get tincture-map (ffirst nodes) :void))))

(defmethod ast->hdn :plain [[_ & nodes]]
  (let [node (get-child #{:tincture
                          :COUNTERCHANGED} nodes)]
    (case (first node)
      :COUNTERCHANGED {:type :heraldry.field.type/counterchanged}
      {:type :heraldry.field.type/plain
       :tincture (ast->hdn node)})))

(defmethod ast->hdn :line-type [[_ node]]
  (let [raw-type (-> node
                     first
                     name
                     s/lower-case
                     keyword)]
    (get {:rayonny :rayonny-flaming}
         raw-type raw-type)))

(defmethod ast->hdn :fimbriation [[_ & nodes]]
  (let [[tincture-1
         tincture-2] (->> nodes
                          (filter (type? #{:tincture}))
                          (take 2)
                          (mapv ast->hdn))]
    (if-not tincture-2
      {:mode :single
       :tincture-1 tincture-1}
      {:mode :double
       :tincture-1 tincture-1
       :tincture-2 tincture-2})))

(defn- add-fimbriation [hdn nodes & {:keys [line-fimbriation?]}]
  (let [fimbriation (some-> (get-child #{:fimbriation} nodes)
                            ast->hdn)
        path (if line-fimbriation?
               [:line :fimbriation]
               [:fimbriation])]
    (cond-> hdn
      fimbriation (assoc-in path fimbriation))))

(defmethod ast->hdn :line [[_ & nodes]]
  (let [line-type (get-child #{:line-type} nodes)]
    (-> nil
        (cond->
          line-type (assoc :type (ast->hdn line-type)))
        (add-fimbriation nodes))))

(defn- add-lines [hdn nodes]
  (let [[line
         opposite-line
         extra-line] (->> nodes
                          (filter (type? #{:line}))
                          (mapv ast->hdn))]
    (cond-> hdn
      line (assoc :line line)
      opposite-line (assoc :opposite-line opposite-line)
      extra-line (assoc :extra-line extra-line))))

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

(def ^:private field-type-map
  (into {}
        (map (fn [[key _]]
               [(keyword "partition" (-> key name s/upper-case))
                key]))
        field.options/field-map))

(defn- get-field-type [nodes]
  (some->> nodes
           (get-child field-type-map)
           first
           (get field-type-map)))

(defn- add-partition-options [hdn nodes]
  (let [partition-options (some->> nodes
                                   (filter (type? #{:ENHANCED
                                                    :DEHANCED
                                                    :REVERSED}))
                                   (map (fn [[key & nodes]]
                                          [key nodes]))
                                   (into {}))
        partition-type (-> hdn :type name keyword)]
    (cond-> hdn
      (get partition-options :ENHANCED) (cond->
                                          (#{:per-fess
                                             :per-saltire
                                             :tierced-per-pall
                                             :tierced-per-fess
                                             :quartered
                                             :gyronny}
                                           partition-type) (assoc-in [:anchor :offset-y] 12.5)
                                          (#{:per-bend
                                             :per-bend-sinister}
                                           partition-type) (->
                                                             (assoc-in [:anchor :offset-y] 30)
                                                             (assoc-in [:orientation :offset-y] 30))
                                          (= :per-chevron
                                             partition-type) (->
                                                               (assoc-in [:anchor :offset-y] 15)
                                                               (assoc-in [:orientation :point] :angle)
                                                               (assoc-in [:orientation :angle] 45)))
      (get partition-options :DEHANCED) (cond->
                                          (#{:per-fess
                                             :per-saltire
                                             :tierced-per-pall
                                             :tierced-per-fess
                                             :quartered
                                             :gyronny}
                                           partition-type) (assoc-in [:anchor :offset-y] -12.5)
                                          (#{:per-bend
                                             :per-bend-sinister}
                                           partition-type) (->
                                                             (assoc-in [:anchor :offset-y] -30)
                                                             (assoc-in [:orientation :offset-y] -30))
                                          (= :per-chevron
                                             partition-type) (->
                                                               (assoc-in [:anchor :offset-y] -15)
                                                               (assoc-in [:orientation :point] :angle)
                                                               (assoc-in [:orientation :angle] 45)))
      (get partition-options :REVERSED) (cond->
                                          (= :per-chevron
                                             partition-type) (assoc-in [:origin :point] :chief)
                                          (= :tierced-per-pall
                                             partition-type) (assoc-in [:origin :point] :bottom)
                                          (= :per-pile
                                             partition-type) (assoc-in [:anchor :point] :top)))))

(def ^:private field-locations
  {:point/DEXTER :dexter
   :point/SINISTER :sinister
   :point/CHIEF :chief
   :point/BASE :base
   :point/FESS :fess
   :point/DEXTER-CHIEF :chief-dexter
   :point/CHIEF-DEXTER :chief-dexter
   :point/SINISTER-CHIEF :chief-sinister
   :point/CHIEF-SINISTER :chief-sinister
   :point/DEXTER-BASE :base-dexter
   :point/BASE-DEXTER :base-dexter
   :point/SINISTER-BASE :base-sinister
   :point/BASE-SINISTER :base-sinister})

(defmethod ast->hdn :field-location [[_ & nodes]]
  (some-> (get-child field-locations nodes)
          first
          field-locations))

(defmethod ast->hdn :field-reference [[_ & nodes]]
  (let [number (some-> (get-child #{:ordinal} nodes)
                       ast->hdn)
        location (some-> (get-child #{:field-location} nodes)
                         ast->hdn)]
    (or number
        location)))

(def ^:private field-reference-map
  {:barry {}
   :bendy {}
   :bendy-sinister {}
   :chequy {}
   :chevronny {}
   :fretty {}
    ;; TODO: gyronny names
   :gyronny {}
   :lozengy {}
   :masony {}
   :paly {}
   :papellony {}
   :per-bend {:chief 0
              :base 1}
   :per-bend-sinister {:chief 0
                       :base 1}
    ;; TODO: inverted needs to swap these
   :per-chevron {:chief 0
                 :base 1}
   :per-fess {:chief 0
              :base 1}
   :per-pale {:dexter 0
              :sinister 1}
    ;; TODO: inverted needs to swap these
   :per-pile {:chief 0
              :dexter 1
              :sinister 2}
   :per-saltire {:chief 0
                 :dexter 1
                 :sinister 2
                 :base 3}
   :potenty {}
   :quartered {:chief-dexter 0
               :chief-sinister 1
               :base-dexter 2
               :base-sinister 3}
   :tierced-per-fess {:chief 0
                      :fess 1
                      :base 2}
   :tierced-per-pale {:dexter 0
                      :fess 1
                      :sinister 2}
   ;; TODO: inverted needs to swap these
   :tierced-per-pall {:chief 0
                      :dexter 1
                      :sinister 2}
   :vairy {}})

(defn- translate-field-reference [reference field-type]
  (if (keyword? reference)
    (get-in field-reference-map [(some-> field-type name keyword) reference] reference)
    (dec reference)))

(defmethod ast->hdn :partition-field [[_ & nodes]]
  (let [field (ast->hdn (get-child #{:field :plain} nodes))
        references (->> nodes
                        (filter (type? #{:field-reference}))
                        (map ast->hdn))]
    {:references references
     :field field}))

(defn- field-reference-sort-key [reference]
  (if (int? reference)
    [0 reference]
    ;; keyword references
    [1 reference]))

(defn- sanitize-referenced-fields [fields num-mandatory-fields]
  (let [fields (map-indexed
                (fn [index {:keys [references field] :as field-data}]
                  (update (if (empty? references)
                            {:references [index]
                             :field field}
                            field-data)
                          :references #(->> % set (sort-by field-reference-sort-key)))) fields)]
    (persistent!
     (reduce
      (fn [result {:keys [references field]}]
        (let [integer-references (filter int? references)
              [seen-references
               new-references] (split-with
                                #(get result %)
                                integer-references)
              first-new-reference (first new-references)]
          (doseq [reference seen-references]
            (assoc! result reference
                    (conj (get result reference [])
                          field)))
          (when first-new-reference
            (assoc! result first-new-reference
                    (conj (get result first-new-reference [])
                          field))
            (doseq [reference (rest new-references)]
              (assoc! result reference
                      (conj (get result reference [])
                            (if (< reference num-mandatory-fields)
                              field
                              first-new-reference))))))

        (let [keyword-references (filter keyword? references)]
          (doseq [reference keyword-references]
            (assoc! result reference
                    (conj (get result reference [])
                          field))))

        result)
      (transient {})
      fields))))

(defn- resolve-reference-chains [fields reference-map]
  (loop [fields (vec fields)
         [index & rest] (range (count fields))]
    (if index
      (if-let [reference (-> fields (get index) :index)]
        (let [real-reference (first (get reference-map reference))]
          (if (int? real-reference)
            (recur (assoc fields index {:type :heraldry.field.type/ref
                                        :index real-reference})
                   rest)
            (recur fields rest)))
        (recur fields rest))
      fields)))

(defn- populate-field-references [default-fields reference-map]
  (resolve-reference-chains
   (loop [fields (vec default-fields)
          [index & rest] (range (count default-fields))]
     (if index
       (let [new-field (first (get reference-map index))
             new-field (if (int? new-field)
                         {:type :heraldry.field.type/ref
                          :index new-field}
                         new-field)]
         (if new-field
           (recur (assoc fields index new-field) rest)
           ;; if the default field already is a reference, then we might have to
           (recur fields rest)))
       fields)) reference-map))

(defn- reference-to-string [reference]
  (if (keyword? reference)
    (name reference)
    (let [field-number (inc reference)]
      (str (or (number/to-roman field-number) field-number) "."))))

(defn- add-field-reference-warnings [hdn reference-map num-expected-field num-mandatory-fields]
  (let [unknown-references (->> reference-map
                                keys
                                (filter (fn [reference]
                                          (or (keyword? reference)
                                              (<= num-expected-field reference)))))
        multi-references (keep (fn [[k v]]
                                 (when (-> v count (> 1))
                                   k))
                               reference-map)
        missing-mandatory-references (set/difference
                                      (set (range num-mandatory-fields))
                                      (set (keys reference-map)))
        warnings (cond-> []
                   (seq missing-mandatory-references) (conj (str "Field"
                                                                 (when (-> missing-mandatory-references
                                                                           count
                                                                           (> 1))
                                                                   "s")
                                                                 " for partition missing: "
                                                                 (s/join ", " (map reference-to-string
                                                                                   (sort-by field-reference-sort-key
                                                                                            missing-mandatory-references)))))
                   (seq unknown-references) (conj (str "Field"
                                                       (when (-> unknown-references
                                                                 count
                                                                 (> 1))
                                                         "s")
                                                       " not found in partition: "
                                                       (s/join ", " (map reference-to-string
                                                                         (sort-by field-reference-sort-key
                                                                                  unknown-references)))))
                   (seq multi-references) (conj (str "Field"
                                                     (when (-> multi-references
                                                               count
                                                               (> 1))
                                                       "s")
                                                     " for partition mentioned more than once: "
                                                     (s/join ", " (map reference-to-string
                                                                       (sort-by field-reference-sort-key
                                                                                multi-references))))))]
    (cond-> hdn
      (seq warnings) (assoc ::warnings warnings))))

(defmethod ast->hdn :partition [[_ & nodes]]
  (let [field-type (get-field-type nodes)
        layout (some-> (get-child #{:layout
                                    :horizontal-layout
                                    :vertical-layout} nodes)
                       ast->hdn)
        field-type (if (and (= field-type :heraldry.field.type/gyronny)
                            layout
                            (-> layout :num-fields-x (not= 8)))
                     :heraldry.field.type/gyronny-n
                     field-type)
        ;; TODO: num-fields-x, num-fields-y, num-base-fields should be the defaults for the partition type
        default-fields (walk/postwalk
                        (fn [data]
                          (cond-> data
                            (and (map? data)
                                 (:tincture data)) (assoc :tincture :void)))
                        (field/raw-default-fields
                         field-type
                         (-> layout :num-fields-x (or 6))
                         (-> layout :num-fields-y (or 6))
                         2))
        given-fields (->> nodes
                          (filter (type? #{:partition-field}))
                          (map ast->hdn)
                          (map (fn [field-data]
                                 (update field-data
                                         :references
                                         (fn [references]
                                           (map #(translate-field-reference % field-type)
                                                references))))))
        num-mandatory-fields (case field-type
                               nil 0
                               :tierced-per-pale 3
                               :tierced-per-fess 3
                               :tierced-per-pall 3
                               :per-pile 3
                               2)
        reference-map (sanitize-referenced-fields
                       given-fields
                       ;; TODO: this could be DRYer
                       num-mandatory-fields)
        fields (populate-field-references default-fields reference-map)]
    (-> {:type field-type
         :fields fields}
        (add-field-reference-warnings reference-map (count fields) num-mandatory-fields)
        (add-lines nodes)
        (cond->
          layout (assoc :layout layout))
        (add-partition-options nodes))))

(defmethod ast->hdn :amount [[_ node]]
  (ast->hdn node))

(defmethod ast->hdn :A [_]
  1)

(defmethod ast->hdn :number/NUMBER [[_ number-string]]
  (js/parseInt number-string))

(defmethod ast->hdn :number-word [node]
  (->> node
       (tree-seq (some-fn map? vector? seq?) seq)
       (keep (fn [node]
               (when (and (vector? node)
                          (= (count node) 2)
                          (-> node second string?))
                 (second node))))
       (map (fn [s]
              (or (number/from-string s) 0)))
       (reduce +)))

(def ^:private max-label-points 20)

(defn- add-ordinary-options [hdn nodes]
  (let [ordinary-options (some->> nodes
                                  (filter (type? #{:HUMETTY
                                                   :VOIDED
                                                   :ENHANCED
                                                   :DEHANCED
                                                   :REVERSED
                                                   :THROUGHOUT
                                                   :TRUNCATED
                                                   :DOVETAILED
                                                   ;; nothing to do there, just here for reference
                                                   #_:DEXTER
                                                   :point/SINISTER
                                                   :label-points}))
                                  (map (fn [[key & nodes]]
                                         [key nodes]))
                                  (into {}))
        ordinary-type (-> hdn :type name keyword)]
    (cond-> hdn
      (get ordinary-options :HUMETTY) (assoc :humetty {:humetty? true})
      (get ordinary-options :VOIDED) (assoc :voided {:voided? true})
      (get ordinary-options :REVERSED) (cond->
                                         (= :chevron
                                            ordinary-type) (assoc-in [:origin :point] :chief)
                                         (= :pall
                                            ordinary-type) (assoc-in [:origin :point] :bottom)
                                         (= :pile
                                            ordinary-type) (assoc-in [:anchor :point] :bottom))
      (get ordinary-options :THROUGHOUT) (cond->
                                           (= :pile
                                              ordinary-type) (assoc-in [:geometry :stretch] 1))
      (get ordinary-options :ENHANCED) (cond->
                                         (#{:fess
                                            :cross
                                            :saltire
                                            :pall
                                            :gore
                                            :quarter
                                            :label}
                                          ordinary-type) (assoc-in [:anchor :offset-y] 12.5)
                                         (#{:bend
                                            :bend-sinister}
                                          ordinary-type) (->
                                                           (assoc-in [:anchor :offset-y] 30)
                                                           (assoc-in [:orientation :offset-y] 30))
                                         (= :chief
                                            ordinary-type) (assoc-in [:geometry :size] (- 25 10))
                                         (= :base
                                            ordinary-type) (assoc-in [:geometry :size] (+ 25 10))
                                         (= :chevron
                                            ordinary-type) (->
                                                             (assoc-in [:anchor :offset-y] 15)
                                                             (assoc-in [:orientation :point] :angle)
                                                             (assoc-in [:orientation :angle] 45))
                                         (= :point
                                            ordinary-type) (assoc-in [:geometry :height] (- 50 25)))
      (get ordinary-options :DEHANCED) (cond->
                                         (#{:fess
                                            :cross
                                            :saltire
                                            :pall
                                            :gore
                                            :quarter
                                            :label}
                                          ordinary-type) (assoc-in [:anchor :offset-y] -12.5)
                                         (#{:bend
                                            :bend-sinister}
                                          ordinary-type) (->
                                                           (assoc-in [:anchor :offset-y] -30)
                                                           (assoc-in [:orientation :offset-y] -30))
                                         (= :chief
                                            ordinary-type) (assoc-in [:geometry :size] (+ 25 10))
                                         (= :base
                                            ordinary-type) (assoc-in [:geometry :size] (- 25 10))
                                         (= :chevron
                                            ordinary-type) (->
                                                             (assoc-in [:anchor :offset-y] -15)
                                                             (assoc-in [:orientation :point] :angle)
                                                             (assoc-in [:orientation :angle] 45))
                                         (= :point
                                            ordinary-type) (assoc-in [:geometry :height] (+ 50 25)))
      (get ordinary-options :TRUNCATED) (cond->
                                          (= :label
                                             ordinary-type) (assoc :variant :truncated))
      (get ordinary-options :DOVETAILED) (cond->
                                           (= :label
                                              ordinary-type) (assoc-in [:geometry :eccentricity] 0.4))
      (get ordinary-options :point/SINISTER) (cond->
                                               (= :gore
                                                  ordinary-type) (assoc-in [:orientation :point] :top-right)
                                               (= :point
                                                  ordinary-type) (assoc :variant :sinister)
                                               (= :quarter
                                                  ordinary-type) (assoc :variant :sinister-chief))
      (get ordinary-options :label-points) (cond->
                                             (= :label
                                                ordinary-type) (assoc :num-points
                                                                      (->> (get ordinary-options :label-points)
                                                                           (get-child #{:amount})
                                                                           ast->hdn
                                                                           (min max-label-points)))))))

(defmethod ast->hdn :cottise [[_ & nodes]]
  (let [field (ast->hdn (get-child #{:field} nodes))]
    (-> default/cottise
        (assoc :field field)
        (add-lines nodes)
        (add-fimbriation nodes :line-fimbriation? true))))

(defmethod ast->hdn :cottising [[_ & nodes]]
  (let [[cottise-1
         cottise-2] (->> nodes
                         (filter (type? #{:cottise}))
                         (map ast->hdn))
        double-node (get-child #{:DOUBLY} nodes)
        cottise-2 (or cottise-2
                      (when double-node
                        cottise-1))]
    (-> {:cottise-1 cottise-1}
        (cond->
          cottise-2 (assoc :cottise-2 cottise-2))
        (add-lines nodes))))

(defn- add-cottising [hdn nodes]
  (let [[main
         opposite
         extra] (->> nodes
                     (filter (type? #{:cottising}))
                     (map ast->hdn))
        ordinary-type (-> hdn :type name keyword)
        opposite (or opposite
                     (when (#{:fess
                              :pale
                              :bend
                              :bend-sinister
                              :chevron
                              :pall} ordinary-type)
                       main))
        extra (or extra
                  (when (#{:pall} ordinary-type)
                    main))]
    (cond-> hdn
      main (update :cottising merge main)
      opposite (update :cottising merge (set/rename-keys opposite
                                                         {:cottise-1 :cottise-opposite-1
                                                          :cottise-2 :cottise-opposite-2}))
      extra (update :cottising merge (set/rename-keys extra
                                                      {:cottise-1 :cottise-extra-1
                                                       :cottise-2 :cottise-extra-2})))))

(def ^:private ordinary-type-map
  (into {:ordinary/PALLET :heraldry.ordinary.type/pale
         :ordinary/BARRULET :heraldry.ordinary.type/fess
         :ordinary/CHEVRONNEL :heraldry.ordinary.type/chevron
         :ordinary/CANTON :heraldry.ordinary.type/quarter
         :ordinary/BENDLET :heraldry.ordinary.type/bend
         :ordinary/BENDLET-SINISTER :heraldry.ordinary.type/bend-sinister}
        (map (fn [[key _]]
               [(keyword "ordinary" (-> key name s/upper-case))
                key]))
        ordinary.options/ordinary-map))

(defn- get-ordinary-type [nodes]
  (let [node-type (->> nodes
                       (get-child ordinary-type-map)
                       first)]
    [node-type (get ordinary-type-map node-type)]))

(defmethod ast->hdn :ordinary [[_ & nodes]]
  (let [[node-type ordinary-type] (get-ordinary-type nodes)]
    (-> {:type ordinary-type
         :field (ast->hdn (get-child #{:field} nodes))}
        (cond->
          (#{:ordinary/PALLET}
           node-type) (assoc-in [:geometry :size] 15)
          (#{:ordinary/BARRULET
             :ordinary/CHEVRONNEL
             :ordinary/BENDLET
             :ordinary/BENDLET-SINISTER}
           node-type) (assoc-in [:geometry :size] 10)
          (#{:ordinary/CANTON}
           node-type) (assoc-in [:geometry :size] 80)
          (= :heraldry.ordinary.type/gore
             ordinary-type) (assoc :line {:type :enarched
                                          :flipped? true}))
        (add-ordinary-options nodes)
        (add-lines nodes)
        (add-cottising nodes)
        (add-fimbriation nodes :line-fimbriation? (not= node-type :ordinary/LABEL)))))

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

(defn- add-tincture-modifiers [hdn nodes]
  (let [modifiers (into {}
                        (comp (filter (type? #{:tincture-modifier}))
                              (map ast->hdn))
                        nodes)]
    (cond-> hdn
      (seq modifiers) (assoc :tincture modifiers))))

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

(defn find-tinctures [ast]
  (->> ast
       (tree-seq
        (fn [node]
          (and (vector? node)
               (-> node first keyword?)))
        rest)
       (keep (fn [ast]
               (let [kind (first ast)]
                 (if (= kind :SAME)
                   (second ast)
                   (get tincture-map kind)))))
       vec))

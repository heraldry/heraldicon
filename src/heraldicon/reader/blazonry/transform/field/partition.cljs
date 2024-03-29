(ns heraldicon.reader.blazonry.transform.field.partition
  (:require
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [heraldicon.heraldry.field.core :as field]
   [heraldicon.heraldry.field.options :as field.options]
   [heraldicon.reader.blazonry.result :as result]
   [heraldicon.reader.blazonry.transform.line :refer [add-lines]]
   [heraldicon.reader.blazonry.transform.shared :refer [ast->hdn get-child transform-first transform-all filter-nodes]]
   [heraldicon.util.number :as number]))

(def ^:private field-type-map
  (-> (into {}
            (map (fn [[key _]]
                   [(keyword "partition" (-> key name str/upper-case))
                    key]))
            field.options/field-map)
      (assoc :partition/FUSILY :heraldry.field.type/lozengy)))

(defn- get-field-type [nodes]
  (some->> nodes
           (get-child field-type-map)
           first
           (get field-type-map)))

(defn- add-partition-options [hdn nodes]
  (let [partition-options (some->> nodes
                                   (filter-nodes #{:ENHANCED
                                                   :DEHANCED
                                                   :REVERSED
                                                   :pattern-option/BENDY
                                                   :pattern-option/BENDY-SINISTER
                                                   :partition/FUSILY})
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
                                             partition-type) (assoc-in [:anchor :point] :top))

      (get partition-options :partition/FUSILY) (update :layout (fn [{:keys [num-fields-x
                                                                             num-fields-y]
                                                                      :as layout}]
                                                                  (let [num-fields-x (or num-fields-x 6)
                                                                        num-fields-y (or num-fields-y (int (/ num-fields-x 2)))]
                                                                    (assoc layout
                                                                           :num-fields-x num-fields-x
                                                                           :num-fields-y num-fields-y))))
      (get partition-options :pattern-option/BENDY) (assoc-in [:layout :rotation] 45)
      (get partition-options :pattern-option/BENDY-SINISTER) (assoc-in [:layout :rotation] -45))))

(def ^:private variant-map
  {:vairy-variant/COUNTER :counter
   :vairy-variant/IN-PALE :in-pale
   :vairy-variant/EN-POINT :en-point
   :vairy-variant/ANCIEN :ancien
   :potenty-variant/COUNTER :counter
   :potenty-variant/IN-PALE :in-pale
   :potenty-variant/EN-POINT :en-point})

(defn- add-variant [hdn nodes]
  (let [variant (some->> nodes
                         (filter-nodes variant-map)
                         ffirst
                         variant-map)]
    (cond-> hdn
      variant (assoc :variant variant))))

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
              first-new-reference (first new-references)
              result (loop [result result
                            [reference & rest] seen-references]
                       (if reference
                         (recur (assoc! result reference
                                        (conj (get result reference [])
                                              field))
                                rest)
                         result))
              result (if first-new-reference
                       (loop [result (assoc! result first-new-reference
                                             (conj (get result first-new-reference [])
                                                   field))
                              [reference & rest] (rest new-references)]
                         (if reference
                           (recur (assoc! result reference
                                          (conj (get result reference [])
                                                (if (< reference num-mandatory-fields)
                                                  field
                                                  first-new-reference)))
                                  rest)
                           result))
                       result)
              keyword-references (filter keyword? references)]
          (loop [result result
                 [reference & rest] keyword-references]
            (if reference
              (recur (assoc! result reference
                             (conj (get result reference [])
                                   field))
                     rest)
              result))))
      (transient {})
      fields))))

(defn- resolve-reference-chains [fields reference-map]
  (loop [fields (vec fields)
         [index & rest] (range (count fields))]
    (if index
      (if-let [reference (-> fields (get index) :index)]
        (let [real-reference (first (get reference-map reference))]
          (if (int? real-reference)
            (recur (assoc fields index {:type :heraldry.subfield.type/reference
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
                         {:type :heraldry.subfield.type/reference
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
                                                                 (str/join ", " (map reference-to-string
                                                                                     (sort-by field-reference-sort-key
                                                                                              missing-mandatory-references)))))
                   (seq unknown-references) (conj (str "Field"
                                                       (when (-> unknown-references
                                                                 count
                                                                 (> 1))
                                                         "s")
                                                       " not found in partition: "
                                                       (str/join ", " (map reference-to-string
                                                                           (sort-by field-reference-sort-key
                                                                                    unknown-references)))))
                   (seq multi-references) (conj (str "Field"
                                                     (when (-> multi-references
                                                               count
                                                               (> 1))
                                                       "s")
                                                     " for partition mentioned more than once: "
                                                     (str/join ", " (map reference-to-string
                                                                         (sort-by field-reference-sort-key
                                                                                  multi-references))))))]
    (cond-> hdn
      (seq warnings) (assoc ::result/warnings warnings))))

(defn- determine-num-base-fields [reference-map]
  (let [first-index-not-present (first (filter (comp not reference-map) (range)))]
    (when-not (zero? first-index-not-present)
      first-index-not-present)))

(defmethod ast->hdn :partition [[_ & nodes]]
  (let [field-type (get-field-type nodes)
        layout (transform-first #{:layout
                                  :horizontal-layout
                                  :vertical-layout} nodes)
        field-type (cond
                     (and (= field-type :heraldry.field.type/gyronny)
                          layout
                          (-> layout :num-fields-x (not= 8))) :heraldry.field.type/gyronny-n
                     (and (= field-type :heraldry.field.type/quartered)
                          layout
                          (or (-> layout :num-fields-x (not= 2))
                              (-> layout :num-fields-y (not= 2)))) :heraldry.field.type/quarterly
                     :else field-type)
        given-fields (->> (transform-all #{:partition-field} nodes)
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
        num-base-fields (or (determine-num-base-fields reference-map)
                            2)
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
                         num-base-fields
                         1))
        layout (some-> layout
                       (cond->
                         (not= num-base-fields 2) (assoc :num-base-fields num-base-fields)))
        fields (populate-field-references default-fields reference-map)]
    (-> {:type field-type
         :fields (mapv (fn [{:keys [type]
                             :as subfield}]
                         (if (isa? type :heraldry.subfield/type)
                           subfield
                           {:type :heraldry.subfield.type/field
                            :field subfield}))
                       fields)}
        (add-field-reference-warnings reference-map (count fields) num-mandatory-fields)
        (add-lines nodes)
        (cond->
          layout (assoc :layout layout))
        (add-partition-options nodes)
        (add-variant nodes))))

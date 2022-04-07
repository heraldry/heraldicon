(ns heraldry.reader.blazonry.transform
  (:require
   [clojure.set :as set]
   [clojure.string :as s]
   [clojure.walk :as walk]
   [heraldry.coat-of-arms.attributes :as attributes]
   [heraldry.coat-of-arms.charge.options :as charge.options]
   [heraldry.coat-of-arms.field.core :as field]
   [heraldry.coat-of-arms.field.options :as field.options]
   [heraldry.coat-of-arms.ordinary.options :as ordinary.options]
   [heraldry.coat-of-arms.tincture.core :as tincture]
   [heraldry.util :as util]
   [taoensso.timbre :as log]))

(defmulti ast->hdn first)

(defmethod ast->hdn :default [ast]
  (log/debug :ast->hdn-error ast)
  ast)

(defn type? [type-fn]
  #(-> % first type-fn))

(defn get-child [type-fn nodes]
  (->> nodes
       (filter (type? type-fn))
       first))

(def tincture-map
  (->> tincture/tincture-map
       (map (fn [[key _]]
              [(-> key
                   name
                   s/upper-case
                   keyword)
               key]))
       (into {:PROPER :void})))

(def roman-ordinal-strings
  {"i." 1
   "ii." 2
   "iii." 3
   "iv." 4
   "iiii." 4
   "v." 5
   "vi." 6
   "vii." 7
   "viii." 8
   "ix." 9
   "x." 10
   "xi." 11
   "xii." 12
   "xiii." 13
   "xiv." 14
   "xv." 15
   "xvi." 16
   "xvii." 17
   "xviii." 18
   "xix." 19
   "xx." 20})

(def roman-ordinal-strings-by-number
  (set/map-invert roman-ordinal-strings))

(def ordinal-strings
  (merge {"1st" 1
          "2nd" 2
          "3rd" 3
          "4th" 4
          "5th" 5
          "6th" 6
          "7th" 7
          "8th" 8
          "9th" 9
          "10th" 10
          "11th" 11
          "12th" 12
          "13th" 13
          "14th" 14
          "15th" 15
          "16th" 16
          "17th" 17
          "18th" 18
          "19th" 19
          "20th" 20

          "1." 1
          "2." 2
          "3." 3
          "4." 4
          "5." 5
          "6." 6
          "7." 7
          "8." 8
          "9." 9
          "10." 10
          "11." 11
          "12." 12
          "13." 13
          "14." 14
          "15." 15
          "16." 16
          "17." 17
          "18." 18
          "19." 19
          "20." 20

          "first" 1
          "second" 2
          "third" 3
          "fourth" 4
          "fifth" 5
          "sixth" 6
          "seventh" 7
          "eighth" 8
          "ninth" 9
          "tenth" 10
          "eleventh" 11
          "twelveth" 12
          "thirteenth" 13
          "fourteenth" 14
          "fifteenth" 15
          "sixteenth" 16
          "seventeenth" 17
          "eighteenth" 18
          "nineteenth" 19
          "twentieth" 20}
         roman-ordinal-strings))

(defmethod ast->hdn :ordinal [[_ & nodes]]
  (->> nodes
       (tree-seq (some-fn map? vector? seq?) seq)
       (filter string?)
       first
       (get ordinal-strings)))

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

(defn add-fimbriation [hdn nodes]
  (let [fimbriation (some-> (get-child #{:fimbriation} nodes)
                            ast->hdn)]
    (cond-> hdn
      fimbriation (assoc :fimbriation fimbriation))))

(defmethod ast->hdn :line [[_ & nodes]]
  (let [line-type (get-child #{:line-type} nodes)]
    (-> nil
        (cond->
          line-type (assoc :type (ast->hdn line-type)))
        (add-fimbriation nodes))))

(defn add-lines [hdn nodes]
  (let [[line
         opposite-line
         extra-line] (->> nodes
                          (filter (type? #{:line}))
                          (mapv ast->hdn))]
    (cond-> hdn
      line (assoc :line line)
      opposite-line (assoc :opposite-line opposite-line)
      extra-line (assoc :extra-line extra-line))))

(def max-layout-amount 50)

(defmethod ast->hdn :horizontal-layout [[_ & nodes]]
  (let [amount (-> (get-child #{:amount} nodes)
                   ast->hdn)]
    {:num-fields-x (min max-layout-amount amount)}))

(defmethod ast->hdn :vertical-layout-implicit [[_ & nodes]]
  (let [amount (-> (get-child #{:amount} nodes)
                   ast->hdn)]
    {:num-fields-y (min max-layout-amount amount)}))

(defmethod ast->hdn :vertical-layout-explicit [[_ & nodes]]
  (let [amount (-> (get-child #{:amount} nodes)
                   ast->hdn)]
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

(def field-type-map
  (->> field.options/fields
       (map (fn [key]
              [(-> key
                   name
                   s/upper-case
                   keyword)
               key]))
       (into {})))

(defn get-field-type [nodes]
  (some->> nodes
           (get-child field-type-map)
           first
           (get field-type-map)))

(defn add-partition-options [hdn nodes]
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
                                           partition-type) (assoc-in [:origin :offset-y] 12.5)
                                          (#{:per-bend
                                             :per-bend-sinister}
                                           partition-type) (->
                                                            (assoc-in [:origin :offset-y] 30)
                                                            (assoc-in [:anchor :offset-y] 30))
                                          (= :per-chevron
                                             partition-type) (->
                                                              (assoc-in [:origin :offset-y] 15)
                                                              (assoc-in [:anchor :point] :angle)
                                                              (assoc-in [:anchor :angle] 45)))
      (get partition-options :DEHANCED) (cond->
                                          (#{:per-fess
                                             :per-saltire
                                             :tierced-per-pall
                                             :tierced-per-fess
                                             :quartered
                                             :gyronny}
                                           partition-type) (assoc-in [:origin :offset-y] -12.5)
                                          (#{:per-bend
                                             :per-bend-sinister}
                                           partition-type) (->
                                                            (assoc-in [:origin :offset-y] -30)
                                                            (assoc-in [:anchor :offset-y] -30))
                                          (= :per-chevron
                                             partition-type) (->
                                                              (assoc-in [:origin :offset-y] -15)
                                                              (assoc-in [:anchor :point] :angle)
                                                              (assoc-in [:anchor :angle] 45)))
      (get partition-options :REVERSED) (cond->
                                          (= :per-chevron
                                             partition-type) (assoc-in [:direction-anchor :point] :chief)
                                          (= :tierced-per-pall
                                             partition-type) (assoc-in [:direction-anchor :point] :bottom)
                                          (= :per-pile
                                             partition-type) (assoc-in [:origin :point] :top)))))

(def field-locations
  {:POINT-DEXTER :dexter
   :POINT-SINISTER :sinister
   :POINT-CHIEF :chief
   :POINT-BASE :base
   :POINT-FESS :fess
   :POINT-DEXTER-CHIEF :chief-dexter
   :POINT-CHIEF-DEXTER :chief-dexter
   :POINT-SINISTER-CHIEF :chief-sinister
   :POINT-CHIEF-SINISTER :chief-sinister
   :POINT-DEXTER-BASE :base-dexter
   :POINT-BASE-DEXTER :base-dexter
   :POINT-SINISTER-BASE :base-sinister
   :POINT-BASE-SINISTER :base-sinister})

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

(def field-reference-map
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

(defn translate-field-reference [reference field-type]
  (if (keyword? reference)
    (get-in field-reference-map [(some-> field-type name keyword) reference] reference)
    (dec reference)))

(defmethod ast->hdn :partition-field [[_ & nodes]]
  (let [field (-> (get-child #{:field :plain} nodes)
                  ast->hdn)
        references (->> nodes
                        (filter (type? #{:field-reference}))
                        (map ast->hdn))]
    {:references references
     :field field}))

(defn field-reference-sort-key [reference]
  (if (int? reference)
    [0 reference]
    ;; keyword references
    [1 reference]))

(defn sanitize-referenced-fields [fields num-mandatory-fields]
  (let [fields (map-indexed
                (fn [index {:keys [references field] :as field-data}]
                  (-> (if (empty? references)
                        {:references [index]
                         :field field}
                        field-data)
                      (update :references #(->> % set (sort-by field-reference-sort-key))))) fields)]
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

(defn resolve-reference-chains [fields reference-map]
  (loop [fields (vec fields)
         [index & rest] (range (count fields))]
    (if index
      (if-let [reference (-> fields (get index) :index)]
        (let [real-reference (first (get reference-map reference))]
          (if (int? real-reference)
            (recur (util/vec-replace fields index {:type :heraldry.field.type/ref
                                                   :index real-reference})
                   rest)
            (recur fields rest)))
        (recur fields rest))
      fields)))

(defn populate-field-references [default-fields reference-map]
  (-> (loop [fields (vec default-fields)
             [index & rest] (range (count default-fields))]
        (if index
          (let [new-field (-> reference-map
                              (get index)
                              first)
                new-field (if (int? new-field)
                            {:type :heraldry.field.type/ref
                             :index new-field}
                            new-field)]
            (if new-field
              (recur
               (util/vec-replace fields index new-field)
               rest)
              ;; if the default field already is a reference, then we might have to
              (recur fields rest)))
          fields))
      (resolve-reference-chains reference-map)))

(defn reference-to-string [reference]
  (if (keyword? reference)
    (name reference)
    (let [field-number (inc reference)]
      (get roman-ordinal-strings-by-number field-number (str field-number)))))

(defn add-field-reference-warnings [hdn reference-map num-expected-field num-mandatory-fields]
  (let [unknown-references (->> reference-map
                                keys
                                (filter (fn [reference]
                                          (or (keyword? reference)
                                              (<= num-expected-field reference)))))
        multi-references (->> reference-map
                              (keep (fn [[k v]]
                                      (when (-> v count (> 1))
                                        k))))
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

(defmethod ast->hdn :NUMBER [[_ number-string]]
  (js/parseInt number-string))

(def number-strings
  {"one" 1
   "two" 2
   "double" 2
   "three" 3
   "triple" 3
   "four" 4
   "five" 5
   "six" 6
   "seven" 7
   "eight" 8
   "nine" 9
   "ten" 10
   "eleven" 11
   "twelve" 12
   "thirteen" 13
   "fourteen" 14
   "fifteen" 15
   "sixteen" 16
   "seventeen" 17
   "eighteen" 18
   "nineteen" 19
   "twenty" 20
   "thirty" 30
   "forty" 40
   "fifty" 50
   "sixty" 60
   "seventy" 70
   "eighty" 80
   "ninety" 90})

(defmethod ast->hdn :number-word [[_ & nodes]]
  (->> nodes
       (tree-seq (some-fn map? vector? seq?) seq)
       (filter string?)
       (map (fn [s]
              (get number-strings s 0)))
       (reduce +)))

(defmethod ast->hdn :components [[_ & nodes]]
  (let [components (->> nodes
                        (filter (type? #{:component}))
                        (mapcat ast->hdn)
                        vec)]
    (when (seq components)
      components)))

(defmethod ast->hdn :component [[_ node]]
  (ast->hdn node))

(def max-label-points 20)

(defn add-ordinary-options [hdn nodes]
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
                                                   :SINISTER
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
                                            ordinary-type) (assoc-in [:direction-anchor :point] :chief)
                                         (= :pall
                                            ordinary-type) (assoc-in [:direction-anchor :point] :bottom)
                                         (= :pile
                                            ordinary-type) (assoc-in [:origin :point] :bottom))
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
                                          ordinary-type) (assoc-in [:origin :offset-y] 12.5)
                                         (#{:bend
                                            :bend-sinister}
                                          ordinary-type) (->
                                                          (assoc-in [:origin :offset-y] 30)
                                                          (assoc-in [:anchor :offset-y] 30))
                                         (= :chief
                                            ordinary-type) (assoc-in [:geometry :size] (- 25 10))
                                         (= :base
                                            ordinary-type) (assoc-in [:geometry :size] (+ 25 10))
                                         (= :chevron
                                            ordinary-type) (->
                                                            (assoc-in [:origin :offset-y] 15)
                                                            (assoc-in [:anchor :point] :angle)
                                                            (assoc-in [:anchor :angle] 45))
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
                                          ordinary-type) (assoc-in [:origin :offset-y] -12.5)
                                         (#{:bend
                                            :bend-sinister}
                                          ordinary-type) (->
                                                          (assoc-in [:origin :offset-y] -30)
                                                          (assoc-in [:anchor :offset-y] -30))
                                         (= :chief
                                            ordinary-type) (assoc-in [:geometry :size] (+ 25 10))
                                         (= :base
                                            ordinary-type) (assoc-in [:geometry :size] (- 25 10))
                                         (= :chevron
                                            ordinary-type) (->
                                                            (assoc-in [:origin :offset-y] -15)
                                                            (assoc-in [:anchor :point] :angle)
                                                            (assoc-in [:anchor :angle] 45))
                                         (= :point
                                            ordinary-type) (assoc-in [:geometry :height] (+ 50 25)))
      (get ordinary-options :TRUNCATED) (cond->
                                          (= :label
                                             ordinary-type) (assoc :variant :truncated))
      (get ordinary-options :DOVETAILED) (cond->
                                           (= :label
                                              ordinary-type) (assoc-in [:geometry :eccentricity] 0.4))
      (get ordinary-options :SINISTER) (cond->
                                         (= :gore
                                            ordinary-type) (assoc-in [:anchor :point] :top-right)
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
  (let [field (-> (get-child #{:field} nodes)
                  ast->hdn)]
    (-> {:field field}
        (add-lines nodes))))

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

(defn add-cottising [hdn nodes]
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

(def ordinary-type-map
  (->> ordinary.options/ordinaries
       (map (fn [key]
              [(-> key
                   name
                   s/upper-case
                   keyword)
               key]))
       (into {:PALLET :heraldry.ordinary.type/pale
              :BARRULET :heraldry.ordinary.type/fess
              :CHEVRONNEL :heraldry.ordinary.type/chevron
              :BENDLET :heraldry.ordinary.type/bend
              :BENDLET-SINISTER :heraldry.ordinary.type/bend-sinister})))

(defn get-ordinary-type [nodes]
  (let [node-type (->> nodes
                       (get-child ordinary-type-map)
                       first)]
    [node-type (get ordinary-type-map node-type)]))

(defmethod ast->hdn :ordinary [[_ & nodes]]
  (let [[node-type ordinary-type] (get-ordinary-type nodes)]
    (-> {:type ordinary-type
         :field (ast->hdn (get-child #{:field} nodes))}
        (cond->
          (#{:PALLET}
           node-type) (assoc-in [:geometry :size] 15)
          (#{:BARRULET
             :CHEVRONNEL
             :BENDLET
             :BENDLET-SINISTER}
           node-type) (assoc-in [:geometry :size] 10)
          (= :heraldry.ordinary.type/gore
             ordinary-type) (assoc :line {:type :enarched
                                          :flipped? true}))
        (add-ordinary-options nodes)
        (add-lines nodes)
        (add-cottising nodes)
        (add-fimbriation nodes))))

(def max-ordinary-group-amount 20)

(defmethod ast->hdn :ordinary-group [[_ & nodes]]
  (let [amount-node (get-child #{:amount} nodes)
        amount (if amount-node
                 (ast->hdn amount-node)
                 1)
        ordinary (ast->hdn (get-child #{:ordinary} nodes))]
    (vec (repeat (-> amount
                     (max 1)
                     (min max-ordinary-group-amount)) ordinary))))

(def attitude-map
  (->> attributes/attitude-map
       (map (fn [[key _]]
              [(-> key
                   name
                   s/upper-case
                   keyword)
               key]))
       (into {})))

(defmethod ast->hdn :attitude [[_ & nodes]]
  (some->> nodes
           (get-child attitude-map)
           first
           (get attitude-map)))

(def facing-map
  (->> attributes/facing-map
       (map (fn [[key _]]
              [(-> key
                   name
                   s/upper-case
                   keyword)
               key]))
       (into {})))

(defmethod ast->hdn :facing [[_ & nodes]]
  (some->> nodes
           (get-child facing-map)
           first
           (get facing-map)))

(def max-star-points 100)

(defn add-charge-options [hdn nodes]
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

(def charge-type-map
  (->> charge.options/charges
       (map (fn [key]
              [(-> key
                   name
                   s/upper-case
                   keyword)
               key]))
       (into {:ESTOILE :heraldry.charge.type/star})))

(defmethod ast->hdn :charge-standard [[_ & nodes]]
  (let [charge-type-node-kind (first (get-child charge-type-map nodes))
        charge-type (get charge-type-map charge-type-node-kind)]
    (-> {:type charge-type}
        (add-charge-options nodes)
        (cond->
          (= charge-type-node-kind :ESTOILE) (assoc :wavy-rays? true)))))

(defmethod ast->hdn :charge-other-type [[_ custom-charge-type-node]]
  (let [normalized-keyword (-> custom-charge-type-node
                               first
                               name
                               (s/replace "custom-charge-type-" "")
                               keyword)]
    (keyword "heraldry.charge.type" normalized-keyword)))

(defmethod ast->hdn :charge-other [[_ & nodes]]
  (let [charge-type (-> (get-child #{:charge-other-type} nodes)
                        ast->hdn)]
    (-> {:type charge-type
         :variant {:id "charge:N87wec"
                   :version 0}}
        (add-charge-options nodes))))

(defmethod ast->hdn :charge [[_ & nodes]]
  (-> (get-child #{:charge-standard
                   :charge-other} nodes)
      ast->hdn
      (add-fimbriation nodes)))

(def max-charge-group-columns 20)

(def max-charge-group-rows 20)

(defn charge-group [charge amount nodes]
  (let [[arrangement-type
         & arrangement-nodes] (second (get-child #{:charge-arrangement} nodes))]
    (-> {:charges [charge]}
        (cond->
          (nil? arrangement-type) (merge
                                   {::default-charge-group-amount amount})
          (= :FESSWISE
             arrangement-type) (merge
                                {:type :heraldry.charge-group.type/rows
                                 :spacing (/ 95 amount)
                                 :strips [{:type :heraldry.component/charge-group-strip
                                           :slots (vec (repeat amount 0))}]})
          (= :PALEWISE
             arrangement-type) (merge
                                {:type :heraldry.charge-group.type/columns
                                 :spacing (/ 95 amount)
                                 :strips [{:type :heraldry.component/charge-group-strip
                                           :slots (vec (repeat amount 0))}]})
          (= :BENDWISE
             arrangement-type) (merge
                                {:type :heraldry.charge-group.type/rows
                                 :strip-angle 45
                                 :spacing (/ 120 amount)
                                 :strips [{:type :heraldry.component/charge-group-strip
                                           :slots (vec (repeat amount 0))}]})
          (= :BENDWISE-SINISTER
             arrangement-type) (merge
                                {:type :heraldry.charge-group.type/rows
                                 :strip-angle -45
                                 :spacing (/ 120 amount)
                                 :strips [{:type :heraldry.component/charge-group-strip
                                           :slots (vec (repeat amount 0))}]})
          (= :CHEVRONWISE
             arrangement-type) (merge
                                {:type :heraldry.charge-group.type/rows
                                 :spacing (/ 90 amount)
                                 :strips (->> (range (-> amount
                                                         inc
                                                         (/ 2)))
                                              (map (fn [index]
                                                     {:type :heraldry.component/charge-group-strip
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
          (= :IN-ORLE
             arrangement-type) (merge
                                {:type :heraldry.charge-group.type/in-orle
                                 :slots (vec (repeat amount 0))})
          (= :IN-ANNULLO
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
                                   :spacing (/ 95 (max width height))
                                   :strips (mapv (fn [amount]
                                                   {:type :heraldry.component/charge-group-strip
                                                    :slots (vec (repeat amount 0))})
                                                 amounts)}))))))

(def tincture-modifier-type-map
  (->> attributes/tincture-modifier-map
       (map (fn [[key _]]
              [(-> key
                   name
                   s/upper-case
                   keyword)
               key]))
       (into {})))

(defn get-tincture-modifier-type [nodes]
  (let [node-type (->> nodes
                       (get-child tincture-modifier-type-map)
                       first)]
    (get tincture-modifier-type-map node-type)))

(defmethod ast->hdn :tincture-modifier-type [[_ & nodes]]
  (get-tincture-modifier-type nodes))

(defmethod ast->hdn :tincture-modifier [[_ & nodes]]
  (let [modifier-type (-> (get-child #{:tincture-modifier-type} nodes)
                          ast->hdn)
        tincture (-> (get-child #{:tincture} nodes)
                     ast->hdn)]
    [modifier-type tincture]))

(defn add-tincture-modifiers [hdn nodes]
  (let [modifiers (->> nodes
                       (filter (type? #{:tincture-modifier}))
                       (map ast->hdn)
                       (into {}))]
    (cond-> hdn
      (seq modifiers) (assoc :tincture modifiers))))

(def max-charge-group-amount 64)

(defmethod ast->hdn :charge-group [[_ & nodes]]
  (let [amount-node (get-child #{:amount} nodes)
        amount (if amount-node
                 (ast->hdn amount-node)
                 1)
        amount (-> amount
                   (max 1)
                   (min max-charge-group-amount))
        field (ast->hdn (get-child #{:field} nodes))
        charge (-> #{:charge}
                   (get-child nodes)
                   ast->hdn
                   (assoc :field field)
                   (add-tincture-modifiers nodes))]
    (if (= amount 1)
      [charge]
      [(charge-group charge amount nodes)])))

(defn semy [charge nodes]
  (let [layout (some-> (get-child #{:layout
                                    :horizontal-layout
                                    :vertical-layout} nodes)
                       ast->hdn)]
    (cond-> {:type :heraldry.component/semy
             :charge charge}
      layout (assoc :layout layout))))

(defmethod ast->hdn :semy [[_ & nodes]]
  (let [field (ast->hdn (get-child #{:field} nodes))
        charge (-> #{:charge}
                   (get-child nodes)
                   ast->hdn
                   (assoc :field field)
                   (add-tincture-modifiers nodes))]
    [(semy charge nodes)]))

(defmethod ast->hdn :field [[_ & nodes]]
  (if-let [nested-field (get-child #{:field} nodes)]
    (ast->hdn nested-field)
    (let [field (ast->hdn (get-child #{:variation} nodes))
          component (some-> (get-child #{:component} nodes)
                            ast->hdn)
          components (some->> nodes
                              (get-child #{:components})
                              ast->hdn)
          components (vec (concat component
                                  components))]
      (cond-> field
        (seq components) (assoc :components components)))))

(defmethod ast->hdn :variation [[_ node]]
  (ast->hdn node))

(defmethod ast->hdn :blazon [[_ node]]
  (ast->hdn node))

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

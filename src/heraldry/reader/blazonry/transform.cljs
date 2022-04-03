(ns heraldry.reader.blazonry.transform
  (:require
   [clojure.set :as set]
   [clojure.string :as s]
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

(defmethod ast->hdn :tincture [[_ tincture]]
  (get tincture-map (first tincture) :void))

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

(defmethod ast->hdn :partition [[_ & nodes]]
  (let [field-type (get-field-type nodes)
        layout (some-> (get-child #{:layout
                                    :horizontal-layout
                                    :vertical-layout} nodes)
                       ast->hdn)
        ;; TODO: num-fields-x, num-fields-y, num-base-fields should be the defaults for the partition type
        default-fields (field/raw-default-fields
                        field-type
                        (-> layout :num-fields-x (or 6))
                        (-> layout :num-fields-y (or 6))
                        2)
        given-fields (->> nodes
                          (filter (type? #{:field :plain}))
                          (mapv ast->hdn))
        fields (loop [fields default-fields
                      [[index field] & rest] (map-indexed vector given-fields)]
                 (if index
                   (recur (util/vec-replace (vec fields) index field)
                          rest)
                   (vec fields)))]
    (-> {:type field-type
         :fields fields}
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
                                                 :star-points
                                                 :attitude
                                                 :facing}))
                                (map (fn [[key & nodes]]
                                       [key nodes]))
                                (into {}))
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
      (get charge-options :attitude) (assoc :attitude (ast->hdn (get charge-options :attitude)))
      (get charge-options :facing) (assoc :facing (ast->hdn (get charge-options :facing))))))

(def charge-type-map
  (->> charge.options/charges
       (map (fn [key]
              [(-> key
                   name
                   s/upper-case
                   keyword)
               key]))
       (into {})))

(defn get-standard-charge-type [nodes]
  (some->> nodes
           (get-child charge-type-map)
           first
           (get charge-type-map)))

(defmethod ast->hdn :charge-standard [[_ & nodes]]
  (-> {:type (get-standard-charge-type nodes)}
      (add-charge-options nodes)))

(defmethod ast->hdn :charge-other-type [[_ s]]
  (let [normalized-name (s/replace s #"[ -]+" "-")]
    (keyword "heraldry.charge.type" normalized-name)))

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

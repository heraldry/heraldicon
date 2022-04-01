(ns heraldry.blazonry.parser
  (:require
   [clojure.set :as set]
   [clojure.string :as s]
   [clojure.walk :as walk]
   [heraldry.coat-of-arms.charge.options :as charge.options]
   [heraldry.coat-of-arms.field.core :as field]
   [heraldry.coat-of-arms.field.options :as field.options]
   [heraldry.coat-of-arms.ordinary.options :as ordinary.options]
   [heraldry.util :as util]
   [instaparse.core :as insta]
   [taoensso.timbre :as log])
  (:require-macros [heraldry.blazonry.parser :refer [load-grammar]]))

(def grammar
  (load-grammar))

(def parser
  (insta/parser
   grammar
   :auto-whitespace :standard))

(defn rename-root-nodes [data]
  (if (and (keyword? data)
           (#{:root-field
              :root-variation
              :root-plain} data))
    (-> data
        name
        (subs (count "root-"))
        keyword)
    data))

(defn clean-ast [data]
  (->> data
       (walk/postwalk
        rename-root-nodes)))

(defn -parse-as-part [s]
  (let [s (s/lower-case s)]
    (loop [[[rule part-name] & rest] [[:layout-words "layout"]
                                      [:cottising-word "cottising"]
                                      [:tincture "tincture"]
                                      [:COUNTERCHANGED "tincture"]
                                      [:line-type "line"]
                                      [:FIMBRIATED "fimbriation"]
                                      [:partition-type "partition"]
                                      [:amount "number"]
                                      [:attitude "attitude"]
                                      [:facing "facing"]
                                      [:ordinary-type "ordinary"]
                                      [:ordinary-option "ordinary option"]
                                      [:charge-standard-type "charge"]
                                      [:charge-option "charge option"]]]
      (when rule
        (or (try
              (let [parsed (parser s :start rule)]
                (when (vector? parsed)
                  part-name))
              (catch :default _))
            (recur rest))))))

(def parse-as-part
  (memoize -parse-as-part))

(defn parse [s]
  (let [result (-> s
                   s/lower-case
                   parser
                   clean-ast)]
    (if (vector? result)
      result
      (let [r (:reason result)]
        (js/console.log :parse-error-at (subs s (:index result)))
        (js/console.log :error r)
        (log/debug :parse-error-at (subs s (:index result)))
        (log/debug :error r)
        (throw (ex-info "Parse error" {:reason (:reason result)
                                       :index (:index result)}))
        nil))))

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

(defmethod ast->hdn :tincture [[_ tincture]]
  (->> tincture
       first
       name
       s/lower-case
       keyword))

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

(defmethod ast->hdn :horizontal-layout [[_ & nodes]]
  (let [amount (-> (get-child #{:amount} nodes)
                   ast->hdn)]
    {:num-fields-x amount}))

(defmethod ast->hdn :vertical-layout-implicit [[_ & nodes]]
  (let [amount (-> (get-child #{:amount} nodes)
                   ast->hdn)]
    {:num-fields-y amount}))

(defmethod ast->hdn :vertical-layout-explicit [[_ & nodes]]
  (let [amount (-> (get-child #{:amount} nodes)
                   ast->hdn)]
    {:num-fields-y amount}))

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

(defmethod ast->hdn :ordinary-group [[_ & nodes]]
  (let [amount-node (get-child #{:amount} nodes)
        amount (if amount-node
                 (ast->hdn amount-node)
                 1)
        ordinary (ast->hdn (get-child #{:ordinary} nodes))]
    (vec (repeat (max 1 amount) ordinary))))

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
                                                                           ast->hdn))))))

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

(defmethod ast->hdn :ordinary-group [[_ & nodes]]
  (let [amount-node (get-child #{:amount} nodes)
        amount (if amount-node
                 (ast->hdn amount-node)
                 1)
        ordinary (ast->hdn (get-child #{:ordinary} nodes))]
    (vec (repeat (max 1 amount) ordinary))))

(defn add-charge-options [hdn nodes]
  (let [charge-options (some->> nodes
                                (filter (type? #{:MIRRORED
                                                 :REVERSED
                                                 :star-points}))
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
                                                                      ast->hdn))))))

(def charge-type-map
  (->> charge.options/charges
       (map (fn [key]
              [(-> key
                   name
                   s/upper-case
                   keyword)
               key]))
       (into {})))

(defn get-charge-type [nodes]
  (some->> nodes
           (get-child charge-type-map)
           first
           (get charge-type-map)))

(defmethod ast->hdn :charge-standard [[_ & nodes]]
  (-> {:type (get-charge-type nodes)}
      (add-charge-options nodes)))

(defmethod ast->hdn :charge [[_ & nodes]]
  (-> (get-child #{:charge-standard} nodes)
      ast->hdn
      (add-fimbriation nodes)))

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
                                                   (map ast->hdn))
                                      width (apply max amounts)
                                      height (count amounts)]
                                  {:type :heraldry.charge-group.type/rows
                                   :spacing (/ 95 (max width height))
                                   :strips (mapv (fn [amount]
                                                   {:type :heraldry.component/charge-group-strip
                                                    :slots (vec (repeat amount 0))})
                                                 amounts)}))))))

(defmethod ast->hdn :charge-group [[_ & nodes]]
  (let [amount-node (get-child #{:amount} nodes)
        amount (if amount-node
                 (ast->hdn amount-node)
                 1)
        amount (max 1 amount)
        field (ast->hdn (get-child #{:field} nodes))
        charge (-> #{:charge}
                   (get-child nodes)
                   ast->hdn
                   (assoc :field field))]
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

(defn add-charge-group-defaults [{::keys [default-charge-group-amount]
                                  :keys [type]
                                  :as hdn} & {:keys [parent-ordinary-type]}]
  (let [type-namespace (some-> type namespace)]
    (cond-> hdn
      default-charge-group-amount (->
                                   (dissoc ::default-charge-group-amount)
                                   (merge
                                    (case (some-> parent-ordinary-type name keyword)
                                      :pale {:type :heraldry.charge-group.type/columns
                                             :spacing (/ 95 default-charge-group-amount)
                                             :strips [{:type :heraldry.component/charge-group-strip
                                                       :slots (vec (repeat default-charge-group-amount 0))}]}

                                      :chevron {:type :heraldry.charge-group.type/rows
                                                :spacing (/ 90 default-charge-group-amount)
                                                :strips (->> (range (-> default-charge-group-amount
                                                                        inc
                                                                        (/ 2)))
                                                             (map (fn [index]
                                                                    {:type :heraldry.component/charge-group-strip
                                                                     :stretch (if (and (zero? index)
                                                                                       (even? default-charge-group-amount))
                                                                                1
                                                                                (if (and (pos? index)
                                                                                         (even? default-charge-group-amount))
                                                                                  (+ 1 (/ (inc index)
                                                                                          index))
                                                                                  2))
                                                                     :slots (if (zero? index)
                                                                              (if (odd? default-charge-group-amount)
                                                                                [0]
                                                                                [0 0])
                                                                              (-> (concat [0]
                                                                                          (repeat (dec index) nil)
                                                                                          [0])
                                                                                  vec))}))
                                                             vec)}

                                      :bordure {:type :heraldry.charge-group.type/in-orle
                                                :slots (vec (repeat default-charge-group-amount 0))}

                                      :orle {:type :heraldry.charge-group.type/in-orle
                                             :distance 2.5
                                             :slots (vec (repeat default-charge-group-amount 0))}

                                      {:type :heraldry.charge-group.type/rows
                                       :spacing (/ 95 default-charge-group-amount)
                                       :strips [{:type :heraldry.component/charge-group-strip
                                                 :slots (vec (repeat default-charge-group-amount 0))}]})))

      (= type-namespace
         "heraldry.ordinary.type") (update
                                    :field
                                    (fn [field]
                                      (cond-> field
                                        (:components field) (update
                                                             :components
                                                             (fn [components]
                                                               (mapv
                                                                (fn [component]
                                                                  (add-charge-group-defaults
                                                                   component
                                                                   :parent-ordinary-type type))
                                                                components)))))))))

(defn replace-adjusted-components [hdn indexed-components]
  (update
   hdn
   :components
   (fn [components]
     (vec
      (loop [components components
             [[index component] & rest] indexed-components]
        (if index
          (recur
           (util/vec-replace components index component)
           rest)
          components))))))

(defn arrange-ordinaries-in-one-dimension [hdn indexed-components & {:keys [offset-keyword
                                                                            spacing
                                                                            default-size]}]
  (let [num-elements (count indexed-components)
        size-without-spacing (->> indexed-components
                                  (map second)
                                  (map (fn [component]
                                         (-> component
                                             :geometry
                                             :size
                                             (or default-size))))
                                  (reduce +))
        total-size-with-margin (-> num-elements
                                   inc
                                   (* spacing)
                                   (+ size-without-spacing))
        stretch-factor (min 1
                            (/ 100 total-size-with-margin))
        total-size (-> num-elements
                       dec
                       (* spacing)
                       (+ size-without-spacing)
                       (* stretch-factor))
        adjusted-indexed-components (for [[index component] indexed-components]
                                      (let [size (-> component :geometry :size (or default-size))
                                            size-so-far (->> indexed-components
                                                             (take index)
                                                             (map second)
                                                             (map (fn [component]
                                                                    (-> component
                                                                        :geometry
                                                                        :size
                                                                        (or default-size))))
                                                             (reduce +))
                                            offset (-> size-so-far
                                                       (+ (* index spacing))
                                                       (+ (/ size 2))
                                                       (* stretch-factor)
                                                       (- (/ total-size 2)))]
                                        [index
                                         (-> component
                                             (assoc-in [:geometry :size] (* size stretch-factor))
                                             (assoc-in [:origin offset-keyword] offset))]))]
    (replace-adjusted-components hdn adjusted-indexed-components)))

(defn arrange-orles [hdn indexed-components]
  (let [indexed-components indexed-components
        default-size 3
        spacing 2
        initial-spacing 3
        adjusted-indexed-components (for [[real-index component] indexed-components]
                                      (let [index (min real-index 5)
                                            size (-> component :geometry :size (or default-size))
                                            size-so-far (->> indexed-components
                                                             (take index)
                                                             (map second)
                                                             (map (fn [component]
                                                                    (-> component
                                                                        :thickness
                                                                        (or default-size))))
                                                             (reduce +))
                                            offset (+ size-so-far
                                                      initial-spacing
                                                      (* index spacing))]
                                        [real-index
                                         (-> component
                                             (assoc :thickness (if (-> component
                                                                       :line
                                                                       (or :straight)
                                                                       (not= :straight))
                                                                 (/ size 2)
                                                                 size))
                                             (assoc :distance offset))]))]
    (replace-adjusted-components hdn adjusted-indexed-components)))

(defn arrange-ordinaries [hdn indexed-components]
  (let [ordinary-type (-> indexed-components
                          first
                          second
                          :type
                          name
                          keyword)]
    (case ordinary-type
      :pale (arrange-ordinaries-in-one-dimension hdn indexed-components
                                                 :offset-keyword :offset-x
                                                 :spacing 10
                                                 :default-size 20)
      :pile (arrange-ordinaries-in-one-dimension
             hdn
             (->> indexed-components
                  (map (fn [[index component]]
                         [index (-> component
                                    (assoc-in [:anchor :point] :angle)
                                    (assoc-in [:anchor :angle] 0))])))
             :offset-keyword :offset-x
             :spacing 0
             :default-size 33.333333)
      :fess (arrange-ordinaries-in-one-dimension hdn indexed-components
                                                 :offset-keyword :offset-y
                                                 :spacing 5
                                                 :default-size 15)
      :bend (arrange-ordinaries-in-one-dimension
             hdn
             (->> indexed-components
                  (map (fn [[index component]]
                         [index (-> component
                                    (assoc-in [:anchor :point] :angle)
                                    (assoc-in [:anchor :angle] 45))])))

             :offset-keyword :offset-y
             :spacing 15
             :default-size 15)
      :bend-sinister (arrange-ordinaries-in-one-dimension
                      hdn
                      (->> indexed-components
                           (map (fn [[index component]]
                                  [index (-> component
                                             (assoc-in [:anchor :point] :angle)
                                             (assoc-in [:anchor :angle] 45))])))

                      :offset-keyword :offset-y
                      :spacing 15
                      :default-size 15)
      :chevron (arrange-ordinaries-in-one-dimension
                hdn
                (->> indexed-components
                     (map (fn [[index component]]
                            [index (-> component
                                       (assoc-in [:anchor :point] :angle)
                                       (assoc-in [:anchor :angle] 45)
                                       (assoc-in [:direction-anchor :point] :angle)
                                       (assoc-in [:direction-anchor :angle] 0))])))
                :offset-keyword :offset-y
                :spacing 15
                :default-size 15)
      :orle (arrange-orles hdn indexed-components)
      hdn)))

(defn process-ordinary-groups [hdn]
  (if (some-> hdn :type namespace (= "heraldry.field.type"))
    (let [components-by-type (->> hdn
                                  :components
                                  (map-indexed vector)
                                  (group-by (comp :type second)))]
      (doall
       (loop [hdn hdn
              [[component-type indexed-components] & rest] components-by-type]
         (if (and component-type
                  (-> indexed-components
                      count
                      (> 1)))
           (recur (if (-> component-type namespace (= "heraldry.ordinary.type"))
                    (arrange-ordinaries hdn indexed-components)
                    hdn)
                  rest)
           hdn))))

    hdn))

(defn blazon->hdn [data]
  (let [hdn (some-> data
                    parse
                    ast->hdn)]
    (->> hdn
         (walk/prewalk add-charge-group-defaults)
         (walk/postwalk process-ordinary-groups))))

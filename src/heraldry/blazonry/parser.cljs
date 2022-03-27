(ns heraldry.blazonry.parser
  (:require
   [clojure.string :as s]
   [clojure.walk :as walk]
   [heraldry.coat-of-arms.field.core :as field]
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

(defn unify-partition-type-nodes [data]
  (if (and (keyword? data)
           (-> data name (s/starts-with? "partition-type")))
    :partition-type
    data))

(defn unify-ordinary-type-nodes [data]
  (if (and (keyword? data)
           (-> data name (s/starts-with? "ordinary-type")))
    :ordinary-type
    data))

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
        (comp unify-partition-type-nodes
              unify-ordinary-type-nodes
              rename-root-nodes))))

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

(defmethod ast->hdn :partition-type [[_ node]]
  (->> node
       first
       name
       s/lower-case
       (keyword "heraldry.field.type")))

(defmethod ast->hdn :line-type [[_ node]]
  (->> node
       first
       name
       s/lower-case
       keyword))

(defmethod ast->hdn :single-fimbriation [[_ & nodes]]
  (let [tincture-1 (->> nodes
                        (filter (type? #{:tincture}))
                        (take 1)
                        (mapv ast->hdn)
                        first)]
    {:mode :single
     :tincture-1 tincture-1}))

(defmethod ast->hdn :double-fimbriation [[_ & nodes]]
  (let [[tincture-1
         tincture-2] (->> nodes
                          (filter (type? #{:tincture}))
                          (take 2)
                          (mapv ast->hdn))]
    {:mode :double
     :tincture-1 tincture-1
     :tincture-2 tincture-2}))

(defmethod ast->hdn :fimbriation [[_ node]]
  (ast->hdn node))

(defmethod ast->hdn :line [[_ & nodes]]
  (let [line-type (get-child #{:line-type} nodes)
        fimbriation (get-child #{:fimbriation} nodes)]
    (cond-> nil
      line-type (assoc :type (ast->hdn line-type))
      fimbriation (assoc :fimbriation (ast->hdn fimbriation)))))

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

(defmethod ast->hdn :partition [[_ [_partition-group & nodes]]]
  (let [partition-type-node (get-child #{:partition-type} nodes)
        partition-type (ast->hdn partition-type-node)
        ;; TODO: num-fields-x, num-fields-y, num-base-fields should be the defaults for the partition type
        default-fields (field/raw-default-fields partition-type 6 6 2)
        given-fields (->> nodes
                          (filter (type? #{:field}))
                          (mapv ast->hdn))
        fields (loop [fields default-fields
                      [[index field] & rest] (map-indexed vector given-fields)]
                 (if index
                   (recur (util/vec-replace (vec fields) index field)
                          rest)
                   (vec fields)))]

    (-> {:type partition-type
         :fields fields}
        (add-lines nodes))))

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

(defmethod ast->hdn :ordinary-type [[_ node]]
  (->> node
       first
       name
       s/lower-case
       (keyword "heraldry.ordinary.type")))

(defmethod ast->hdn :ordinary [[_ [_ordinary-group & nodes]]]
  (let [ordinary-type-node (get-child #{:ordinary-type} nodes)]
    (-> {:type (ast->hdn ordinary-type-node)
         :field (ast->hdn (get-child #{:field} nodes))}
        (add-lines nodes))))

(defmethod ast->hdn :field [[_ & nodes]]
  (let [field (ast->hdn (get-child #{:variation} nodes))
        components (some->> nodes
                            (get-child #{:components})
                            ast->hdn)]
    (cond-> field
      (seq components) (assoc :components components))))

(defmethod ast->hdn :variation [[_ node]]
  (ast->hdn node))

(defmethod ast->hdn :blazon [[_ node]]
  (ast->hdn node))

(defn blazon->hdn [data]
  (some-> data
          parse
          ast->hdn))

(comment
  grammar

  (parse "per pale or and argent, a pale couped indented fimbriated with or, engrailed fimbriated argent and azure sable")

  (parse "per pale inden")

  (try
    (parse "per pale")
    (catch :default e
      (ex-data e)))

  ;;
  )

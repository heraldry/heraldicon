(ns heraldry.reader.blazonry.parser
  (:require
   [clojure.string :as s]
   [clojure.walk :as walk]
   [instaparse.core :as insta])
  (:require-macros
   [heraldry.reader.blazonry.parser :refer [load-grammar-template]]))

(def grammar-template
  (load-grammar-template))

(defn pluralize [charge-type]
  (if (s/ends-with? charge-type "s")
    (str charge-type "es")
    (str charge-type "s")))

(defn charge-type-rules [charge-type]
  [(str "#'\\b" charge-type "\\b'")
   (str "#'\\b" (s/replace charge-type " " "-") "\\b'")
   (str "#'\\b" (s/replace charge-type " " "' '") "\\b'")])

(declare default)

(defn -bad-charge-type? [charge-type]
  (try
    (let [parsed (insta/parse (:parser default) charge-type :start :bad-charge-type)]
      (vector? parsed))
    (catch :default _
      false)))

(def bad-charge-type?
  (memoize -bad-charge-type?))

(defn charge-type-rules-injection [charge-type-rules]
  (if (seq charge-type-rules)
    (let [charge-type-alternatives (str "| " (s/join "\n    | " (-> charge-type-rules
                                                                    keys
                                                                    sort)))
          charge-type-rule-definitions (s/join
                                        "\n"
                                        (map
                                         (fn [[rule terminals]]
                                           (str rule "\n"
                                                "    = "
                                                (s/join "\n    | " (sort terminals))))

                                         charge-type-rules))]
      (str charge-type-alternatives
           "\n"
           "\n"
           charge-type-rule-definitions))
    ""))

(defn generate [charges]
  (let [charge-map (->> charges
                        (group-by #(some-> % :type name keyword))
                        (map (fn [[key value]]
                               [key (sort-by (fn [charge]
                                               [(if (-> charge :username (= "heraldicon"))
                                                  0
                                                  1)
                                                (if (-> charge :is-public)
                                                  0
                                                  1)
                                                (case (:attitude charge)
                                                  :rampant [0 :_]
                                                  nil [1 :_]
                                                  [2 (:attitude charge)])
                                                (if (-> charge :facing (or :to-dexter))
                                                  [0 :_]
                                                  [1 (:facing charge)])
                                                (:id charge)]) value)]))
                        (into {}))
        charge-type-rules (->> charge-map
                               keys
                               (keep (fn [charge-type]
                                       (let [rule-name (->> charge-type
                                                            name
                                                            (str "custom-charge-type-"))
                                             clean-name (some-> charge-type
                                                                name
                                                                s/lower-case
                                                                (s/replace #"[^a-z0-9]+" " ")
                                                                s/trim)]
                                         (when (and (-> clean-name count pos?)
                                                    (not (bad-charge-type? clean-name)))
                                           [rule-name
                                            (set (concat (charge-type-rules clean-name)
                                                         (charge-type-rules (pluralize clean-name))))]))))
                               (into {}))
        grammar (s/replace
                 grammar-template
                 #"\{% charge-types %\}"
                 (charge-type-rules-injection charge-type-rules))]
    {:parser (insta/parser
              grammar
              :start :blazon
              :auto-whitespace :standard)
     :charge-map charge-map}))

(def default
  (generate []))

(def ast-node-normalization
  {:root-field :field
   :root-variation :variation
   :root-plain :plain
   :partition-field-plain :partition-field
   :ordinal-including-dot :ordinal})

(defn rename-root-node [ast]
  (if (keyword? ast)
    (get ast-node-normalization ast ast)
    ast))

(defn normalize-nodes [ast]
  (walk/postwalk rename-root-node ast))

(defn enumerate-same-tincture-references [ast]
  (let [counter (atom 0)]
    (walk/prewalk
     (fn [ast]
       (if (and (vector? ast)
                (-> ast first (= :SAME)))
         (let [new-value [:SAME {::tincture-same-id @counter}]]
           (swap! counter inc)
           new-value)
         ast))
     ast)))

(defn clean-ast [ast]
  (->> ast
       normalize-nodes
       enumerate-same-tincture-references))

(defn -parse-as-part [s {:keys [parser]}]
  (let [s (s/lower-case s)]
    (loop [[[rule part-name] & rest] [[:layout-words "layout"]
                                      [:cottising-word "cottising"]
                                      [:tincture "tincture"]
                                      [:COUNTERCHANGED "tincture"]
                                      [:partition-type "partition"]
                                      [:line-type "line"]
                                      [:FIMBRIATED "fimbriation"]
                                      [:amount "number"]
                                      [:attitude "attitude"]
                                      [:facing "facing"]
                                      [:tincture-modifier-type "extra tincture"]
                                      [:ordinary-type "ordinary"]
                                      [:ordinary-option "ordinary option"]
                                      [:charge-standard-type "charge"]
                                      [:charge-other-type "charge"]
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

(defn parse [s {:keys [parser]}]
  (let [result (-> s
                   s/lower-case
                   parser
                   clean-ast)]
    (if (vector? result)
      result
      (throw (ex-info "Parse error" {:reason (:reason result)
                                     :index (:index result)})))))

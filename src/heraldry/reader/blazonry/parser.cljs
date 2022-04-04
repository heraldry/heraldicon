(ns heraldry.reader.blazonry.parser
  (:require
   [clojure.string :as s]
   [clojure.walk :as walk]
   [instaparse.core :as insta])
  (:require-macros
   [heraldry.reader.blazonry.grammar :refer [load-grammar-template]]))

(def grammar-template
  (load-grammar-template))

(defn pluralize [charge-type]
  (if (s/ends-with? charge-type "s")
    (str charge-type "es")
    (str charge-type "s")))

(defn charge-type-rules [charge-type]
  [(str "#'\\b" charge-type "\\b'")
   (str "#'\\b" (s/replace charge-type " " "-") "\\b'")
   (str "#'\\b" (s/replace charge-type " " "' +'") "\\b'")])

(defn generate [charges]
  (let [charge-type-rules (->> charges
                               (map #(-> % :type name))
                               (mapcat (fn [charge-type]
                                         (let [clean-name (some-> charge-type
                                                                  s/lower-case
                                                                  (s/replace #"[^a-z0-9]+" " ")
                                                                  s/trim)]
                                           (when (-> clean-name count pos?)
                                             (concat (charge-type-rules clean-name)
                                                     (charge-type-rules (pluralize clean-name)))))))
                               set
                               sort)
        charge-type-rules-injection (if (seq charge-type-rules)
                                      (str "| " (s/join "| " charge-type-rules))
                                      "")
        grammar (s/replace
                 grammar-template
                 #"\{% charge-types %\}"
                 charge-type-rules-injection)
        charge-map (->> charges
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
                        (into {}))]
    {:parser (insta/parser
              grammar
              :start :blazon
              :auto-whitespace :standard)
     :charge-map charge-map}))

(def default
  (generate []))

(defn rename-root-nodes [ast]
  (if (and (keyword? ast)
           (#{:root-field
              :root-variation
              :root-plain} ast))
    (-> ast
        name
        (subs (count "root-"))
        keyword)
    ast))

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
       (walk/postwalk rename-root-nodes)
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

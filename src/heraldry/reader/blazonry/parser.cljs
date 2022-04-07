(ns heraldry.reader.blazonry.parser
  (:require
   [clojure.string :as s]
   [clojure.walk :as walk]
   [instaparse.core :as insta])
  (:require-macros
   [heraldry.reader.blazonry.parser :refer [default-parser]]))

(defn pluralize [charge-type]
  (if (s/ends-with? charge-type "s")
    (str charge-type "es")
    (str charge-type "s")))

(def default
  {:parser (default-parser)
   :charge-map {}})

(defn -bad-charge-type? [charge-type]
  (try
    (let [parsed (insta/parse (:parser default) charge-type :start :bad-charge-type)]
      (vector? parsed))
    (catch :default _
      false)))

(def bad-charge-type?
  (memoize -bad-charge-type?))

(defn make-rule [[rule terminals]]
  [rule
   {:tag :alt
    :parsers (mapcat
              (fn [terminal]
                (->> [[terminal]
                      [(s/replace terminal #" " "-")]
                      (s/split terminal #" ")]
                     set
                     (keep
                      (fn [terminal-words]
                        {:tag :cat
                         :parsers (into [{:tag :opt
                                          :parser {:tag :nt
                                                   :keyword :whitespace}
                                          :hide true}]
                                        (map (fn [word]
                                               {:tag :regexp
                                                :regexp (re-pattern (str "^\\b" word "\\b"))}))
                                        terminal-words)}))))
              terminals)
    :red {:reduction-type :hiccup
          :key rule}}])

(defn inject-charge-type-rules [parser charge-type-rules]
  (let [charge-type-rule-definitions (into {} (map make-rule charge-type-rules))
        charge-other-type-rule {:tag :alt
                                :parsers (map (fn [[rule _]]
                                                {:tag :nt
                                                 :keyword rule})
                                              charge-type-rules)
                                :red {:reduction-type :hiccup
                                      :key :charge-other-type}}]
    (update parser :grammar
            (fn [rules]
              (-> rules
                  (dissoc :custom-charge-type-lion)
                  (assoc :charge-other-type charge-other-type-rule)
                  (merge charge-type-rule-definitions))))))

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
                                                (cond
                                                  (-> charge :name s/lower-case (s/includes? "ww")) 0
                                                  (-> charge :name s/lower-case (s/includes? "sodacan")) 1
                                                  :else 2)
                                                (case (:attitude charge)
                                                  :none [0 :_]
                                                  :rampant [1 :_]
                                                  nil [2 :_]
                                                  [3 (:attitude charge)])
                                                (case (:facing charge)
                                                  :none [0 :_]
                                                  :to-dexter [1 :_]
                                                  nil [2 :_]
                                                  [3 (:facing charge)])
                                                (-> charge :attributes count)
                                                (:id charge)]) value)]))
                        (into {}))
        charge-type-rules (->> charge-map
                               keys
                               (keep (fn [charge-type]
                                       (let [rule-name (->> charge-type
                                                            name
                                                            (str "custom-charge-type-")
                                                            keyword)
                                             clean-name (some-> charge-type
                                                                name
                                                                s/lower-case
                                                                (s/replace #"[^a-z0-9]+" " ")
                                                                s/trim)
                                             valid-charge-type? (-> charge-type
                                                                    name
                                                                    (s/replace #"[^a-zA-Z0-9-]+" "")
                                                                    keyword
                                                                    (= charge-type))]
                                         (when (and valid-charge-type?
                                                    (-> clean-name count pos?)
                                                    (not (bad-charge-type? clean-name)))
                                           [rule-name
                                            (set [clean-name
                                                  (pluralize clean-name)])]))))
                               (into {}))
        default-parser (:parser default)
        new-parser (inject-charge-type-rules
                    default-parser
                    charge-type-rules)]
    {:parser new-parser
     :charge-map charge-map}))

(comment
  {:tag :alt
   :parsers ({:tag :cat
              :parsers ({:tag :opt
                         :parser {:tag :nt
                                  :keyword :whitespace}
                         :hide true}
                        {:tag :regexp
                         :regexp #"^\\bsejant[ -]erect\\b"})}
             {:tag :cat
              :parsers ({:tag :cat
                         :parsers ({:tag :opt
                                    :parser {:tag :nt
                                             :keyword :whitespace}
                                    :hide true}
                                   {:tag :regexp
                                    :regexp #"^\\bsejant\\b"})}
                        {:tag :cat
                         :parsers ({:tag :opt
                                    :parser {:tag :nt
                                             :keyword :whitespace}
                                    :hide true}
                                   {:tag :string
                                    :string "\\berect\\b"})})})
   :red {:reduction-type :hiccup
         :key :SEJANT-ERECT}}
  [:charge-other-type
   {:tag :nt
    :keyword :custom-charge-type-lion
    :red {:reduction-type :hiccup
          :key :charge-other-type}}]

  [:charge-other-type {:tag :alt
                       :parsers ({:tag :nt
                                  :keyword :custom-charge-type-lion}
                                 {:tag :nt
                                  :keyword :custom-charge-type-lion2})
                       :red {:reduction-type :hiccup
                             :key :charge-other-type}}]

  [:custom-charge-type-lion
   {:tag :alt
    :parsers [{:tag :cat
               :parsers [{:tag :opt
                          :parser {:tag :nt
                                   :keyword :whitespace}
                          :hide true}
                         {:tag :regexp
                          :regexp #"^\\blion\\b"}]}
              {:tag :cat
               :parsers [{:tag :opt
                          :parser {:tag :nt
                                   :keyword :whitespace}
                          :hide true}
                         {:tag :regexp
                          :regexp #"^\\blions\\b"}]}]
    :red {:reduction-type :hiccup
          :key :custom-charge-type-lion}}]

;;
  )

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

(ns heraldicon.reader.blazonry.parser
  (:require
   ["genex" :as genex]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [heraldicon.reader.blazonry.transform.tincture :as tincture]
   [instaparse.core :as insta])
  (:require-macros
   [heraldicon.reader.blazonry.parser :refer [default-parser]]))

(defn- pluralize [charge-type]
  (if (str/ends-with? charge-type "s")
    (str charge-type "es")
    (str charge-type "s")))

(defn- generate-strings-for-rule [parser rule]
  (let [result (insta/parse parser "±" :start rule)]
    (into #{}
          (comp (mapcat (fn [{:keys [tag expecting]}]
                          (case tag
                            :optional []
                            :string [expecting]
                            :regexp (-> expecting
                                        genex
                                        .generate
                                        js->clj))))
                (map str/trim))
          (:reason result))))

(defn- suggestion-classifications [parser]
  (->> [[:layout-words "layout"]
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
        [:charge-option "charge option"]]
       (map (fn [[rule class]]
              (into {}
                    (map (fn [s]
                           [s class]))
                    (generate-strings-for-rule parser rule))))
       (apply merge-with (fn [a _] a))))

(def default
  (let [parser (default-parser)]
    {:parser parser
     :bad-charge-types (generate-strings-for-rule parser :bad-charge-type)
     :suggestion-classifications (suggestion-classifications parser)
     :charge-map {}}))

(defn- make-rule [[rule terminals]]
  [rule
   {:tag :alt
    :parsers (mapcat
              (fn [terminal]
                (->> [[terminal]
                      [(str/replace terminal #" " "-")]
                      (str/split terminal #" ")]
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

(defn- inject-charge-type-rules [parser charge-type-rules]
  (let [charge-type-rule-definitions (into {}
                                           (map make-rule)
                                           charge-type-rules)
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
                  (dissoc :charge-other-type/lion)
                  (assoc :charge-other-type charge-other-type-rule)
                  (merge charge-type-rule-definitions))))))

(defn generate [charges]
  (let [charge-map (into {}
                         (map (fn [[key value]]
                                [key (sort-by (fn [charge]
                                                [(if (-> charge :username (= "heraldicon"))
                                                   0
                                                   1)
                                                 (if (-> charge :access (= :public))
                                                   0
                                                   1)
                                                 (cond
                                                   (-> charge :name str/lower-case (str/includes? "ww")) 0
                                                   (-> charge :name str/lower-case (str/includes? "sodacan")) 1
                                                   :else 2)
                                                 (case (-> charge :data :attitude)
                                                   :none [0 :_]
                                                   :rampant [1 :_]
                                                   nil [2 :_]
                                                   [3 (-> charge :data :attitude)])
                                                 (case (-> charge :data :facing)
                                                   :none [0 :_]
                                                   :to-dexter [1 :_]
                                                   nil [2 :_]
                                                   [3 (-> charge :data :facing)])
                                                 (-> charge :data :attributes count)
                                                 (:id charge)]) value)]))
                         (group-by #(some-> % :data :charge-type name keyword)
                                   charges))
        charge-type-rules (into {}
                                (keep (fn [charge-type]
                                        (let [charge-type-name (name charge-type)
                                              rule-name (keyword "charge-other-type" charge-type-name)
                                              clean-name (some-> charge-type-name
                                                                 str/lower-case
                                                                 (str/replace #"[^a-z0-9]+" " ")
                                                                 str/trim)
                                              valid-charge-type? (re-matches #"^[a-zA-Z0-9-]+$" charge-type-name)]
                                          (when (and valid-charge-type?
                                                     (not (str/blank? clean-name)))
                                            (if (contains? (:bad-charge-types default) clean-name)
                                              [rule-name (let [clean-name (str "charge " clean-name)]
                                                           (set [clean-name
                                                                 (pluralize clean-name)]))]
                                              [rule-name (set [clean-name
                                                               (pluralize clean-name)])])))))
                                (keys charge-map))
        default-parser (:parser default)
        new-parser (inject-charge-type-rules
                    default-parser
                    charge-type-rules)]
    {:parser new-parser
     :suggestion-classifications (suggestion-classifications new-parser)
     :charge-map charge-map}))

(def ^:private ast-node-normalization
  {:root-field :field
   :root-variation :variation
   :root-plain :plain
   :partition-field-plain :partition-field
   :ordinal-including-dot :ordinal})

(defn- rename-root-node [ast]
  (if (keyword? ast)
    (get ast-node-normalization ast ast)
    ast))

(defn- normalize-nodes [ast]
  (walk/postwalk rename-root-node ast))

(defn- enumerate-same-tincture-references [ast]
  (let [counter (atom 0)]
    (walk/prewalk
     (fn [ast]
       (if (and (vector? ast)
                (-> ast first (= :SAME)))
         (let [new-value [:SAME {::tincture/tincture-same-id @counter}]]
           (swap! counter inc)
           new-value)
         ast))
     ast)))

(defn- clean-ast [ast]
  (->> ast
       normalize-nodes
       enumerate-same-tincture-references))

(defn parse [s {:keys [parser]}]
  (let [result (-> s
                   str/lower-case
                   parser
                   clean-ast)]
    (if (vector? result)
      result
      (throw (ex-info "Parse error" {:reason (:reason result)
                                     :index (:index result)})))))

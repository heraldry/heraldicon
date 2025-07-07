(ns heraldicon.frontend.validation
  (:require
   [clojure.string :as str]
   [heraldicon.context :as c]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.tooltip :as tooltip]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]))

(defn- field-tinctures-for-validation [context]
  (let [field-type (interface/get-sanitized-data (c/++ context :type))
        tincture (interface/get-sanitized-data (c/++ context :tincture))
        subfield-1-type (interface/get-sanitized-data (c/++ context :fields 0 :field :type))
        subfield-1-tincture (interface/get-sanitized-data (c/++ context :fields 0 :field :tincture))
        subfield-2-type (interface/get-sanitized-data (c/++ context :fields 1 :field :type))
        subfield-2-tincture (interface/get-sanitized-data (c/++ context :fields 1 :field :tincture))
        field-type (some-> field-type name keyword)
        subfield-1-type (some-> subfield-1-type name keyword)
        subfield-2-type (some-> subfield-2-type name keyword)]
    (when field-type
      (cond-> #{}
        (= field-type :plain) (conj tincture)
        (and (not= field-type :plain)
             (= subfield-1-type :plain)
             subfield-1-tincture) (conj subfield-1-tincture)
        (and (not= field-type :plain)
             (= subfield-2-type :plain)
             subfield-2-tincture) (conj subfield-2-tincture)
        ;; at least one of the subfields is not a plain field, but we'll stop here, only
        ;; report that there's more with the magical :mixed tincture
        (and (not= field-type :plain)
             (or (not= subfield-1-type :plain)
                 (not= subfield-2-type :plain))) (conj :mixed)))))

(defn- list-tinctures [tinctures]
  (->> tinctures
       sort
       (map tincture/translate-tincture)
       (string/combine
        ", ")))

(defn- verify-rule-of-tincture [parent-tinctures own-tinctures fimbriated?]
  (let [parent-kinds (into #{}
                           (map tincture/kind)
                           parent-tinctures)
        own-kinds (into #{}
                        (map tincture/kind)
                        own-tinctures)]
    (cond
      (= #{:metal}
         parent-kinds
         own-kinds) {:level :warning
                     :message (string/format-tr (tr
                                                 (if fimbriated?
                                                   :string.validation.rule-of-tincture/metal-fimbriation-on-metal-field
                                                   :string.validation.rule-of-tincture/metal-field-on-metal-field))
                                                (list-tinctures own-tinctures)
                                                (list-tinctures parent-tinctures))}
      (= #{:colour}
         parent-kinds
         own-kinds) {:level :warning
                     :message (string/format-tr (tr
                                                 (if fimbriated?
                                                   :string.validation.rule-of-tincture/colour-fimbriation-on-colour-field
                                                   :string.validation.rule-of-tincture/colour-field-on-colour-field))
                                                (list-tinctures own-tinctures)
                                                (list-tinctures parent-tinctures))})))

(defn- validate-tinctures [field-context parent-field-context fimbriation-context]
  (let [field-tinctures (field-tinctures-for-validation field-context)
        parent-field-tinctures (field-tinctures-for-validation parent-field-context)
        fimbriation-tincture-1 (interface/get-sanitized-data (c/++ fimbriation-context :tincture-1))
        fimbriation-tincture-2 (interface/get-sanitized-data (c/++ fimbriation-context :tincture-2))
        fimbriation-options (interface/get-options fimbriation-context)]
    (when (or fimbriation-options
              (= (-> field-context :path drop-last)
                 (-> fimbriation-context :path drop-last)))
      (let [which (-> fimbriation-context
                      :path
                      drop-last
                      last)
            tinctures-touching-parent (or (when fimbriation-tincture-2
                                            #{fimbriation-tincture-2})
                                          (when fimbriation-tincture-1
                                            #{fimbriation-tincture-1})
                                          field-tinctures)
            fimbriated? (or fimbriation-tincture-2 fimbriation-tincture-1)
            main-check (verify-rule-of-tincture parent-field-tinctures
                                                tinctures-touching-parent
                                                fimbriated?)
            fimbriation-tincture-1-kind (some-> fimbriation-tincture-1
                                                tincture/kind)
            fimbriation-tincture-2-kind (some-> fimbriation-tincture-2
                                                tincture/kind)
            field-tincture-kinds (into #{}
                                       (map tincture/kind)
                                       field-tinctures)]
        [{:fimbriated? fimbriation-tincture-1}
         (cond-> []
           main-check (conj (update main-check
                                    :message (fn [message]
                                               (string/str-tr
                                                (case which
                                                  :line (string/str-tr :string.entity/main-line ": ")
                                                  :opposite-line (string/str-tr :string.entity/opposite-line ": ")
                                                  :extra-line (string/str-tr :string.entity/extra-line ": ")
                                                  nil)
                                                message))))

           (= fimbriation-tincture-1-kind
              fimbriation-tincture-2-kind
              :metal) (conj {:level :note
                             :message (string/format-tr (tr
                                                         :string.validation.rule-of-tincture/fimbriation-tinctures-both-metal)
                                                        (tincture/translate-tincture fimbriation-tincture-1)
                                                        (tincture/translate-tincture fimbriation-tincture-2))})

           (= fimbriation-tincture-1-kind
              fimbriation-tincture-2-kind
              :colour) (conj {:level :note
                              :message (string/format-tr (tr
                                                          :string.validation.rule-of-tincture/fimbriation-tinctures-both-colour)
                                                         (tincture/translate-tincture fimbriation-tincture-1)
                                                         (tincture/translate-tincture fimbriation-tincture-2))})

           (= (set [fimbriation-tincture-1-kind])
              field-tincture-kinds
              #{:metal}) (conj {:level :note
                                :message (string/format-tr (tr
                                                            :string.validation.rule-of-tincture/metal-fimbriation-touches-metal-field)
                                                           (tincture/translate-tincture fimbriation-tincture-1)
                                                           (list-tinctures field-tinctures))})

           (= (set [fimbriation-tincture-1-kind])
              field-tincture-kinds
              #{:colour}) (conj {:level :note
                                 :message (string/format-tr (tr
                                                             :string.validation.rule-of-tincture/colour-fimbriation-touches-colour-field)
                                                            (tincture/translate-tincture fimbriation-tincture-1)
                                                            (list-tinctures field-tinctures))}))]))))

(def ^:private level-order
  {:error 0
   :warning 1
   :note 2})

(def ^:private which-order
  {:line 0
   :opposite-line 1
   :extra-line 2})

(defn- sort-validations [validations]
  (sort-by (juxt (comp level-order :level)
                 (comp which-order :which))
           validations))

(defn validate-ordinary [context]
  (let [field-context (c/++ context :field)
        parent-field-context (c/-- context 2)
        main-validation (validate-tinctures field-context parent-field-context (c/++ context :fimbriation))
        other-validations [(validate-tinctures field-context parent-field-context (c/++ context :line :fimbriation))
                           (validate-tinctures field-context parent-field-context (c/++ context :opposite-line :fimbriation))
                           (validate-tinctures field-context parent-field-context (c/++ context :extra-line :fimbriation))]
        fimbriated? (some (comp :fimbriated? first) other-validations)
        ;; if any of the line validations had fimbriations, then use all of them, because
        ;; each need to be validated on their own then, the main one doesn't matter anymore;
        ;; if on other other hand none of the line ones had fimbriation, then only the main
        ;; check is relevant
        validations (map second (if fimbriated?
                                  other-validations
                                  [main-validation]))]
    (->> validations
         (apply concat)
         (filter identity)
         sort-validations)))

;; TODO: probably a bug, this derefs subscriptions inside the subscription building function
(defn validate-charge [context]
  (let [field-context (c/++ context :field)
        parent-semy-context (c/-- context)
        parent-charge-group-context (c/-- context 2)
        parent-type (or
                     (interface/get-raw-data (c/++ parent-semy-context :type))
                     (interface/get-raw-data (c/++ parent-charge-group-context :type)))
        parent-field-context (case parent-type
                               :heraldry/charge-group (c/-- parent-charge-group-context 2)
                               :heraldry/semy (c/-- parent-semy-context 2)
                               (c/-- context 2))
        main-validation (validate-tinctures field-context parent-field-context (c/++ context :fimbriation))]
    (sort-validations (second main-validation))))

(defn validate-cottise [context]
  (let [field-context (c/++ context :field)
        parent-field-context (interface/parent (interface/parent context))
        main-validation (validate-tinctures field-context parent-field-context (c/++ context :fimbriation))
        other-validations [(validate-tinctures field-context parent-field-context (c/++ context :line :fimbriation))
                           (validate-tinctures field-context parent-field-context (c/++ context :opposite-line :fimbriation))]
        fimbriated? (some (comp :fimbriated? first) other-validations)
        ;; if any of the line validations had fimbriations, then use all of them, because
        ;; each need to be validated on their own then, the main one doesn't matter anymore;
        ;; if on other other hand none of the line ones had fimbriation, then only the main
        ;; check is relevant
        validations (map second (if fimbriated?
                                  other-validations
                                  [main-validation]))]
    (->> validations
         (apply concat)
         (filter identity)
         sort-validations)))

(defn validate-field [context]
  (let [field-type (-> (interface/get-raw-data (c/++ context :type)) name keyword)
        num-fields-x (interface/get-sanitized-data (c/++ context :layout :num-fields-x))
        num-fields-y (interface/get-sanitized-data (c/++ context :layout :num-fields-y))]
    (cond-> []
      (and (= field-type :paly)
           (or (not (integer? num-fields-x))
               (odd? num-fields-x))) (conj {:level :warning
                                            :message :string.validation.partition/paly-should-have-even-number-of-fields})
      (and (= field-type :barry)
           (or (not (integer? num-fields-y))
               (odd? num-fields-y))) (conj {:level :warning
                                            :message :string.validation.partition/barry-should-have-even-number-of-fields})
      (and (#{:bendy :bendy-sinister} field-type)
           (or (not (integer? num-fields-y))
               (odd? num-fields-y))) (conj {:level :warning
                                            :message :string.validation.partition/bendy-should-have-even-number-of-fields}))))

(defn- validate-attribution [context public?]
  (let [nature (interface/get-sanitized-data (c/++ context :nature))
        source-fields [(interface/get-sanitized-data (c/++ context :source-license))
                       (interface/get-sanitized-data (c/++ context :source-name))
                       (interface/get-sanitized-data (c/++ context :source-link))
                       (interface/get-sanitized-data (c/++ context :source-creator-name))
                       (interface/get-sanitized-data (c/++ context :source-creator-link))]]
    (when (and public?
               (= nature :derivative)
               (seq (filter (fn [value]
                              (if (keyword? value)
                                (= value :none)
                                (str/blank? value))) source-fields)))
      [{:level :error
        :message :string.validation.attribution/all-source-fields-required}])))

(defn- validate-access [context]
  (let [public? (= (interface/get-sanitized-data (c/++ context :access))
                   :public)
        license (interface/get-sanitized-data (c/++ context :attribution :license))]
    (when (and public? (= license :none))
      [{:level :error
        :message :string.validation.attribution/license-required-for-public-objects}])))

(defn validate-entity [context]
  (let [access (validate-access context)
        public? (= (interface/get-sanitized-data (c/++ context :access))
                   :public)
        attribution (validate-attribution (c/++ context :attribution) public?)]
    (concat
     access
     attribution)))

(defn- validation-color [level]
  (case level
    :note "#ffd24d"
    :warning "#ffb366"
    :error "#b30000"
    "#ccc"))

(defn- render-icon [level]
  [:i.fas.fa-exclamation-triangle {:style {:color (validation-color level)}}])

(defn render [validation]
  (if (seq validation)
    (let [first-message (first validation)]
      [tooltip/info
       (into [:ul {:style {:position "relative"
                           :padding-left "1.8em"}}]
             (map-indexed (fn [idx {:keys [level message]}]
                            ^{:key idx}
                            [:li [:div {:style {:position "absolute"
                                                :left "0em"}}
                                  [render-icon level]]
                             [tr message]]))
             validation)
       :element [render-icon (:level first-message)]
       :width "25em"])

    [:<>]))

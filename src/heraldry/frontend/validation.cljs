(ns heraldry.frontend.validation
  (:require
   [clojure.string :as s]
   [heraldry.coat-of-arms.tincture.core :as tincture]
   [heraldry.component :as component]
   [heraldry.context :as c]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.gettext :refer [string]]
   [heraldry.util :as util]
   [re-frame.core :as rf]))

(def level-order
  {:error 0
   :warning 1
   :note 2})

(def which-order
  {:line 0
   :opposite-line 1
   :extra-line 2})

(defn validation-color [level]
  (case level
    :note "#ffd24d"
    :warning "#ffb366"
    :error "#b30000"
    "#ccc"))

(rf/reg-sub :field-tinctures-for-validation
  (fn [[_ context] _]
    [(rf/subscribe [:heraldry.state/sanitized-data (c/++ context :type)])
     (rf/subscribe [:heraldry.state/sanitized-data (c/++ context :tincture)])
     (rf/subscribe [:heraldry.state/sanitized-data (c/++ context :fields 0 :type)])
     (rf/subscribe [:heraldry.state/sanitized-data (c/++ context :fields 0 :tincture)])
     (rf/subscribe [:heraldry.state/sanitized-data (c/++ context :fields 1 :type)])
     (rf/subscribe [:heraldry.state/sanitized-data (c/++ context :fields 1 :tincture)])])

  (fn [[field-type
        tincture
        subfield-1-type
        subfield-1-tincture
        subfield-2-type
        subfield-2-tincture] [_ _context]]
    (let [field-type (some-> field-type name keyword)
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
                   (not= subfield-2-type :plain))) (conj :mixed))))))

(rf/reg-sub :fimbriation-tinctures-for-validation
  (fn [[_ context] _]
    [(rf/subscribe [:heraldry.state/sanitized-data (c/++ context :tincture-1)])
     (rf/subscribe [:heraldry.state/sanitized-data (c/++ context :tincture-2)])])

  (fn [[tincture-1 tincture-2] [_ _context]]
    [tincture-1 tincture-2]))

(defn list-tinctures [tinctures]
  (->> tinctures
       sort
       (map tincture/translate-tincture)
       (util/combine
        ", ")))

(defn verify-rule-of-tincture [parent-tinctures own-tinctures fimbriated?]
  (let [parent-kinds (->> parent-tinctures
                          (map tincture/kind)
                          (into #{}))
        own-kinds (->> own-tinctures
                       (map tincture/kind)
                       (into #{}))]
    (cond
      (= #{:metal}
         parent-kinds
         own-kinds) {:level :warning
                     :message (util/str-tr (string "Metal")
                                           " "
                                           (if fimbriated?
                                             (string "Fimbriation")
                                             (string "field")) " (" (list-tinctures own-tinctures)
                                           ") " (string "on") " "
                                           (string "Metal") " " (string "field") " (" (list-tinctures parent-tinctures)
                                           ") " (string "breaks rule of tincture."))}
      (= #{:colour}
         parent-kinds
         own-kinds) {:level :warning
                     :message (util/str-tr (string "Colour")
                                           " " (if fimbriated?
                                                 (string "Fimbriation")
                                                 (string "field")) " (" (list-tinctures own-tinctures)
                                           ") " (string "on") " "
                                           (string "Colour") " " (string "field") " (" (list-tinctures parent-tinctures)
                                           ") " (string "breaks rule of tincture."))})))

(rf/reg-sub :validate-tinctures
  (fn [[_ field-context parent-field-context fimbriation-context] _]
    [(rf/subscribe [:field-tinctures-for-validation field-context])
     (rf/subscribe [:field-tinctures-for-validation parent-field-context])
     (rf/subscribe [:fimbriation-tinctures-for-validation fimbriation-context])
     (rf/subscribe [:heraldry.state/options fimbriation-context])])

  (fn [[field-tinctures
        parent-field-tinctures
        [fimbriation-tincture-1
         fimbriation-tincture-2]
        fimbriation-options] [_ field-context _parent-field-context fimbriation-context]]
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
            field-tincture-kinds (->> field-tinctures
                                      (map tincture/kind)
                                      (into #{}))]
        [{:fimbriated? fimbriation-tincture-1}
         (cond-> []
           main-check (conj (-> main-check
                                (update :message (fn [message]
                                                   (util/str-tr (case which
                                                                  :line (string "Main line")
                                                                  :opposite-line (string "Opposite line")
                                                                  :extra-line (string "Extra line")
                                                                  nil)
                                                                ": "
                                                                message)))))

           (= fimbriation-tincture-1-kind
              fimbriation-tincture-2-kind
              :metal) (conj {:level :note
                             :message (util/str-tr
                                       (string "Fimbriation tinctures are both metals (")
                                       (tincture/translate-tincture fimbriation-tincture-1)
                                       " " (string "and") " "
                                       (tincture/translate-tincture fimbriation-tincture-2)
                                       ").")})

           (= fimbriation-tincture-1-kind
              fimbriation-tincture-2-kind
              :colour) (conj {:level :note
                              :message (util/str-tr
                                        (string "Fimbriation tinctures are both colours (")
                                        (tincture/translate-tincture fimbriation-tincture-1)
                                        " " (string "and") " "
                                        (tincture/translate-tincture fimbriation-tincture-2)
                                        ").")})

           (= (set [fimbriation-tincture-1-kind])
              field-tincture-kinds
              #{:metal}) (conj {:level :note
                                :message (util/str-tr
                                          (string "Fimbriation metal tincture (")
                                          (tincture/translate-tincture fimbriation-tincture-1)
                                          (string ") touches metal field (")
                                          (list-tinctures field-tinctures)
                                          ").")})

           (= (set [fimbriation-tincture-1-kind])
              field-tincture-kinds
              #{:colour}) (conj {:level :note
                                 :message (util/str-tr
                                           (string "Fimbriation colour tincture (")
                                           (tincture/translate-tincture fimbriation-tincture-1)
                                           (string ") touches colour field (")
                                           (list-tinctures field-tinctures)
                                           ").")}))]))))

(defn sort-validations [validations]
  (sort-by (fn [validation]
             [(-> validation :level level-order)
              (-> validation :which which-order)])
           validations))

(rf/reg-sub :validate-ordinary
  (fn [[_ context] _]
    (let [field-context (c/++ context :field)
          parent-field-context (c/-- context 2)]
      [(rf/subscribe [:validate-tinctures field-context parent-field-context (c/++ context :fimbriation)])
       (rf/subscribe [:validate-tinctures field-context parent-field-context (c/++ context :line :fimbriation)])
       (rf/subscribe [:validate-tinctures field-context parent-field-context (c/++ context :opposite-line :fimbriation)])
       (rf/subscribe [:validate-tinctures field-context parent-field-context (c/++ context :extra-line :fimbriation)])]))

  (fn [[main-validation & other-validations] [_ _context]]
    (let [fimbriated? (some (comp :fimbriated? first) other-validations)
          ;; if any of the line validations had fimbriations, then use all of them, because
          ;; each need to be validated on their own then, the main one doesn't matter anymore;
          ;; if on other other hand none of the line ones had fimbriation, then only the main
          ;; check is relevant
          validations (->> (if fimbriated?
                             other-validations
                             [main-validation])
                           (map second))]
      (->> validations
           (apply concat)
           (filter identity)
           sort-validations))))

;; TODO: probably a bug, this derefs subscriptions inside the subscription building function
(rf/reg-sub :validate-charge
  (fn [[_ context] _]
    (let [field-context (c/++ context :field)
          parent-semy-context (c/-- context)
          parent-charge-group-context (c/-- context 2)
          parent-type (some->
                       (or
                        @(rf/subscribe [:get (c/++ parent-semy-context :type)])
                        @(rf/subscribe [:get (c/++ parent-charge-group-context :type)]))
                       component/type->component-type)
          parent-field-context (case parent-type
                                 :heraldry.component/charge-group (c/-- parent-charge-group-context 2)
                                 :heraldry.component/semy (c/-- parent-semy-context 2)
                                 (c/-- context 2))]
      (rf/subscribe [:validate-tinctures field-context parent-field-context (c/++ context :fimbriation)])))

  (fn [main-validation [_ _context]]
    (sort-validations (second main-validation))))

(rf/reg-sub :validate-cottise
  (fn [[_ context] _]
    (let [field-context (c/++ context :field)
          parent-field-context (-> context
                                   (c/-- 2)
                                   (c/++ :field))]
      [(rf/subscribe [:validate-tinctures field-context parent-field-context (c/++ context :fimbriation)])
       (rf/subscribe [:validate-tinctures field-context parent-field-context (c/++ context :line :fimbriation)])
       (rf/subscribe [:validate-tinctures field-context parent-field-context (c/++ context :opposite-line :fimbriation)])]))

  (fn [[main-validation & other-validations] [_ _context]]
    (let [fimbriated? (some (comp :fimbriated? first) other-validations)
          ;; if any of the line validations had fimbriations, then use all of them, because
          ;; each need to be validated on their own then, the main one doesn't matter anymore;
          ;; if on other other hand none of the line ones had fimbriation, then only the main
          ;; check is relevant
          validations (->> (if fimbriated?
                             other-validations
                             [main-validation])
                           (map second))]
      (->> validations
           (apply concat)
           (filter identity)
           sort-validations))))

(rf/reg-sub :validate-field
  (fn [[_ context] _]
    [(rf/subscribe [:get (c/++ context :type)])
     (rf/subscribe [:heraldry.state/sanitized-data (c/++ context :layout :num-fields-x)])
     (rf/subscribe [:heraldry.state/sanitized-data (c/++ context :layout :num-fields-y)])])

  (fn [[field-type num-fields-x num-fields-y] [_ _context]]
    (let [field-type (-> field-type name keyword)]
      (cond-> []
        (and (= field-type :paly)
             (odd? num-fields-x)) (conj {:level :warning
                                         :message (string "Paly should have an even number of fields, for an odd number use pales.")})
        (and (= field-type :barry)
             (odd? num-fields-y)) (conj {:level :warning
                                         :message (string "Barry should have an even number of fields, for an odd number use bars.")})
        (and (#{:bendy :bendy-sinister} field-type)
             (odd? num-fields-y)) (conj {:level :warning
                                         :message (string "Bendy should have an even number of fields, for an odd number use bends.")})))))

(rf/reg-sub :validate-attribution
  (fn [[_ context] _]
    [(rf/subscribe [:heraldry.state/sanitized-data (c/++ context :nature)])
     (rf/subscribe [:heraldry.state/sanitized-data (c/++ context :source-license)])
     (rf/subscribe [:heraldry.state/sanitized-data (c/++ context :source-name)])
     (rf/subscribe [:heraldry.state/sanitized-data (c/++ context :source-link)])
     (rf/subscribe [:heraldry.state/sanitized-data (c/++ context :source-creator-name)])
     (rf/subscribe [:heraldry.state/sanitized-data (c/++ context :source-creator-link)])])

  (fn [[nature & source-fields] [_ _context]]
    (when (and (= nature :derivative)
               (seq (filter (fn [value]
                              (if (keyword? value)
                                (= value :none)
                                (-> value (or "") s/trim count zero?))) source-fields)))
      [{:level :error
        :message (string "All source fields required for derivative work.")}])))

(rf/reg-sub :validate-is-public
  (fn [[_ context] _]
    [(rf/subscribe [:heraldry.state/sanitized-data (c/++ context :is-public)])
     (rf/subscribe [:heraldry.state/sanitized-data (c/++ context :attribution :license)])])

  (fn [[is-public license] [_ _context]]
    (when (and is-public (= license :none))
      [{:level :error
        :message (string "License required for public objects.")}])))

(rf/reg-sub :validate-arms-general
  (fn [[_ context] _]
    [(rf/subscribe [:validate-is-public context])
     (rf/subscribe [:validate-attribution (c/++ context :attribution)])])

  (fn [[is-public
        attribution] [_ _context]]
    (concat
     is-public
     attribution)))

(rf/reg-sub :validate-charge-general
  (fn [[_ context] _]
    [(rf/subscribe [:validate-is-public context])
     (rf/subscribe [:validate-attribution (c/++ context :attribution)])])

  (fn [[is-public
        attribution] [_ _context]]
    (concat
     is-public
     attribution)))

(rf/reg-sub :validate-collection-general
  (fn [[_ context] _]
    [(rf/subscribe [:validate-is-public context])
     (rf/subscribe [:validate-attribution (c/++ context :attribution)])])

  (fn [[is-public
        attribution] [_ _context]]
    (concat
     is-public
     attribution)))

(rf/reg-sub :validate-ribbon-general
  (fn [[_ context] _]
    [(rf/subscribe [:validate-is-public context])
     (rf/subscribe [:validate-attribution (c/++ context :attribution)])])

  (fn [[is-public
        attribution] [_ _context]]
    (concat
     is-public
     attribution)))

(defn render-icon [level]
  [:i.fas.fa-exclamation-triangle {:style {:color (validation-color level)}}])

(defn render [validation]
  (if (seq validation)
    (let [first-message (first validation)]
      [:div.tooltip.info {:style {:display "inline-block"
                                  :margin-left "0.2em"}}
       [render-icon (:level first-message)]
       [:div.bottom {:style {:width "25em"}}
        [:ul {:style {:position "relative"
                      :padding-left "1.8em"}}
         (doall
          (for [[idx {:keys [level message]}] (map-indexed vector validation)]
            ^{:key idx}
            [:li [:div {:style {:position "absolute"
                                :left "0em"}}
                  [render-icon level]]
             [tr message]]))]]])
    [:<>]))

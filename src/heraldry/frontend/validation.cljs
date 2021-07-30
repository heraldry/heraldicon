(ns heraldry.frontend.validation
  (:require [clojure.string :as s]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.interface :as interface]
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
  (fn [[_ path] _]
    [(rf/subscribe [:get-sanitized-data (conj path :type)])
     (rf/subscribe [:get-sanitized-data (conj path :tincture)])
     (rf/subscribe [:get-sanitized-data (conj path :fields 0 :type)])
     (rf/subscribe [:get-sanitized-data (conj path :fields 0 :tincture)])
     (rf/subscribe [:get-sanitized-data (conj path :fields 1 :type)])
     (rf/subscribe [:get-sanitized-data (conj path :fields 1 :tincture)])])

  (fn [[field-type
        tincture
        subfield-1-type
        subfield-1-tincture
        subfield-2-type
        subfield-2-tincture] [_ _path]]
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
  (fn [[_ path] _]
    [(rf/subscribe [:get-sanitized-data (conj path :tincture-1)])
     (rf/subscribe [:get-sanitized-data (conj path :tincture-2)])])

  (fn [[tincture-1 tincture-2] [_ _path]]
    [tincture-1 tincture-2]))

(defn list-tinctures [tinctures]
  (->> tinctures
       sort
       (map util/translate-tincture)
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
                     :message (str "Metal " (if fimbriated?
                                              "fimbriation"
                                              "field") " (" (list-tinctures own-tinctures)
                                   ") on metal field (" (list-tinctures parent-tinctures)
                                   ") breaks rule of tincture.")}
      (= #{:colour}
         parent-kinds
         own-kinds) {:level :warning
                     :message (str "Colour " (if fimbriated?
                                               "fimbriation"
                                               "field") " (" (list-tinctures own-tinctures)
                                   ") on colour field (" (list-tinctures parent-tinctures)
                                   ") breaks rule of tincture.")})))

(rf/reg-sub :validate-tinctures
  (fn [[_ field-path parent-field-path fimbriation-path] _]
    [(rf/subscribe [:field-tinctures-for-validation field-path])
     (rf/subscribe [:field-tinctures-for-validation parent-field-path])
     (rf/subscribe [:fimbriation-tinctures-for-validation fimbriation-path])
     (rf/subscribe [:get-relevant-options fimbriation-path])])

  (fn [[field-tinctures
        parent-field-tinctures
        [fimbriation-tincture-1
         fimbriation-tincture-2]
        fimbriation-options] [_ field-path _parent-field-path fimbriation-path]]
    (when (or fimbriation-options
              (= (drop-last field-path) (drop-last fimbriation-path)))
      (let [which (-> fimbriation-path
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
                                                   (str (case which
                                                          :line "Main line: "
                                                          :opposite-line "Opposite line: "
                                                          :extra-line "Extra line: "
                                                          nil)
                                                        message)))))

           (= fimbriation-tincture-1-kind
              fimbriation-tincture-2-kind
              :metal) (conj {:level :note
                             :message (str "Fimbriation tinctures are both metal ("
                                           (util/translate-tincture fimbriation-tincture-1)
                                           " and "
                                           (util/translate-tincture fimbriation-tincture-2)
                                           ").")})

           (= fimbriation-tincture-1-kind
              fimbriation-tincture-2-kind
              :colour) (conj {:level :note
                              :message (str "Fimbriation tinctures are both colour ("
                                            (util/translate-tincture fimbriation-tincture-1)
                                            " and "
                                            (util/translate-tincture fimbriation-tincture-2)
                                            ").")})

           (= (set [fimbriation-tincture-1-kind])
              field-tincture-kinds
              #{:metal}) (conj {:level :note
                                :message (str "Fimbriation metal tincture ("
                                              (util/translate-tincture fimbriation-tincture-1)
                                              ") touches metal field ("
                                              (list-tinctures field-tinctures)
                                              ").")})

           (= (set [fimbriation-tincture-1-kind])
              field-tincture-kinds
              #{:colour}) (conj {:level :note
                                 :message (str "Fimbriation colour tincture ("
                                               (util/translate-tincture fimbriation-tincture-1)
                                               ") touches colour field ("
                                               (list-tinctures field-tinctures)
                                               ").")}))]))))

(defn sort-validations [validations]
  (sort-by (fn [validation]
             [(-> validation :level level-order)
              (-> validation :which which-order)])
           validations))

(rf/reg-sub :validate-ordinary
  (fn [[_ path] _]
    (let [field-path (conj path :field)
          parent-field-path (->> path
                                 (drop-last 2)
                                 vec)]
      [(rf/subscribe [:validate-tinctures field-path parent-field-path (conj path :fimbriation)])
       (rf/subscribe [:validate-tinctures field-path parent-field-path (conj path :line :fimbriation)])
       (rf/subscribe [:validate-tinctures field-path parent-field-path (conj path :opposite-line :fimbriation)])
       (rf/subscribe [:validate-tinctures field-path parent-field-path (conj path :extra-line :fimbriation)])]))

  (fn [[main-validation & other-validations] [_ _path]]
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

(rf/reg-sub :validate-charge
  (fn [[_ path] _]
    (let [field-path (conj path :field)
          parent-semy-path (->> path
                                drop-last
                                vec)
          parent-charge-group-path (->> path
                                        (drop-last 2)
                                        vec)
          parent-type (some->
                       (or
                        @(rf/subscribe [:get-value (conj parent-semy-path :type)])
                        @(rf/subscribe [:get-value (conj parent-charge-group-path :type)]))
                       interface/type->component-type)
          parent-field-path (case parent-type
                              :heraldry.component/charge-group (->> parent-charge-group-path
                                                                    (drop-last 2)
                                                                    vec)
                              :heraldry.component/semy (->> parent-semy-path
                                                            (drop-last 2)
                                                            vec)
                              (->> path
                                   (drop-last 2)
                                   vec))]
      (rf/subscribe [:validate-tinctures field-path parent-field-path (conj path :fimbriation)])))

  (fn [main-validation [_ _path]]
    (sort-validations (second main-validation))))

(rf/reg-sub :validate-cottise
  (fn [[_ path] _]
    (let [field-path (conj path :field)
          parent-field-path (-> path
                                (->> (drop-last 2))
                                vec
                                (conj :field))]
      [(rf/subscribe [:validate-tinctures field-path parent-field-path (conj path :fimbriation)])
       (rf/subscribe [:validate-tinctures field-path parent-field-path (conj path :line :fimbriation)])
       (rf/subscribe [:validate-tinctures field-path parent-field-path (conj path :opposite-line :fimbriation)])]))

  (fn [[main-validation & other-validations] [_ _path]]
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
  (fn [[_ path] _]
    [(rf/subscribe [:get-value (conj path :type)])
     (rf/subscribe [:get-sanitized-data (conj path :layout :num-fields-x)])
     (rf/subscribe [:get-sanitized-data (conj path :layout :num-fields-y)])])

  (fn [[field-type num-fields-x num-fields-y] [_ _path]]
    (let [field-type (-> field-type name keyword)]
      (cond-> []
        (and (= field-type :paly)
             (odd? num-fields-x)) (conj {:level :warning
                                         :message "Paly should have an even number of fields, for an odd number use pales."})
        (and (= field-type :barry)
             (odd? num-fields-y)) (conj {:level :warning
                                         :message "Barry should have an even number of fields, for an odd number use bars."})
        (and (#{:bendy :bendy-sinister} field-type)
             (odd? num-fields-y)) (conj {:level :warning
                                         :message "Bendy should have an even number of fields, for an odd number use bends."})))))

(rf/reg-sub :validate-attribution
  (fn [[_ path] _]
    [(rf/subscribe [:get-sanitized-data (conj path :nature)])
     (rf/subscribe [:get-sanitized-data (conj path :source-license)])
     (rf/subscribe [:get-sanitized-data (conj path :source-name)])
     (rf/subscribe [:get-sanitized-data (conj path :source-link)])
     (rf/subscribe [:get-sanitized-data (conj path :source-creator-name)])
     (rf/subscribe [:get-sanitized-data (conj path :source-creator-link)])])

  (fn [[nature & source-fields] [_ _path]]
    (when (and (= nature :derivative)
               (seq (filter (fn [value]
                              (if (keyword? value)
                                (= value :none)
                                (-> value (or "") s/trim count zero?))) source-fields)))
      [{:level :error
        :message "All source fields required for derivative work."}])))

(rf/reg-sub :validate-is-public
  (fn [[_ path] _]
    [(rf/subscribe [:get-sanitized-data (conj path :is-public)])
     (rf/subscribe [:get-sanitized-data (conj path :attribution :license)])])

  (fn [[is-public license] [_ _path]]
    (when (and is-public (= license :none))
      [{:level :error
        :message "License required for public objects."}])))

(rf/reg-sub :validate-arms-general
  (fn [[_ path] _]
    [(rf/subscribe [:validate-is-public path])
     (rf/subscribe [:validate-attribution (conj path :attribution)])])

  (fn [[is-public
        attribution] [_ _path]]
    (concat
     is-public
     attribution)))

(rf/reg-sub :validate-charge-general
  (fn [[_ path] _]
    [(rf/subscribe [:validate-is-public path])
     (rf/subscribe [:validate-attribution (conj path :attribution)])])

  (fn [[is-public
        attribution] [_ _path]]
    (concat
     is-public
     attribution)))

(rf/reg-sub :validate-collection-general
  (fn [[_ path] _]
    [(rf/subscribe [:validate-is-public path])
     (rf/subscribe [:validate-attribution (conj path :attribution)])])

  (fn [[is-public
        attribution] [_ _path]]
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
             message]))]]])
    [:<>]))

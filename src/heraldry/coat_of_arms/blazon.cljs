(ns heraldry.coat-of-arms.blazon
  (:require [heraldry.coat-of-arms.attributes :as attributes]
            [heraldry.coat-of-arms.field.core :as field]
            [heraldry.frontend.charge :as charge]
            [heraldry.frontend.util :as util]))

(declare encode-field)

(defn encode-ordinary [{:keys [type field line]}]
  (let [rest (util/combine " " [(util/translate type)
                                (util/translate-line line)
                                (encode-field field)])
        article (if (re-matches #"(?i)^[aeiouh].*" rest)
                  "an"
                  "a")]
    (util/combine " " [article rest])))

(defn encode-charge [{:keys [type attitude field tincture variant]}
                     & {:keys [part-of-charge-group? pluralize?]}]
  (let [charge-data (when variant
                      (charge/fetch-charge-data variant))
        fixed-tincture (-> charge-data
                           :fixed-tincture
                           (or :none)
                           (#(when (not= :none %) %)))]
    (util/combine " " [(when-not part-of-charge-group?
                         "a") (str (util/translate type)
                                   (when pluralize?
                                     "s"))
                       (util/translate attitude)
                       (if fixed-tincture
                         (util/translate fixed-tincture)
                         (encode-field field))
                       (util/combine " and " (map (fn [colour-key]
                                                    (when-let [t (get tincture colour-key)]
                                                      (when (not= t :none)
                                                        (util/combine " " [(util/translate colour-key)
                                                                           (util/translate t)]))))
                                                  (-> attributes/tincture-modifier-map
                                                      keys
                                                      sort)))])))

(defn encode-charge-group [charge-group]
  (let [used-indices (->> charge-group
                          (tree-seq #(or (map? %)
                                         (vector? %)
                                         (seq? %))
                                    (fn [data]
                                      (cond
                                        (and (map? data)
                                             (:slots data)) [data]
                                        (map? data) (:strips data))
                                      (seq data)))
                          (filter :slots)
                          (mapcat :slots)
                          (filter identity)
                          (group-by identity))
        charges (:charges charge-group)]
    (util/combine
     ", "
     (map (fn [[k v]]
            (str (count v) " "
                 (encode-charge (get charges k)
                                :part-of-charge-group? true
                                :pluralize? (> (count v) 1)))) used-indices))))

(defn encode-semy [component]
  (str "semy of " (-> component :charge :type util/translate)))

(defn encode-component [component]
  (case (-> component :type namespace)
    "heraldry.ordinary.type" (encode-ordinary component)
    "heraldry.charge.type" (encode-charge component)
    "heraldry.charge-group.type" (encode-charge-group component)
    "heraldry.component" (encode-semy component)))

(defn encode-field [{:keys [components counterchanged?] :as field} & {:keys [root?]}]
  (if counterchanged?
    "counterchanged"
    (let [field-description (case (:type field)
                              :heraldry.field.type/plain (util/translate-tincture (:tincture field))
                              (let [{:keys [type line fields]} field
                                    mandatory-part-count (field/mandatory-part-count-old field)]
                                (util/combine
                                 " "
                                 [(util/translate type)
                                  (util/translate-line line)
                                  (util/combine " and "
                                                (map
                                                 (fn [[index part]]
                                                   (cond
                                                     (< index
                                                        mandatory-part-count) (encode-field part)
                                                     (-> part
                                                         :type
                                                         (not= :heraldry.field.type/ref)) (util/combine
                                                                                           " "
                                                                                           [(when (-> fields
                                                                                                      count
                                                                                                      (> 3))
                                                                                              (field/part-name type index))
                                                                                            (encode-field part)])))
                                                 (map-indexed vector fields)))])))
          components-description (util/combine ", " (map encode-component components))
          blazon (util/upper-case-first
                  (util/combine ", " [field-description
                                      components-description]))]
      (if (or root?
              (and (-> field :type (= :heraldry.field.type/plain))
                   (-> components-description count zero?)))
        blazon
        (str "[" blazon "]")))))

(ns heraldry.coat-of-arms.blazon
  (:require [heraldry.coat-of-arms.division :as division]
            [heraldry.coat-of-arms.util :as util]))

(declare encode-field)

(defn encode-ordinary [{:keys [type field line]}]
  (let [rest (util/combine " " [(util/translate type)
                                (util/translate-line line)
                                (encode-field field)])
        article (if (re-matches #"(?i)^[aeiouh].*" rest)
                  "an"
                  "a")]
    (util/combine " " [article rest])))

(defn encode-charge [{:keys [type attitude field tincture]}]
  (util/combine " " ["a" (util/translate type)
                     (util/translate attitude)
                     (encode-field field)
                     (util/combine " and " (map (fn [colour-key]
                                                  (when-let [t (get tincture colour-key)]
                                                    (when (not= t :none)
                                                      (util/combine " " [(util/translate colour-key)
                                                                         (util/translate t)]))))
                                                [:armed :langued :attired :unguled]))]))

(defn encode-component [component]
  (case (:component component)
    :ordinary (encode-ordinary component)
    :charge (encode-charge component)))

(defn encode-field [{:keys [division components counterchanged?] :as field} & {:keys [root?]}]
  (if counterchanged?
    "counterchanged"
    (let [tincture (get-in field [:content :tincture])
          field-description (cond
                              tincture (util/translate-tincture tincture)
                              division (let [{:keys [type line fields]} division
                                             mandatory-part-count (division/mandatory-part-count type)]
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
                                                              (not (:ref part)) (util/combine
                                                                                 " "
                                                                                 [(when (-> fields
                                                                                            count
                                                                                            (> 3))
                                                                                    (division/part-name type index))
                                                                                  (encode-field part)])))
                                                          (map-indexed vector fields)))])))
          components-description (util/combine ", " (map encode-component components))
          blazon (util/upper-case-first
                  (util/combine ", " [field-description
                                      components-description]))]
      (if (or root?
              (and tincture
                   (-> components-description count zero?)))
        blazon
        (str "[" blazon "]")))))

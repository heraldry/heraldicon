(ns or.coad.blazon
  (:require [clojure.string :as s]
            [or.coad.division :as division]
            [or.coad.util :as util]))

(declare encode-field)

(defn combine [separator words]
  (s/join separator (filter #(> (count %) 0) words)))

(defn translate-line [{:keys [style]}]
  (when (not= style :straight)
    (util/translate style)))

(defn encode-ordinary [{:keys [type field line]}]
  (combine " " ["a" (util/translate type)
                (translate-line line)
                (encode-field field)]))

(defn encode-charge [{:keys [type attitude tincture]}]
  (let [primary (:primary tincture)]
    (combine " " ["a" (util/translate type)
                  (util/translate attitude)
                  (util/translate primary)
                  (combine " and " (map (fn [colour-key]
                                          (when-let [t (get tincture colour-key)]
                                            (when (not= t primary)
                                              (combine " " [(util/translate colour-key)
                                                            (util/translate t)]))))
                                        [:armed :langued :attired :unguled]))])))

(defn encode-field [{:keys [division ordinaries charges] :as field} & {:keys [root?]}]
  (let [tincture (get-in field [:content :tincture])
        field-description (cond
                            tincture (util/translate tincture)
                            division (let [{:keys [type line fields]} division
                                           mandatory-part-count (division/mandatory-part-count type)]
                                       (combine
                                        " "
                                        [(util/translate type)
                                         (translate-line line)
                                         (combine " and "
                                                  (map
                                                   (fn [[index part]]
                                                     (cond
                                                       (< index
                                                          mandatory-part-count) (encode-field part)
                                                       (not (number? part)) (combine " " [(division/part-name type index) (encode-field part)])))
                                                   (sort-by #(division/part-position type (first %))
                                                            (map-indexed vector fields))))])))
        ordinaries-description (combine ", " (map encode-ordinary ordinaries))
        charges-description (combine ", " (map encode-charge charges))
        blazon (util/upper-case-first
                (combine ", " [field-description
                               ordinaries-description
                               charges-description]))]
    (if (or root?
            (and tincture
                 (-> ordinaries-description count zero?)
                 (-> charges-description count zero?)))
      blazon
      (str "[" blazon "]"))))

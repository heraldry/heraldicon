(ns or.coad.blazon
  (:require [or.coad.division :as division]
            [or.coad.util :as util]))

(declare encode-field)

(defn encode-ordinary [{:keys [type field line]}]
  (util/combine " " ["a" (util/translate type)
                     (util/translate-line line)
                     (encode-field field)]))

(defn encode-charge [{:keys [type attitude field tincture]}]
  (util/combine " " ["a" (util/translate type)
                     (util/translate attitude)
                     (encode-field field)
                     (util/combine " and " (map (fn [colour-key]
                                                  (when-let [t (get tincture colour-key)]
                                                    (util/combine " " [(util/translate colour-key)
                                                                       (util/translate t)])))
                                                [:armed :langued :attired :unguled]))]))

(defn encode-field [{:keys [division ordinaries charges counterchanged?] :as field} & {:keys [root?]}]
  (if counterchanged?
    "counterchanged"
    (let [tincture               (get-in  field [:content :tincture])
          field-description      (cond
                                   tincture (util/translate tincture)
                                   division (let [{:keys [type line fields]} division
                                                  mandatory-part-count       (division/mandatory-part-count type)]
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
                                                                   (not (:ref part))        (util/combine " " [(division/part-name type index) (encode-field part)])))
                                                               (sort-by #(division/part-position type (first %))
                                                                        (map-indexed vector fields))))])))
          ordinaries-description (util/combine ", " (map encode-ordinary ordinaries))
          charges-description    (util/combine ", " (map encode-charge charges))
          blazon                 (util/upper-case-first
                                  (util/combine ", " [field-description
                                                      ordinaries-description
                                                      charges-description]))]
      (if (or root?
              (and tincture
                   (-> ordinaries-description count zero?)
                   (-> charges-description count zero?)))
        blazon
        (str "[" blazon "]")))))

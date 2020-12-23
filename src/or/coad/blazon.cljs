(ns or.coad.blazon
  (:require [clojure.string :as s]
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

(defn encode-field [field & {:keys [root?]}]
  (let [division               (:division field)
        ordinaries             (:ordinaries field)
        tincture               (get-in  field [:content :tincture])
        field-description      (cond
                                 tincture (util/translate tincture)
                                 division (let [{:keys [type line fields]} division]
                                            (combine " " [(util/translate type)
                                                          (translate-line line)
                                                          (combine " and " (map encode-field fields))])))
        ordinaries-description (combine ", " (map encode-ordinary ordinaries))
        blazon                 (util/upper-case-first (combine ", " [field-description ordinaries-description]))]
    (if (or root?
            tincture)
      blazon
      (str "[" blazon "]"))))

(ns or.coad.blazon
  (:require [clojure.string :as s]))

(declare encode-field)

(defn translate [keyword]
  (-> keyword
      name
      (s/replace "-" " ")))

(defn combine [separator words]
  (s/join separator (filter #(> (count %) 0) words)))

(defn translate-line [{:keys [style]}]
  (when (not= style :straight)
    (translate style)))

(defn encode-ordinary [{:keys [type content line]}]
  (combine " " ["a" (translate type)
                (translate-line line)
                (encode-field content)]))

(defn upper-case-first [s]
  (str (s/upper-case (or (first s) "")) (s/join (rest s))))

(defn encode-field [field & {:keys [root?]}]
  (let [division (:division field)
        ordinaries (:ordinaries field)
        tincture (get-in field [:content :tincture])
        field-description (cond
                            tincture (translate tincture)
                            division (let [{:keys [type line content]} division]
                                       (combine " " [(translate type)
                                                     (translate-line line)
                                                     (combine " and " (map encode-field content))])))
        ordinaries-description (combine ", " (map encode-ordinary ordinaries))
        blazon (upper-case-first (combine ", " [field-description ordinaries-description]))]
    (if (or root?
            tincture)
      blazon
      (str "[" blazon "]"))))

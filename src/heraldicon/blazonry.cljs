(ns heraldicon.blazonry
  (:require
   [clojure.string :as s]
   [heraldicon.localization.string :as string]))

;; TODO: this could become a multi method based on a keyword hierarchy,
;; heraldry.tincture/translate-tincture would then be a defmethod for a
;; tincture keyword, etc.
(defn translate [keyword]
  (when keyword
    (-> keyword
        name
        (s/replace "-" " ")
        (s/replace "fleur de lis" "fleur-de-lis")
        (s/replace "fleur de lys" "fleur-de-lys"))))

(defn translate-line [{:keys [type]}]
  (when (not= type :straight)
    (translate type)))

(defn translate-cap-first [keyword]
  (-> keyword
      translate
      string/upper-case-first))

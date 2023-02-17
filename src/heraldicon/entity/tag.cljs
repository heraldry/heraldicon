(ns heraldicon.entity.tag
  (:require
   [clojure.string :as str]))

(defn clean [tag]
  (when-let [tag (some-> tag
                         (cond->
                           (keyword? tag) name)
                         str/lower-case
                         str/trim
                         (str/replace #"^-*" "")
                         (str/replace #"-*$" ""))]
    (when (and (re-matches #"^[a-z0-9][a-z0-9-]*[a-z0-9]$" tag)
               (not (re-matches #".*heraldicon.*" tag)))
      (keyword tag))))

(ns heraldry.tag
  (:require [clojure.string :as s]))

(defn clean [tag]
  (when-let [tag (some-> tag
                         (cond->
                           (keyword? tag) name)
                         s/lower-case
                         s/trim
                         (s/replace #"^-*" "")
                         (s/replace #"-*$" ""))]
    (when (and (re-matches #"^[a-z0-9][a-z0-9-]*[a-z0-9]$" tag)
               (not (re-matches #".*heraldicon.*" tag)))
      (keyword tag))))

(ns heraldicon.heraldry.component
  (:require
   [clojure.string :as s]))

(defn effective-type [raw-type]
  (when (some-> raw-type
                str
                (s/starts-with? ":heraldry.charge.type/"))
    (derive raw-type :heraldry.charge/type))
  ;; TODO: need to limit this to heraldry.* and heraldicon.* namespaces for now,
  ;; as there are types that aren't option roots or components, e.g. lines, but there
  ;; might be a better way
  (when (and (keyword? raw-type)
             (some-> raw-type namespace (s/starts-with? "herald")))
    raw-type))

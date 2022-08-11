(ns heraldicon.heraldry.ordinary.post-process
  (:require
   [heraldicon.heraldry.ordinary.humetty :as humetty]
   [heraldicon.interface :as interface]))

(defn properties [{:keys [humetty line opposite-line extra-line]
                   :as properties}]
  (if (:humetty? humetty)
    (cond-> properties
      opposite-line (update :opposite-line merge (select-keys line [:fimbriation :effective-height]))
      extra-line (update :extra-line merge (select-keys line [:fimbriation :effective-height])))
    properties))

(defn shape [{:keys [shape]
              :as shape-data} context {:keys [humetty]}]
  (let [shape (if (vector? shape)
                shape
                [shape])]
    (if (:humetty? humetty)
      (let [parent-shape (interface/get-exact-parent-shape context)
            adjusted-shape (humetty/coup-2 shape parent-shape humetty)]
        (assoc shape-data
               :shape adjusted-shape
               :lines [{:edge-paths adjusted-shape}]))
      shape-data)))

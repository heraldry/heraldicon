(ns heraldicon.render.ir
  "Render results as data. :defs maps each def's id to its element, :body is
  hiccup that references the defs by id. Merging maps the same id to one def,
  so a def shared by several parts ends up in the output once.")

(def blank
  {:defs (sorted-map)
   :body nil})

(defn add-def
  "Adds a def element such as [:mask {:id ...} ...], keyed by its id."
  [ir element]
  (assoc-in ir [:defs (-> element second :id)] element))

(defn merge-defs
  "Merges the defs of other into ir, keeping ir's body."
  [ir other]
  (update ir :defs merge (:defs other)))

(defn ->hiccup [{:keys [defs body]}]
  [:<>
   (when (seq defs)
     (into [:defs] (vals defs)))
   body])

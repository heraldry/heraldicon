(ns heraldicon.render.ir
  "Render results as data. :defs maps each def's id to its element, :body is
  hiccup that references the defs by id, and :children lists the contexts of
  the nodes the body embeds. Merging maps the same id to one def, so a def
  shared by several parts ends up in the output once.")

(def blank
  {:defs (sorted-map)
   :body nil
   :children []})

(defn add-def
  "Adds a def element such as [:mask {:id ...} ...], keyed by its id."
  [ir element]
  (assoc-in ir [:defs (-> element second :id)] element))

(defn add-child
  "Registers the context of a node that the body embeds, so its defs are
  collected with this node's."
  [ir context]
  (update ir :children conj context))

(defn combine
  "Merges the defs and children of other into ir, keeping ir's body."
  [ir other]
  (-> ir
      (update :defs merge (:defs other))
      (update :children into (:children other))))

(defn- def-element
  ;; a stable component type: an anonymous fn would be a new type per render
  [element]
  element)

(defn defs->hiccup
  "Each def as its own keyed component, so an unchanged def isn't converted
  again when the set of defs changes."
  [defs]
  (when (seq defs)
    (into [:defs]
          (map (fn [[id element]]
                 ^{:key id} [def-element element]))
          defs)))

(defn ->hiccup [{:keys [defs body]}]
  [:<>
   (defs->hiccup defs)
   body])

(ns heraldicon.frontend.component.drag
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.element :as component.element]
   [heraldicon.heraldry.charge-group.core :as charge-group]
   [heraldicon.heraldry.shield-separator :as shield-separator]
   [re-frame.core :as rf]))

(defn- field-component?
  [type]
  (or (isa? type :heraldry/ordinary)
      (isa? type :heraldry/charge)
      (isa? type :heraldry/charge-group)
      (isa? type :heraldry/semy)))

(defn parent?
  [drag-node drop-node]
  (= (:path (:parent-context drag-node))
     (:path (:context drop-node))))

(defn- drop-allowed?
  [drag-node drop-node]
  (let [drag-type (:type drag-node)
        drop-type (:type drop-node)]
    (or (and (isa? drag-type :heraldicon.entity.collection/element)
             (isa? drop-type :heraldicon.entity.collection/data))

        (and (isa? drag-type :heraldicon/charge-type)
             (isa? drop-type :heraldicon/charge-type))

        (and (isa? drag-type :heraldry/helm)
             (isa? drop-type :heraldry/helms))

        (and (isa? drag-type :heraldry/shield-separator)
             (parent? drag-node drop-node)
             (or (isa? drop-type :heraldry/helm)
                 (isa? drop-type :heraldry/ornaments)))

        (and (or (isa? drag-type :heraldry/motto)
                 (isa? drag-type :heraldry/charge)
                 (isa? drag-type :heraldry/charge-group))
             (isa? drop-type :heraldry/helm))

        (and (field-component? drag-type)
             (or (isa? drop-type :heraldry/field)
                 (isa? drop-type :heraldry/subfield)
                 (isa? drop-type :heraldry/ordinary)
                 (isa? drop-type :heraldry/charge)))

        (and (isa? drag-type :heraldry/subfield)
             (isa? drop-type :heraldry/field)
             (parent? drag-node drop-node))

        (and (or (isa? drag-type :heraldry/motto)
                 (isa? drag-type :heraldry/charge)
                 (isa? drag-type :heraldry/charge-group))
             (isa? drop-type :heraldry/ornaments))

        (and (isa? drag-type :heraldry/charge)
             (isa? drop-type :heraldry/charge-group)))))

(defn- parent-node
  [{:keys [parent-context parent-type]
    :as node}]
  (-> node
      (assoc :context parent-context)
      (assoc :type parent-type)
      (dissoc :parent-context :parent-type)))

(defn inside-own-subtree?
  [drag-path drop-path]
  (= (take (count drag-path) drop-path)
     drag-path))

(defn- sibling?
  [path-1 path-2]
  (= (drop-last path-1)
     (drop-last path-2)))

(defn- allowed-edges
  [drag-path drop-path]
  (when (not= drag-path drop-path)
    (let [current-index (last drag-path)
          new-index (last drop-path)
          siblings? (sibling? drag-path drop-path)]
      (cond-> #{:above :below}
        siblings? (cond->
                    (= new-index
                       (dec current-index)) (disj :below)

                    (= new-index
                       (inc current-index)) (disj :above))))))

(defn drop-options
  [drag-node drop-node]
  (let [drag-path (:path (:context drag-node))
        drop-path (:path (:context drop-node))
        drag-type (:type drag-node)
        drop-type (:type drop-node)]
    (when-not (inside-own-subtree? drag-path drop-path)
      (let [inside? (and (not (parent? drag-node drop-node))
                         (drop-allowed? drag-node drop-node))
            allowed-in-parent? (and (drop-allowed? drag-node (parent-node drop-node))
                                    (or (not (isa? drop-type :heraldry/subfield))
                                        (and (isa? drag-type :heraldry/subfield)
                                             (isa? drop-type :heraldry/subfield))))
            in-collection? (int? (last drop-path))
            allowed-edges (allowed-edges drag-path drop-path)
            above? (and allowed-in-parent?
                        in-collection?
                        (get allowed-edges :above))
            below? (and allowed-in-parent?
                        in-collection?
                        (get allowed-edges :below)
                        (not (:open? drop-node)))]
        (cond-> #{}
          inside? (conj :inside)
          above? (conj :above)
          below? (conj :below))))))

(defn- drop-inside-target-context
  [drag-node drop-node]
  (let [drag-type (:type drag-node)
        drop-type (:type drop-node)]
    (cond
      (and (isa? drag-type :heraldicon/charge-type)
           (isa? drop-type :heraldicon/charge-type)) (c/++ (:context drop-node)
                                                           :types component.element/APPEND-INDEX)

      (and (field-component? drag-type)
           (isa? drop-type :heraldry/field)) (c/++ (:context drop-node)
                                                   :components component.element/APPEND-INDEX)

      (and (field-component? drag-type)
           (isa? drop-type :heraldry/subfield)) (c/++ (:context drop-node)
                                                      :field :components component.element/APPEND-INDEX)

      (and (field-component? drag-type)
           (or (isa? drop-type :heraldry/ordinary)
               (isa? drop-type :heraldry/charge))) (c/++ (:context drop-node)
                                                         :field :components component.element/APPEND-INDEX)

      (and (isa? drag-type :heraldry/charge)
           (isa? drop-type :heraldry/charge-group)) (c/++ (:context drop-node)
                                                          :charges component.element/APPEND-INDEX)

      (and (or (isa? drag-type :heraldry/motto)
               (isa? drag-type :heraldry/charge)
               (isa? drag-type :heraldry/charge-group))
           (isa? drop-type :heraldry/ornaments)) (c/++ (:context drop-node)
                                                       :elements component.element/APPEND-INDEX)

      (and (or (isa? drag-type :heraldry/charge)
               (isa? drag-type :heraldry/charge-group))
           (isa? drop-type :heraldry/helm)) (c/++ (:context drop-node)
                                                  :components component.element/APPEND-INDEX)

      :else nil)))

(defn drop-fn
  [drag-node drop-node]
  (let [new-index (last (:path (:context drop-node)))
        drag-node-context (:context drag-node)
        drag-node-parent-type (:parent-type drag-node)
        drop-node-type (:type drop-node)
        drop-node-parent-type (:parent-type drop-node)
        drop-node-context (:context drop-node)
        where (:where drop-node)
        target-context (case where
                         :above (-> drop-node-context c/-- (c/++ new-index))
                         :inside (drop-inside-target-context drag-node drop-node)
                         :below (-> drop-node-context c/-- (c/++ (inc new-index))))
        post-fns (cond-> []
                   (isa? drag-node-parent-type
                         :heraldry/charge-group) (conj #(charge-group/update-missing-charge-indices %1 %2))

                   (or (isa? drag-node-parent-type
                             :heraldry/helm)
                       (isa? drag-node-parent-type
                             :heraldry/ornaments)) (conj #(shield-separator/add-or-remove-shield-separator %1 %2))

                   (or (isa? drop-node-parent-type
                             :heraldry/helm)
                       (isa? drop-node-type
                             :heraldry/helm)
                       (isa? drop-node-parent-type
                             :heraldry/ornaments)
                       (isa? drop-node-type
                             :heraldry/ornaments)) (conj #(shield-separator/add-or-remove-shield-separator %1 %3)))]
    (when target-context
      (rf/dispatch [::component.element/move drag-node-context target-context
                    {:post-fn (when (seq post-fns)
                                (fn [db value-path target-path]
                                  (reduce (fn [db f]
                                            (f db value-path target-path))
                                          db
                                          post-fns)))

                     ;; TODO: this will select a shield-separator if a new component has been
                     ;; added to a helm or ornaments
                     :no-select? (#{:heraldry/helm
                                    :heraldry/shield-separator}
                                  (:type drag-node))}]))))

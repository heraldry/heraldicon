(ns heraldicon.frontend.component.charge-type
  (:require
   [clojure.string :as str]
   [heraldicon.context :as c]
   [heraldicon.frontend.charge-types :as frontend.charge-types]
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.component.drag :as drag]
   [heraldicon.frontend.component.element :as component.element]
   [heraldicon.frontend.component.tree :as tree]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.interface :as interface]
   [re-frame.core :as rf]))

(macros/reg-event-db ::add
  (fn [db [_ {:keys [path]
              ::tree/keys [identifier]} value]]
    (let [types-path (conj path :types)
          elements (-> (get-in db types-path)
                       (conj value)
                       vec)
          new-element-path (conj types-path (-> elements count dec))]
      (-> db
          (assoc-in types-path elements)
          (tree/set-edit-node identifier {:path (conj new-element-path :name)})))))

(defn- remove-charge-type
  [db
   {:keys [path]
    ::tree/keys [identifier]
    :as context}
   & {:keys [force-deleted?]}]
  (if (get-in db (:path (c/++ context :id)))
    [(update-in db (conj path :metadata) (fn [metadata]
                                           (if (and (not force-deleted?)
                                                    (:deleted? metadata))
                                             (dissoc metadata :deleted?)
                                             (assoc metadata :deleted? true))))
     false
     nil]
    (let [[new-db value] (component.element/remove-element db path)
          children (:types value)
          parent-context (c/-- context 2)
          parent-types-path (:path (c/++ parent-context :types))
          siblings (get-in new-db parent-types-path)
          new-siblings (vec (concat siblings children))]
      [(-> new-db
           (assoc-in parent-types-path new-siblings)
           (tree/element-removed identifier path))
       true
       parent-context])))

(macros/reg-event-db ::remove
  (fn [db [_ {::tree/keys [identifier]
              :as context}]]
    (let [[new-db _
           deleted?
           parent-context] (remove-charge-type db context)]
      (cond-> new-db
        deleted? (tree/select-node identifier (:path parent-context) true)))))

(defn- find-root-path
  [db path]
  (let [charge-type-id (get-in db (conj path :id))]
    (if (= charge-type-id :root)
      path
      (when (<= 3 (count path))
        (recur db (subvec path 0 (- (count path) 2)))))))

(defn- find-unknown-charge-type-path
  [db path]
  (let [root-path (find-root-path db path)
        root-types (get-in db (conj root-path :types))
        index (first (filter (fn [idx]
                               (let [name (get-in db (conj root-path :types idx :name))]
                                 (= name "unknown")))
                             (range (count root-types))))]
    (when index
      (conj root-path :types index))))

(macros/reg-event-db ::mark-unknown
  (fn [db [_ {:keys [path]
              ::tree/keys [identifier]
              :as context}]]
    (let [[new-db _
           deleted?] (remove-charge-type db context :force-deleted? true)]
      (if deleted?
        new-db
        (let [target-path (find-unknown-charge-type-path new-db path)
              [new-db _new-value-path] (if target-path
                                         (component.element/move new-db
                                                                 identifier
                                                                 path
                                                                 (conj target-path
                                                                       :types component.element/APPEND-INDEX))
                                         [new-db nil])]
          new-db)))))

(rf/reg-sub-raw ::all-subtypes-count
  (fn [_app-db [_ context]]
    (interface/reaction-or-cache
     ::all-subtypes-count
     context
     #(let [children-count (interface/get-list-size (c/++ context :types))]
        (->> (range children-count)
             (map (fn [idx]
                    @(rf/subscribe [::all-subtypes-count (c/++ context :types idx)])))
             (reduce + children-count))))))

(defn- sort-key
  [{:keys [context]}]
  [(if (pos? (interface/get-list-size (c/++ context :types)))
     0
     1)
   (some-> (interface/get-raw-data (c/++ context :name))
           str/lower-case)])

(defn- sorted-children
  [context]
  (let [num-fields (interface/get-list-size context)]
    (->> (range num-fields)
         (map (fn [idx]
                {:context (c/++ context idx)}))
         (sort-by sort-key))))

(defn- duplicate?
  [type-name]
  (let [name-map @(rf/subscribe [::frontend.charge-types/name-map])]
    (-> name-map
        (get type-name)
        count
        (> 1))))

(defn- deleted?
  [context]
  (interface/get-raw-data (c/++ context :metadata :deleted?)))

(defmethod component/node :heraldicon/charge-type [{::tree/keys [identifier]
                                                    :as context}]
  (let [type-id (interface/get-raw-data (c/++ context :id))
        editing? (= (:ns context) :heraldicon.frontend.charge-types/ns)
        name-context (c/++ context :name)
        types-context (c/++ context :types)
        parent-context (c/-- context 2)
        parent-is-unknown? (= (interface/get-raw-data (c/++ parent-context :name))
                              "unknown")
        root? (= type-id :root)
        type-name (interface/get-raw-data name-context)]
    {:title (if editing?
              (let [num-types @(rf/subscribe [::all-subtypes-count context])
                    deleted?' (deleted? context)
                    deleted-with-undeleted-child? (and deleted?'
                                                       @(rf/subscribe [::frontend.charge-types/undeleted-child? context]))]
                (cond-> type-name
                  (pos? num-types) (str " (" num-types ")")
                  (not type-id) (str " *new*")
                  (duplicate? type-name) (str " *duplicate*")
                  deleted?' (str " *deleted*")
                  deleted-with-undeleted-child? (str " *PROBLEM*")))
              (let [charge-count (interface/get-raw-data (c/++ context :charge_count))]
                (cond-> type-name
                  (pos? charge-count) (str " (" charge-count ")"))))
     :draggable? (when editing?
                   (not root?))
     :drop-options-fn (when editing?
                        drag/drop-options)
     :drop-fn (when editing?
                drag/drop-fn)
     :editable-path (when editing?
                      (:path name-context))
     :buttons (when editing?
                (cond-> [{:icon "fas fa-plus"
                          :title :string.button/add
                          :handler #(rf/dispatch [::add
                                                  context
                                                  {:type :heraldicon/charge-type
                                                   :name "New type"}])}]

                  (not root?) (conj {:icon "far fa-edit"
                                     :title :string.button/edit
                                     :margin "2px"
                                     :handler #(rf/dispatch [::tree/set-edit-node identifier name-context])})

                  (and (not root?)
                       (not parent-is-unknown?)
                       (not= type-name "unknown")) (conj {:icon "fas fa-question-circle"
                                                          :title :string.button/unknown
                                                          :margin "4px"
                                                          :handler #(rf/dispatch [::mark-unknown context])})

                  (not root?) (conj {:icon "far fa-trash-alt"
                                     :remove? true
                                     :title :string.tooltip/remove
                                     :handler #(rf/dispatch [::remove context])})))
     :nodes (sorted-children types-context)}))

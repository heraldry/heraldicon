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

(macros/reg-event-fx ::add
  (fn [{:keys [db]} [_ {:keys [path]} value]]
    (let [types-path (conj path :types)
          elements (-> (get-in db types-path)
                       (conj value)
                       vec)
          new-element-path (conj types-path (-> elements count dec))]
      {:db (-> db
               (assoc-in types-path elements)
               (tree/set-edit-node {:path (conj new-element-path :name)}))})))

(macros/reg-event-fx ::remove
  (fn [{:keys [db]} [_ {:keys [path]
                        :as context}]]
    (if (interface/get-raw-data (c/++ context :id))
      {:db (update-in db (conj path :metadata) (fn [metadata]
                                                 (if (:deleted? metadata)
                                                   (dissoc metadata :deleted?)
                                                   (assoc metadata :deleted? true))))}
      (let [[new-db value] (component.element/remove-element db path)
            children (:types value)
            parent-context (c/-- context 2)
            parent-types-path (:path (c/++ parent-context :types))
            siblings (get-in new-db parent-types-path)
            new-siblings (vec (concat siblings children))]
        {:db (-> new-db
                 (assoc-in parent-types-path new-siblings)
                 (tree/element-removed path)
                 (tree/select-node (:path parent-context) true))}))))

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

(defmethod component/node :heraldicon/charge-type [context]
  (let [type-id (interface/get-raw-data (c/++ context :id))
        name-context (c/++ context :name)
        num-types @(rf/subscribe [::all-subtypes-count context])
        types-context (c/++ context :types)
        root? (= type-id :root)
        type-name (interface/get-raw-data name-context)]
    {:title (cond-> type-name
              (pos? num-types) (str " (" num-types ")")
              (not type-id) (str " *new*")
              (duplicate? type-name) (str " *duplicate*")
              (deleted? context) (str " *deleted*"))
     :draggable? (not root?)
     :drop-options-fn drag/drop-options
     :drop-fn drag/drop-fn
     :editable-path (:path name-context)
     :buttons (cond-> [{:icon "fas fa-plus"
                        :title :string.button/add
                        :handler #(rf/dispatch [::add
                                                context
                                                {:type :heraldicon/charge-type
                                                 :name "New type"}])}]
                (not root?) (conj {:icon "far fa-edit"
                                   :title :string.button/edit
                                   :handler #(rf/dispatch [::tree/set-edit-node name-context])}
                                  {:icon "far fa-trash-alt"
                                   :remove? true
                                   :title :string.tooltip/remove
                                   :handler #(rf/dispatch [::remove context])}))
     :nodes (sorted-children types-context)}))

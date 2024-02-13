(ns heraldicon.frontend.component.charge-type-group
  (:require
   [clojure.string :as str]
   [heraldicon.context :as c]
   [heraldicon.frontend.component.charge-type :as charge-type]
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.component.tree :as tree]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.interface :as interface]
   [re-frame.core :as rf]))

(macros/reg-event-fx ::add
  (fn [{:keys [db]} [_ {:keys [path]} value]]
    (let [groups-path (conj path :groups)
          elements (-> (get-in db groups-path)
                       (conj value)
                       vec)
          new-element-path (conj groups-path (-> elements count dec))]
      {:db (assoc-in db groups-path elements)
       :dispatch-n [[::submenu/close-all]
                    [::tree/select-node new-element-path true]]})))

(macros/reg-event-fx ::remove
  (fn [{:keys [db]} [_ {:keys [path]
                        :as context}]]
    (let [new-groups (get-in db (conj path :groups))
          new-types (get-in db (conj path :types))
          parent-context (c/-- context 2)
          parent-id (get-in db (:path (c/++ parent-context :id)))
          parent-groups-path (:path (c/++ parent-context :groups))
          parent-types-path (if (= parent-id :root)
                              (let [top-level-groups (get-in db (:path (c/++ parent-context :groups)))
                                    index (first (keep-indexed (fn [idx {:keys [id]}]
                                                                 (when (= id :ungrouped)
                                                                   idx))
                                                               top-level-groups))]
                                (:path (c/++ parent-context :groups index :types)))
                              (:path (c/++ parent-context :types)))
          index (last path)
          parent-types (get-in db parent-types-path)
          new-parent-types (vec (concat parent-types
                                        new-types))
          new-db (assoc-in db parent-types-path new-parent-types)
          parent-groups (get-in new-db parent-groups-path)
          new-parent-groups (vec (concat (subvec parent-groups 0 index)
                                         (subvec parent-groups (inc index))
                                         new-groups))
          new-db (assoc-in new-db parent-groups-path new-parent-groups)]
      {:db new-db
       :dispatch-n [[::submenu/close-all]
                    [::tree/select-node (:path parent-context) true]]})))

(defn- sort-key
  [{:keys [context]}]
  (some-> (interface/get-raw-data (c/++ context :name))
          str/lower-case))

(defn- sorted-children
  [context]
  (let [num-fields (interface/get-list-size context)]
    (->> (range num-fields)
         (map (fn [idx]
                {:context (c/++ context idx)}))
         (sort-by sort-key))))

(defmethod component/node :heraldicon/charge-type-group [context]
  (let [group-id (interface/get-raw-data (c/++ context :id))
        num-types (->> (interface/get-raw-data context)
                       (tree-seq map? :groups)
                       (map :types)
                       (apply concat)
                       count)
        groups-context (c/++ context :groups)
        types-context (c/++ context :types)
        name-context (c/++ context :name)]
    {:title (str (interface/get-raw-data name-context) " (" num-types ")")
     :editable-path (:path name-context)
     :draggable? (not (#{:root
                         :ungrouped}
                       group-id))
     :drop-options-fn charge-type/drop-options-fn
     :drop-fn charge-type/drop-fn
     :nodes (concat (sorted-children groups-context)
                    (sorted-children types-context))
     :buttons (cond-> [{:icon "fas fa-plus"
                        :title :string.button/add
                        :menu [{:title "Charge type group"
                                :handler #(rf/dispatch [::add
                                                        context
                                                        {:type :heraldicon/charge-type-group
                                                         :name "New group"}])}
                               {:title "Charge type"
                                :handler #(rf/dispatch [::charge-type/add
                                                        context
                                                        {:type :heraldicon/charge-type
                                                         :name "New type"}])}]}]
                (not (#{:root
                        :ungrouped}
                      group-id)) (conj {:icon "far fa-edit"
                                        :title :string.button/edit
                                        :handler #(do
                                                    (rf/dispatch [::tree/select-node (:path context)])
                                                    (rf/dispatch [::tree/set-edit-node name-context]))}
                                       {:icon "far fa-trash-alt"
                                        :remove? true
                                        :title :string.tooltip/remove
                                        :handler #(rf/dispatch [::remove context])}))}))

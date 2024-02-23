(ns heraldicon.frontend.component.charge-type
  (:require
   [clojure.string :as str]
   [heraldicon.context :as c]
   [heraldicon.frontend.charge-types :as frontend.charge-types]
   [heraldicon.frontend.component.core :as component]
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

(defn remove-element
  [db path]
  (let [elements-path (vec (drop-last path))
        index (last path)
        value (get-in db path)
        elements (get-in db elements-path)
        new-elements (vec (concat (subvec elements 0 index)
                                  (subvec elements (inc index))))]
    [(assoc-in db elements-path new-elements)
     value]))

(macros/reg-event-fx ::remove
  (fn [{:keys [db]} [_ {:keys [path]
                        :as context}]]
    (let [[new-db value] (remove-element db path)
          children (:types value)
          parent-context (c/-- context 2)
          parent-types-path (:path (c/++ parent-context :types))
          siblings (get-in new-db parent-types-path)
          new-siblings (vec (concat siblings children))]
      {:db (-> new-db
               (assoc-in parent-types-path new-siblings)
               (tree/select-node (:path parent-context) true))})))

(defn add-element
  [db elements-path value]
  (let [elements (get-in db elements-path)
        new-elements (vec (conj elements value))
        index (dec (count new-elements))]
    [(assoc-in db elements-path new-elements)
     (conj elements-path index)]))

(defn adjust-path-after-removal
  [path removed-path]
  ; if the removed path is longer, then its removal can't affected the path
  (if (<= (count removed-path)
          (count path))
    (let [index-pos (dec (count removed-path))
          path-start (take index-pos path)
          removal-index (last removed-path)
          path-index (get path index-pos)]
      (if (and (= path-start (drop-last removed-path))
               (< removal-index path-index))
        (vec (concat path-start [(dec path-index)] (drop (inc index-pos) path)))

        path))

    path))

(macros/reg-event-fx ::move
  (fn [{:keys [db]} [_
                     {value-path :path}
                     {target-path :path}]]
    (let [[new-db value] (remove-element db value-path)
          adjusted-target-path (adjust-path-after-removal target-path value-path)
          [new-db new-value-path] (add-element new-db adjusted-target-path value)]
      {:db (tree/select-node new-db new-value-path true)})))

(defn drop-options-fn
  [dragged-node-path drop-node-path drop-node-open?]
  (when (not= (take (count dragged-node-path) drop-node-path)
              dragged-node-path)
    (let [root? (= drop-node-path frontend.charge-types/form-db-path)
          siblings? (= (drop-last dragged-node-path)
                       (drop-last drop-node-path))
          parent? (= (drop-last 2 dragged-node-path)
                     drop-node-path)]
      (cond-> (cond
                root? (when-not parent?
                        #{:inside})
                parent? #{:above :below}
                siblings? #{:inside}
                :else #{:above :inside :below})
        drop-node-open? (disj :below)))))

(defn drop-fn
  [dragged-node-context drop-node-context where]
  (let [target-context (cond-> drop-node-context
                         (not= where :inside) (c/-- 2))
        target-context (c/++ target-context :types)]
    (rf/dispatch [::move dragged-node-context target-context])))

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
              (duplicate? type-name) (str " *duplicate*"))
     :draggable? (not root?)
     :drop-options-fn drop-options-fn
     :drop-fn drop-fn
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

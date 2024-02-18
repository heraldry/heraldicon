(ns heraldicon.frontend.component.charge-type
  (:require
   [clojure.string :as str]
   [heraldicon.context :as c]
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.component.tree :as tree]
   [heraldicon.frontend.element.submenu :as submenu]
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
      {:db (assoc-in db types-path elements)
       :dispatch-n [[::submenu/close-all]
                    [::tree/select-node new-element-path true]
                    [::tree/set-edit-node {:path (conj new-element-path :name)}]]})))

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
      {:db (assoc-in new-db parent-types-path new-siblings)
       :dispatch-n [[::submenu/close-all]
                    [::tree/select-node (:path parent-context) true]]})))

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
      {:db new-db
       :dispatch-n [[::tree/select-node new-value-path true]]})))

(defn drop-options-fn
  [dragged-node-path dragged-node drop-node-path drop-node drop-node-open?]
  (when (not= (take (count dragged-node-path) drop-node-path)
              dragged-node-path)
    (let [dragged-type (:type dragged-node)
          {drop-type :type
           drop-id :id} drop-node
          root? (= drop-id :root)
          siblings? (= (drop-last dragged-node-path)
                       (drop-last drop-node-path))
          parent? (= (drop-last 2 dragged-node-path)
                     drop-node-path)]
      (when (and (= dragged-type :heraldicon/charge-type)
                 (= drop-type :heraldicon/charge-type))
        (cond-> (cond
                  root? (when-not parent?
                          #{:inside})
                  parent? #{:above :below}
                  siblings? #{:inside}
                  :else #{:above :inside :below})
          drop-node-open? (disj :below))))))

(defn drop-fn
  [dragged-node-context drop-node-context where]
  (let [target-context (cond-> drop-node-context
                         (not= where :inside) (c/-- 2))
        target-context (c/++ target-context :types)]
    (rf/dispatch [::move dragged-node-context target-context])))

(defn- sort-key
  [{:keys [context]}]
  [(if (pos? (count (interface/get-raw-data (c/++ context :types))))
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

(defmethod component/node :heraldicon/charge-type [context]
  (let [type-id (interface/get-raw-data (c/++ context :id))
        name-context (c/++ context :name)
        num-types (->> (interface/get-raw-data context)
                       (tree-seq map? :types)
                       count
                       dec)
        types-context (c/++ context :types)
        root? (= type-id :root)]
    {:title (cond-> (interface/get-raw-data name-context)
              (pos? num-types) (str " (" num-types ")"))
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
                                   :handler #(do
                                               (rf/dispatch [::tree/select-node (:path context)])
                                               (rf/dispatch [::tree/set-edit-node name-context]))}
                                  {:icon "far fa-trash-alt"
                                   :remove? true
                                   :title :string.tooltip/remove
                                   :handler #(rf/dispatch [::remove context])}))
     :nodes (sorted-children types-context)}))

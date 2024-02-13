(ns heraldicon.frontend.component.charge-type
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.component.tree :as tree]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.interface :as interface]
   [re-frame.core :as rf]))

(macros/reg-event-fx ::add
  (fn [{:keys [db]} [_ {:keys [path]} value]]
    (let [groups-path (conj path :types)
          elements (-> (get-in db groups-path)
                       (conj value)
                       vec)
          new-element-path (conj groups-path (-> elements count dec))]
      {:db (assoc-in db groups-path elements)
       :dispatch-n [[::submenu/close-all]
                    [::tree/select-node new-element-path true]]})))

;; TODO: what about charges having that type?
(macros/reg-event-fx ::remove
  (fn [{:keys [db]} [_ {:keys [path]
                        :as context}]]
    (let [parent-context (c/-- context 2)
          parent-types-path (:path (c/++ parent-context :types))
          index (last path)
          parent-types (get-in db parent-types-path)
          new-parent-types (vec (concat (subvec parent-types 0 index)
                                        (subvec parent-types (inc index))))]
      {:db (assoc-in db parent-types-path new-parent-types)
       :dispatch-n [[::submenu/close-all]
                    [::tree/select-node (:path parent-context) true]]})))

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

(rf/reg-event-fx ::move-element
  (fn [{:keys [db]} [_ value-context target-context where]]
    (let [value-type (get-in db (:path (c/++ value-context :type)))
          target-context (cond-> target-context
                           (not= where :inside) (c/-- 2))
          target-context (if (= value-type :heraldicon/charge-type-group)
                           (c/++ target-context :groups)
                           (c/++ target-context :types))]
      {:dispatch-n [[::move value-context target-context]]})))

(defn drop-options-fn
  [dragged-node-path dragged-node drop-node-path drop-node drop-node-parent-id drop-node-open?]
  (when (and (not= (take (count dragged-node-path) drop-node-path)
                   dragged-node-path)
             ; TODO: do this check properly, this must not be allowed with above/below types in that group
             (not (and (= (:type dragged-node) :heraldicon/charge-type-group)
                       (or (= (:id drop-node) :ungrouped)
                           (= drop-node-parent-id :ungrouped)))))
    (let [dragged-type (:type dragged-node)
          {drop-type :type
           drop-id :id} drop-node
          root? (= drop-id :root)
          dropped-group? (= dragged-type :heraldicon/charge-type-group)]
      (cond-> (cond
                (= drop-type :heraldicon/charge-type-group) (cond
                                                              (not root?) #{:above :inside :below}
                                                              dropped-group? #{:inside}
                                                              :else nil)

                (= drop-type :heraldicon/charge-type) #{:above :below})
        drop-node-open? (disj :below)))))

(defn drop-fn
  [dragged-node-context drop-node-context where]
  (rf/dispatch [::move-element dragged-node-context drop-node-context where]))

(defmethod component/node :heraldicon/charge-type [context]
  (let [name-context (c/++ context :name)]
    {:title (interface/get-raw-data (c/++ context :name))
     :draggable? true
     :drop-options-fn drop-options-fn
     :drop-fn drop-fn
     :editable-path (:path name-context)
     :buttons [{:icon "far fa-edit"
                :title :string.button/edit
                :handler #(do
                            (rf/dispatch [::tree/select-node (:path context)])
                            (rf/dispatch [::tree/set-edit-node name-context]))}
               {:icon "far fa-trash-alt"
                :remove? true
                :title :string.tooltip/remove
                :handler #(rf/dispatch [::remove context])}]
     :nodes []}))

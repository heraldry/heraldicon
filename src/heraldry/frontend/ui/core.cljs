(ns heraldry.frontend.ui.core
  (:require [clojure.string :as s]
            [heraldry.frontend.state :as state]
            [re-frame.core :as rf]))

(def node-flag-db-path [:ui :component-tree :nodes])

(defn flag-path [path]
  (conj node-flag-db-path path))

(rf/reg-sub :component-data
  (fn [db [_ path]]
    (let [data (get-in db path)]
      [path (cond-> data
              (and (map? data)
                   (-> data :type not)) (assoc :type (keyword "heraldry.type" (last path))))])))

(defn type->component-type [t]
  (let [ts (str t)]
    (cond
      (s/starts-with? ts ":heraldry.type") t
      (s/starts-with? ts ":heraldry.field") :heraldry.type/field
      (s/starts-with? ts ":heraldry.ordinary") :heraldry.type/ordinary
      (s/starts-with? ts ":heraldry.charge") :heraldry.type/charge
      :else :heraldry.type/unknown)))

(defn component->node [path data & {:keys [title open?]}]
  (let [t (cond
            (map? data) (-> data :type type->component-type)
            (vector? data) :heraldry.type/items
            :else :heraldry.type/unknown)]
    (case t
      :heraldry.type/coat-of-arms {:title "coat of arms"
                                   :open? open?
                                   :nodes [{:path (conj path :field)}]}

      :heraldry.type/render-options {:title "render-options"}

      :heraldry.type/ordinary {:title "ordinary"}

      :heraldry.type/charge {:title "charge"}

      :heraldry.type/field {:title "field"
                            :open? open?
                            :nodes [{:title "components"
                                     :path (conj path :components)}]}

      :heraldry.type/items {:title title
                            :open? open?}

      :heraldry.type/unknown {:title "unknown"})))

(rf/reg-sub :component-node
  (fn [[_ path] _]
    [(rf/subscribe [:component-data path])
     (rf/subscribe [:get (flag-path path)])])

  (fn [[[path component-data] open?]]
    (component->node path component-data :open? open?)))

(defn component-node [path & {:keys [title]}]
  (let [node-data @(rf/subscribe [:component-node path])
        {node-title :title
         open? :open?
         nodes :nodes} node-data
        openable? (-> nodes count pos?)
        title (or node-title title)]
    [:<>
     [:div.node-name
      (cond-> {}
        openable? (->
                   (assoc :class "clickable")
                   (assoc :on-click #(state/dispatch-on-event % [:set (flag-path path) (not open?)]))))
      (when openable?
        [:i.far {:class (if open?
                          "fa-minus-square"
                          "fa-plus-square")}])
      title]
     (when open?
       [:ul
        (for [{node-path :path
               title :title} nodes]
          ^{:key node-path} [:li [component-node node-path :title title]])])]))

(defn component-tree [paths]
  [:div.tree
   [:ul
    (for [[idx node-path] (map-indexed vector paths)]
      ^{:key idx} [:li [component-node node-path]])]])

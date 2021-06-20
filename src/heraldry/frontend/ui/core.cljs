(ns heraldry.frontend.ui.core
  (:require [clojure.string :as s]
            [heraldry.frontend.state :as state]
            [re-frame.core :as rf]))

(def node-flag-db-path [:ui :component-tree :nodes])
(def ui-selected-component-path [:ui :selected-component])

(defn flag-path [path]
  (conj node-flag-db-path path))

(rf/reg-sub :component-data
  (fn [db [_ path]]
    (let [data (get-in db path)
          ;; TODO: this should either not be necessary or be done betterly
          data (if (and (not data)
                        (= (last path) :components))
                 []
                 data)]
      (cond-> data
        (and (map? data)
             (-> data :type not)) (assoc :type (keyword "heraldry.type" (last path)))))))

(defn type->component-type [t]
  (let [ts (str t)]
    (cond
      (s/starts-with? ts ":heraldry.type") t
      (s/starts-with? ts ":heraldry.field") :heraldry.type/field
      (s/starts-with? ts ":heraldry.ordinary") :heraldry.type/ordinary
      (s/starts-with? ts ":heraldry.charge") :heraldry.type/charge
      :else :heraldry.type/unknown)))

(defn component->node [path data & {:keys [title open? selected?]}]
  (let [t (cond
            (map? data) (-> data :type type->component-type)
            (vector? data) :heraldry.type/items
            :else :heraldry.type/unknown)]
    (merge {:open? open?
            :selected? selected?
            :selectable? true}
           (case t
             :heraldry.type/coat-of-arms {:title "coat of arms"
                                          :nodes [{:path (conj path :field)}
                                                  {:title "components"
                                                   :path (conj path :field :components)}]}

             :heraldry.type/render-options {:title "render-options"}

             :heraldry.type/ordinary {:title "ordinary"
                                      :nodes [{:path (conj path :field)}
                                              {:title "components"
                                               :path (conj path :field :components)}]}

             :heraldry.type/charge {:title "charge"
                                    :nodes [{:path (conj path :field)}
                                            {:title "components"
                                             :path (conj path :field :components)}]}

             :heraldry.type/field {:title "field"}

             :heraldry.type/items {:title title
                                   :nodes (->> data
                                               count
                                               range
                                               (map (fn [idx]
                                                      {:path (conj path idx)}))
                                               vec)
                                   :selectable? false}

             :heraldry.type/unknown {:title "unknown"}))))

(rf/reg-sub :component-node
  (fn [[_ path] _]
    [(rf/subscribe [:component-data path])
     (rf/subscribe [:get (flag-path path)])
     (rf/subscribe [:get ui-selected-component-path])])

  (fn [[component-data open? selected-component-path] [_ path]]
    (component->node
     path
     component-data
     :open? open?
     :selected? (= path selected-component-path))))

(defn component-node [path & {:keys [title]}]
  (let [node-data @(rf/subscribe [:component-node path])
        {node-title :title
         open? :open?
         selected? :selected?
         selectable? :selectable?
         nodes :nodes} node-data
        openable? (-> nodes count pos?)
        title (or node-title title)]
    [:<>
     [:div.node-name.clickable.no-select
      {:class (when selected?
                "selected")
       :on-click #(do
                    (when (or (not open?)
                              (not selectable?)
                              selected?)
                      (rf/dispatch [:set (flag-path path) (not open?)]))
                    (when selectable?
                      (rf/dispatch [:set ui-selected-component-path path]))
                    (.stopPropagation %))}
      (when openable?
        [:span.node-icon.clickable
         {:class "clickable"
          :on-click #(state/dispatch-on-event % [:set (flag-path path) (not open?)])}
         [:i.far {:class (if open?
                           "fa-minus-square"
                           "fa-plus-square")}]])
      title]
     (when open?
       [:ul
        (for [{node-path :path
               title :title} nodes]
          ^{:key node-path} [:li [component-node node-path :title title]])])]))

(defn component-tree [paths]
  [:div.tree
   {:style {:border "1px solid #ddd"
            :border-radius "10px"
            :padding "10px"}}
   [:ul
    (for [[idx node-path] (map-indexed vector paths)]
      ^{:key idx} [:li [component-node node-path]])]])

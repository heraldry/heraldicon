(ns heraldry.frontend.ui.core
  (:require [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.form.coat-of-arms] ;; needed for defmethods
            [heraldry.frontend.ui.form.render-options] ;; needed for defmethods
            [heraldry.frontend.ui.interface :as interface]
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

(rf/reg-sub :component-node
  (fn [[_ path] _]
    [(rf/subscribe [:component-data path])
     (rf/subscribe [:get (flag-path path)])
     (rf/subscribe [:get ui-selected-component-path])])

  (fn [[component-data open? selected-component-path] [_ path]]
    (merge {:open? open?
            :selected? (= path selected-component-path)
            :selectable? true}
           (interface/component-node-data path component-data))))

(rf/reg-sub :component-form
  (fn [[_ path] _]
    [(rf/subscribe [:component-node path])
     (rf/subscribe [:component-data path])])

  (fn [[{:keys [title]} component-data] [_ path]]
    (merge
     {:title title
      :path path}
     (interface/component-form-data component-data))))

(defmethod interface/component-node-data :heraldry.type/ordinary [path _component-data]
  {:title "ordinary"
   :nodes [{:path (conj path :field)}
           {:title "components"
            :path (conj path :field :components)}]})

(defmethod interface/component-node-data :heraldry.type/charge [path _component-data]
  {:title "charge"
   :nodes [{:path (conj path :field)}
           {:title "components"
            :path (conj path :field :components)}]})

(defmethod interface/component-node-data :heraldry.type/field [_path _component-data]
  {:title "field"})

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
  [:div.ui-tree
   {:style {:border "1px solid #ddd"
            :border-radius "10px"
            :padding "10px"}}
   [:ul
    (for [[idx node-path] (map-indexed vector paths)]
      ^{:key idx} [:li [component-node node-path]])]])

(defn component-form [path]
  (let [{:keys [title path form form-args]} (when path
                                              @(rf/subscribe [:component-form path]))]
    [:div.ui-component
     [:div.header
      [:h1 title]]
     [:div.content {:style {:height "30vh"}}
      (when form
        [form path form-args])]]))

(defn selected-component []
  (let [selected-component-path @(rf/subscribe [:get ui-selected-component-path])]
    [component-form selected-component-path]))

(ns heraldicon.frontend.component.tree
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.core :as frontend.component]
   [heraldicon.frontend.element.hover-menu :as hover-menu]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.validation :as validation]
   [re-frame.core :as rf]))

(def node-context
  {:render-options {:type :heraldry/render-options}
   :render-options-path [:context :render-options]})

(defn node-data [{:keys [path] :as context}]
  (merge {:open? @(rf/subscribe [:ui-component-node-open? path])
          :selected? (= path @(rf/subscribe [:ui-component-node-selected-path]))
          :selectable? true}
         (frontend.component/node context)))

(defn node [{:keys [path] :as context} & {:keys [title parent-buttons]}]
  (let [{node-title :title
         :keys [open?
                selected?
                selectable?
                nodes
                buttons
                annotation
                validation
                icon]} (node-data context)
        openable? (-> nodes count pos?)
        title (or node-title title)
        buttons (concat buttons parent-buttons)]
    [:<>
     [:div.node-name.clickable.no-select
      {:class (when selected?
                "selected")
       :on-click #(do
                    (when (or (not open?)
                              (not selectable?)
                              selected?)
                      (rf/dispatch [:ui-component-node-toggle path]))
                    (when selectable?
                      (rf/dispatch [:ui-component-node-select path]))
                    (.stopPropagation %))
       :style {:color (when-not selectable?
                        "#000")}}
      (if openable?
        [:span.node-icon.clickable
         {:on-click #(state/dispatch-on-event % [:ui-component-node-toggle path])
          :style {:width "0.9em"}}
         [:i.fa.ui-icon {:class (if open?
                                  "fa-angle-down"
                                  "fa-angle-right")}]]
        [:span.node-icon
         [:i.fa.ui-icon.fa-angle-down {:style {:opacity 0}}]])

      (when icon
        (let [effective-icon (if selected?
                               (:selected icon)
                               (:default icon))
              icon-style {:width "14px"
                          :height "16px"
                          :margin-right "5px"
                          :vertical-align "top"
                          :transform "translate(0,3px)"}]
          (if (vector? effective-icon)
            (update-in effective-icon [1 :style] merge icon-style)
            [:img {:src effective-icon
                   :style icon-style}])))

      [tr title]

      annotation

      (validation/render validation)

      (into [:<>]
            (comp (filter :menu)
                  (map-indexed (fn [idx {:keys [icon menu disabled? title]}]
                                 [hover-menu/hover-menu
                                  (c/++ context idx)
                                  title
                                  menu
                                  [:i.ui-icon {:class icon
                                               :title (tr title)
                                               :style {:margin-left "0.5em"
                                                       :font-size "0.8em"
                                                       :color (if disabled?
                                                                "#ccc"
                                                                "#777")
                                                       :cursor (when disabled? "not-allowed")}}]
                                  :disabled? disabled?
                                  :require-click? true])))
            buttons)

      (into [:span {:style {:margin-left "0.5em"}}]
            (comp (remove :menu)
                  (map-indexed (fn [idx {:keys [icon handler disabled? title remove?]}]
                                 [:span.node-icon
                                  {:class (when disabled? "disabled")
                                   :title (tr title)
                                   :style {:margin-left (when (and (pos? idx)
                                                                   remove?) "0.5em")}}
                                  [:i.ui-icon {:class icon
                                               :on-click (when-not disabled? handler)
                                               :style {:font-size "0.8em"
                                                       :color (if disabled?
                                                                "#ccc"
                                                                "#777")
                                                       :cursor (when disabled? "not-allowed")}}]])))
            buttons)]

     (when open?
       (into [:ul]
             (map (fn [{:keys [context title buttons]}]
                    ^{:key context}
                    [:li [node context :title title :parent-buttons buttons]]))
             nodes))]))

(defn tree [paths]
  [:div.ui-tree
   (into [:ul]
         (map-indexed (fn [idx node-path]
                        ^{:key idx}
                        [:li
                         (if (= node-path :spacer)
                           [:div {:style {:height "1em"}}]
                           [node (c/<< node-context :path node-path)])]))
         paths)])

(ns heraldicon.frontend.element.hover-menu
  (:require
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [re-frame.core :as rf]))

(rf/reg-sub :ui-hover-menu-open?
  (fn [db [_ path]]
    (get-in db [:ui :hover-menu-open? path])))

(macros/reg-event-db :ui-hover-menu-open
  (fn [db [_ path]]
    (assoc-in db [:ui :hover-menu-open? path] true)))

(macros/reg-event-db :ui-hover-menu-close
  (fn [db [_ path]]
    (update-in db [:ui :hover-menu-open?] dissoc path)))

(defn hover-menu [{:keys [path]} title menu trigger-element & {:keys [disabled?
                                                                      require-click?]}]
  (let [menu-open? @(rf/subscribe [:ui-hover-menu-open? path])
        menu-disabled? disabled?]
    [:span.node-icon.ui-hover-menu
     {:class (when disabled? "disabled")
      (if require-click?
        :on-click
        :on-mouse-enter) (when-not disabled?
                           (fn [event]
                             (rf/dispatch [:ui-hover-menu-open path])
                             (.preventDefault event)
                             (.stopPropagation event)))
      :on-mouse-leave (when-not disabled?
                        (fn [event]
                          (rf/dispatch [:ui-hover-menu-close path])
                          (.preventDefault event)
                          (.stopPropagation event)))}
     trigger-element
     [:ul.ui-menu {:style {:padding 0
                           :font-weight 400
                           :display (if menu-open?
                                      "block"
                                      "none")
                           :min-width "7em"}}
      [:li.ui-menu-header [tr title]]
      (into [:<>]
            (map (fn [{:keys [icon title handler tooltip disabled?]}]
                   (let [handler (when handler
                                   #(do
                                      (rf/dispatch [:ui-hover-menu-close path])
                                      (.preventDefault %)
                                      (.stopPropagation %)
                                      (handler %)))
                         color (if (or menu-disabled? disabled?)
                                 "#ccc"
                                 "#777")]
                     ^{:key title}
                     [:li.ui-menu-item
                      {:style {:color color}
                       :on-click (when-not (or menu-disabled? disabled?) handler)
                       :title (tr tooltip)}
                      (when icon
                        [:i.ui-icon {:class icon
                                     :style {:margin-right "5px"
                                             :color color}}])
                      [tr title]])))
            menu)]]))

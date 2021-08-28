(ns heraldry.frontend.ui.element.hover-menu
  (:require [heraldry.frontend.macros :as macros]
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

(defn hover-menu [path title menu trigger-element & {:keys [disabled?
                                                            require-click?]}]
  (let [menu-open? @(rf/subscribe [:ui-hover-menu-open? path])]
    [:span.node-icon.ui-hover-menu
     {:class (when disabled? "disabled")
      (if require-click?
        :on-click
        :on-mouse-enter) (when-not disabled?
                           (fn [event]
                             (rf/dispatch [:ui-hover-menu-open path])
                             (.stopPropagation event)))
      :on-mouse-leave (when-not disabled?
                        (fn [event]
                          (rf/dispatch [:ui-hover-menu-close path])
                          (.stopPropagation event)))}
     trigger-element
     [:ul.ui-menu {:style {:padding 0
                           :font-weight 400
                           :display (if menu-open?
                                      "block"
                                      "none")
                           :min-width "7em"}}
      [:li.ui-menu-header title]
      (for [{:keys [icon title handler]} menu]
        (let [handler (when handler
                        #(do
                           (rf/dispatch [:ui-hover-menu-close path])
                           (handler %)))]
          ^{:key title}
          [:li.ui-menu-item
           {:style {:color "#000"}
            :on-click (when-not disabled? handler)}
           (when icon
             [:i.ui-icon {:class icon
                          :style {:margin-right "5px"
                                  :color (if disabled?
                                           "#ccc"
                                           "#777")}}])
           title]))]]))

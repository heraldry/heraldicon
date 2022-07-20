(ns heraldicon.frontend.element.hover-menu
  (:require
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [re-frame.core :as rf]))

(rf/reg-sub ::open?
  (fn [db [_ path]]
    (get-in db [:ui :hover-menu-open? path])))

(macros/reg-event-db ::open
  (fn [db [_ path]]
    (assoc-in db [:ui :hover-menu-open? path] true)))

(macros/reg-event-db ::close
  (fn [db [_ path]]
    (update-in db [:ui :hover-menu-open?] dissoc path)))

(defn hover-menu [{:keys [path]} title menu trigger-element & {:keys [disabled?
                                                                      require-click?]}]
  (let [menu-open? @(rf/subscribe [::open? path])
        menu-disabled? disabled?]
    [:span.node-icon.ui-hover-menu
     {:class (when disabled? "disabled")
      (if require-click?
        :on-click
        :on-mouse-enter) (when-not disabled?
                           (fn [event]
                             (rf/dispatch [::open path])
                             (.preventDefault event)
                             (.stopPropagation event)))
      :on-mouse-leave (when-not disabled?
                        (fn [event]
                          (rf/dispatch [::close path])
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
                                      (rf/dispatch [::close path])
                                      (.preventDefault %)
                                      (.stopPropagation %)
                                      (handler %)))
                         disabled (when (or menu-disabled? disabled?)
                                    "disabled-item")]
                     ^{:key title}
                     [:li.ui-menu-item
                      {:class disabled
                       :on-click (when-not (or menu-disabled? disabled?) handler)
                       :title (tr tooltip)
                       :style {:cursor (when disabled? "not-allowed")}}
                      (when icon
                        [:i.ui-icon {:class (str icon " " disabled)
                                     :style {:margin-right "5px"}}])
                      [tr title]])))
            menu)]]))

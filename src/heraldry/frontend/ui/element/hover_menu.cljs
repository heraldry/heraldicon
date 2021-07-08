(ns heraldry.frontend.ui.element.hover-menu
  (:require [re-frame.core :as rf]))

(defn hover-menu [path title menu trigger-element & {:keys [disabled?]}]
  (let [menu-open? @(rf/subscribe [:ui-hover-menu-open? path])]
    [:span.node-icon.ui-hover-menu
     {:class (when disabled? "disabled")
      :on-mouse-enter (when-not disabled?
                        (fn [event]
                          (when-not menu-open?
                            (rf/dispatch [:ui-hover-menu-open path]))
                          (.stopPropagation event)))}
     trigger-element
     [:ul.ui-menu {:style {:padding 0
                           :font-weight 400
                           :display (if menu-open?
                                      "block"
                                      "none")
                           :min-width "7em"}
                   :on-mouse-leave (fn [event]
                                     (when menu-open?
                                       (rf/dispatch [:ui-hover-menu-close path]))
                                     (.stopPropagation event))}
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

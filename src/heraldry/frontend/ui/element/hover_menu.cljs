(ns heraldry.frontend.ui.element.hover-menu
  (:require [re-frame.core :as rf]))

(defn hover-menu [path title menu trigger-element & {:keys [disabled?]}]
  [:span.node-icon.ui-hover-menu
   {:class (when disabled? "disabled")
    :on-mouse-over #(rf/dispatch [:ui-hover-menu-open path])
    :on-mouse-out #(rf/dispatch [:ui-hover-menu-close path])}
   trigger-element
   [:ul.ui-menu {:style {:padding 0
                         :font-weight 400
                         :display (if @(rf/subscribe [:ui-hover-menu-open? path])
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
         title]))]])

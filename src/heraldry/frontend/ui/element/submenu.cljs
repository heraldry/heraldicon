(ns heraldry.frontend.ui.element.submenu
  (:require [heraldry.frontend.state :as state]
            [re-frame.core :as rf]))

(defn submenu [path title link-name styles & content]
  (let [submenu-id (conj path title)
        submenu-open? @(rf/subscribe [:ui-submenu-open? submenu-id])]
    [:div.ui-submenu-setting {:style {:display "inline-block"}
                              :on-click #(.stopPropagation %)}
     [:a {:on-click #(state/dispatch-on-event % [:ui-submenu-open submenu-id])}
      link-name]
     (when submenu-open?
       [:div.ui-component.ui-submenu {:style styles}
        [:div.header [:a {:on-click #(state/dispatch-on-event % [:ui-submenu-close submenu-id])}
                      [:i.far.fa-times-circle]]
         " " title]
        (into [:div.content]
              content)])]))

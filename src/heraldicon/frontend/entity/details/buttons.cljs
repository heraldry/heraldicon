(ns heraldicon.frontend.entity.details.buttons
  (:require
   [heraldicon.frontend.entity.action.copy-to-new :as action.copy-to-new]
   [heraldicon.frontend.entity.action.save :as action.save]
   [heraldicon.frontend.entity.action.share :as action.share]
   [heraldicon.frontend.ui.element.hover-menu :as hover-menu]))

(defn buttons [form-id]
  [:<>
   [:div.buttons {:style {:display "flex"}}
    [:div {:style {:flex "auto"}}]
    [action.share/button form-id]

    [hover-menu/hover-menu
     {:path [:arms-form-action-menu]}
     :string.button/actions
     [(action.share/action form-id)
      (action.copy-to-new/action form-id)]
     [:button.button {:style {:flex "initial"
                              :color "#777"
                              :margin-left "10px"}}
      [:i.fas.fa-ellipsis-h]]]

    [action.save/button form-id]]])

(ns heraldicon.frontend.entity.details.buttons
  (:require
   [heraldicon.frontend.entity.action.copy-to-new :as copy-to-new]
   [heraldicon.frontend.entity.action.export-png :as export-png]
   [heraldicon.frontend.entity.action.export-svg :as export-svg]
   [heraldicon.frontend.entity.action.save :as save]
   [heraldicon.frontend.entity.action.share :as share]
   [heraldicon.frontend.ui.element.hover-menu :as hover-menu]))

(defn buttons [form-id additional-buttons]
  [:<>
   [:div.buttons {:style {:display "flex"}}
    additional-buttons

    [:div {:style {:flex "auto"}}]
    [share/button form-id]

    [hover-menu/hover-menu
     {:path [:arms-form-action-menu]}
     :string.button/actions
     (filter identity
             [(export-svg/action form-id)
              (export-png/action form-id)
              (share/action form-id)
              (copy-to-new/action form-id)])
     [:button.button {:style {:flex "initial"
                              :color "#777"
                              :margin-left "10px"}}
      [:i.fas.fa-ellipsis-h]]]

    [save/button form-id]]])

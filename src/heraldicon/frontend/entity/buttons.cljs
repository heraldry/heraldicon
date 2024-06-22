(ns heraldicon.frontend.entity.buttons
  (:require
   [heraldicon.frontend.element.hover-menu :as hover-menu]
   [heraldicon.frontend.entity.action.copy-to-new :as copy-to-new]
   [heraldicon.frontend.entity.action.export-png :as export-png]
   [heraldicon.frontend.entity.action.export-svg :as export-svg]
   [heraldicon.frontend.entity.action.export-svg-clips :as export-svg-clips]
   [heraldicon.frontend.entity.action.save :as save]
   [heraldicon.frontend.entity.action.share :as share]))

(defn buttons [entity-type additional-buttons]
  [:<>
   [:div.buttons {:style {:display "flex"
                          :height "2.25em"}}
    additional-buttons

    [:div {:style {:flex "auto"}}]
    [share/button entity-type]

    [hover-menu/hover-menu
     {:path [:arms-form-action-menu]}
     :string.button/actions
     (filter identity
             [(export-svg/action entity-type)
              (export-svg-clips/action entity-type)
              (export-png/action entity-type)
              (share/action entity-type)
              (copy-to-new/action entity-type)])
     [:button.button {:style {:flex "initial"
                              :color "#777"
                              :margin-left "10px"}}
      [:i.fas.fa-ellipsis-h]]
     :require-click? true]

    [save/button entity-type]]])

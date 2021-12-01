(ns heraldry.frontend.ui.element.arms-reference-select
  (:require
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.ui.element.arms-select :as arms-select]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]
   [re-frame.core :as rf]))

(defn link-to-arms [context]
  (fn [arms]
    [:a {:on-click #(rf/dispatch [:set context {:id (:id arms)
                                                :version 0}])}
     (:name arms)]))

(defn arms-reference-select [context]
  (when-let [option (interface/get-relevant-options context)]
    (let [{arms-id :id
           version :version} (interface/get-raw-data context)
          {:keys [ui]} option
          label (:label ui)
          [_status arms-data] (when arms-id
                                (state/async-fetch-data
                                 [:arms-references arms-id version]
                                 [arms-id version]
                                 #(arms-select/fetch-arms arms-id version nil)))
          arms-title (-> arms-data
                         :name
                         (or (string "None")))]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context (string "Select Arms") [tr arms-title] nil
         [arms-select/list-arms (link-to-arms context)]]]])))

(defmethod ui-interface/form-element :arms-reference-select [context]
  [arms-reference-select context])

(ns heraldicon.frontend.ui.element.arms-reference-select
  (:require
   [heraldicon.entity.id :as id]
   [heraldicon.frontend.api :as api]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.ui.element.arms-select :as arms-select]
   [heraldicon.frontend.ui.element.submenu :as submenu]
   [heraldicon.frontend.ui.interface :as ui.interface]
   [heraldicon.interface :as interface]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]))

(defmethod ui.interface/form-element :arms-reference-select [context]
  (when-let [option (interface/get-relevant-options context)]
    (let [{arms-id :id
           version :version} (interface/get-raw-data context)
          {:keys [ui]} option
          label (:label ui)
          [_status arms-data] (when arms-id
                                (state/async-fetch-data
                                 [:arms-references arms-id version]
                                 [arms-id version]
                                 #(api/fetch-arms arms-id version nil)))
          arms-title (-> arms-data
                         :name
                         (or :string.miscellaneous/none))]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context :string.option/select-arms
         [tr arms-title]
         {:style {:position "fixed"
                  :transform "none"
                  :left "45vw"
                  :width "53vw"
                  :top "10vh"
                  :height "80vh"}}
         [arms-select/list-arms (fn [{:keys [id]}]
                                  {:href (reife/href :view-arms-by-id {:id (id/for-url id)})
                                   :on-click (fn [event]
                                               (doto event
                                                 .preventDefault
                                                 .stopPropagation)
                                               (rf/dispatch [:set context {:id id
                                                                           :version 0}]))})
          :selected-arms arms-data
          :display-selected-item? true]]]])))

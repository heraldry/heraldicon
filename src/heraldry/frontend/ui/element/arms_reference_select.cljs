(ns heraldry.frontend.ui.element.arms-reference-select
  (:require
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.ui.element.arms-select :as arms-select]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.interface :as interface]
   [heraldry.util :as util]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]))

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
                                  {:href (reife/href :view-arms-by-id {:id (util/id-for-url id)})
                                   :on-click (fn [event]
                                               (doto event
                                                 .preventDefault
                                                 .stopPropagation)
                                               (rf/dispatch [:set context {:id id
                                                                           :version 0}]))})
          :selected-arms arms-data]]]])))

(defmethod ui-interface/form-element :arms-reference-select [context]
  [arms-reference-select context])

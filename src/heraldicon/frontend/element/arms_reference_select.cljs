(ns heraldicon.frontend.element.arms-reference-select
  (:require
   [heraldicon.entity.id :as id]
   [heraldicon.frontend.element.arms-select :as arms-select]
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.repository.entity-for-rendering :as entity-for-rendering]
   [heraldicon.frontend.tooltip :as tooltip]
   [heraldicon.interface :as interface]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]))

(defn form [context & {:keys [title on-select display-selected-item? tooltip]
                       :or {display-selected-item? true}}]
  (let [{arms-id :id
         version :version} (interface/get-raw-data context)
        {:ui/keys [label]} (interface/get-options context)
        {:keys [_status entity]} (when arms-id
                                   @(rf/subscribe [::entity-for-rendering/data arms-id version]))
        arms-title (or title
                       (-> entity
                           :name
                           (or :string.miscellaneous/none)))]
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
                                {:href (reife/href :route.arms.details/by-id {:id (id/for-url id)})
                                 :on-click (fn [event]
                                             (doto event
                                               .preventDefault
                                               .stopPropagation)
                                             (if on-select
                                               (on-select (:path context) {:id id
                                                                           :version 0})
                                               (rf/dispatch [:set context {:id id
                                                                           :version 0}])))})
        :selected-item entity
        :display-selected-item? display-selected-item?
        :list-id :arms-reference-select]]]
     [tooltip/info tooltip]]))

(defmethod element/element :ui.element/arms-reference-select [context]
  (form context))

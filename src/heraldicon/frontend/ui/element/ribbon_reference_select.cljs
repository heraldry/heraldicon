(ns heraldicon.frontend.ui.element.ribbon-reference-select
  (:require
   [heraldicon.entity.id :as id]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.preview :as preview]
   [heraldicon.frontend.repository.entity :as entity]
   [heraldicon.frontend.ui.element.ribbon-select :as ribbon-select]
   [heraldicon.frontend.ui.element.submenu :as submenu]
   [heraldicon.frontend.component.entity.ribbon.data :as ribbon.data]
   [heraldicon.frontend.ui.interface :as ui.interface]
   [heraldicon.interface :as interface]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]))

(macros/reg-event-db :set-ribbon-data
  (fn [db [_ path ribbon-data]]
    (let [previous-segments (get-in db (conj path :segments))]
      (-> db
          (assoc-in path ribbon-data)
          (update-in (conj path :segments) (fn [segments]
                                             (ribbon.data/restore-previous-text-segments
                                              segments
                                              previous-segments
                                              [:text
                                               :font])))))))

(macros/reg-event-db :set-ribbon-reference
  (fn [db [_ path ribbon]]
    (let [{ribbon-id :id
           ribbon-version :version} ribbon
          parent-path (-> path drop-last vec)
          ;; TODO: this sets the data either right away when the result status is :done,
          ;; or inside the on-loaded call
          ;; this is a bit hacky still, also because it dispatches inside the event,
          ;; and there's a race condition, because the ::entity/data below also fetches
          ;; the ribbon with a different subscription, resulting in two requests being
          ;; sent to the API
          on-ribbon-load #(rf/dispatch [:set-ribbon-data
                                        (conj parent-path :ribbon)
                                        (-> % :data :ribbon)])
          {:keys [status entity]} @(rf/subscribe [::entity/data ribbon-id ribbon-version on-ribbon-load])]
      (when (= status :done)
        (on-ribbon-load entity))
      (assoc-in db path {:id ribbon-id
                         :version ribbon-version}))))

(defn- choice-preview [context]
  (let [ribbon (interface/get-raw-data context)
        img-url (preview/preview-url
                 :ribbon ribbon
                 :width 64
                 :height 72)]
    [:div {:style {:width "4em"
                   :height "4.5em"
                   :border "1.5px solid #ddd"}}
     (when ribbon
       [:img.clickable {:src img-url}])]))

(defmethod ui.interface/form-element :ribbon-reference-select [context]
  (when-let [option (interface/get-relevant-options context)]
    (let [{ribbon-id :id
           version :version} (interface/get-raw-data context)
          {:keys [ui]} option
          label (:label ui)
          {:keys [_status entity]} @(rf/subscribe [::entity/data ribbon-id version])
          ribbon-title (-> entity
                           :name
                           (or :string.miscellaneous/none))]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context :string.option/select-ribbon
         [:div
          [tr ribbon-title]
          [choice-preview context]]
         {:style {:position "fixed"
                  :transform "none"
                  :left "45vw"
                  :width "53vw"
                  :top "10vh"
                  :height "80vh"}}
         [ribbon-select/list-ribbons
          (fn [ribbon]
            {:href (reife/href :route.ribbon.details/by-id {:id (id/for-url (:id ribbon))})
             :on-click (fn [event]
                         (doto event
                           .preventDefault
                           .stopPropagation)
                         (rf/dispatch [:set-ribbon-reference (:path context) ribbon]))})
          :selected-ribbon entity
          :display-selected-item? true]]]])))

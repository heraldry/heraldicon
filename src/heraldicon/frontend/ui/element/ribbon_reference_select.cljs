(ns heraldicon.frontend.ui.element.ribbon-reference-select
  (:require
   [com.wsscode.async.async-cljs :refer [<? go-catch]]
   [heraldicon.entity.id :as id]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.preview :as preview]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.ui.element.ribbon-select :as ribbon-select]
   [heraldicon.frontend.ui.element.submenu :as submenu]
   [heraldicon.frontend.ui.form.entity.ribbon.data :as ribbon.data]
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
          ;; TODO: this is a hacky way to do it, and it also means the API call is done twice,
          ;; here and again via the unrelated the async + cache mechanism when the reference is used
          _ (go-catch
             (let [ribbon-data (<? (ribbon-select/fetch-ribbon ribbon-id ribbon-version nil))]
               (rf/dispatch [:set-ribbon-data
                             (conj parent-path :ribbon)
                             (-> ribbon-data :data :ribbon)])))]
      (-> db
          (assoc-in path {:id ribbon-id
                          :version ribbon-version})))))

(defn choice-preview [context]
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

(defn ribbon-reference-select [context]
  (when-let [option (interface/get-relevant-options context)]
    (let [{ribbon-id :id
           version :version} (interface/get-raw-data context)
          {:keys [ui]} option
          label (:label ui)
          [_status ribbon-data] (when ribbon-id
                                  (state/async-fetch-data
                                   [:ribbon-references ribbon-id version]
                                   [ribbon-id version]
                                   #(ribbon-select/fetch-ribbon ribbon-id version nil)))
          ribbon-title (-> ribbon-data
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
            {:href (reife/href :view-ribbon-by-id {:id (id/for-url (:id ribbon))})
             :on-click (fn [event]
                         (doto event
                           .preventDefault
                           .stopPropagation)
                         (rf/dispatch [:set-ribbon-reference (:path context) ribbon]))})
          :selected-ribbon ribbon-data
          :display-selected-item? true]]]])))

(defmethod ui.interface/form-element :ribbon-reference-select [context]
  [ribbon-reference-select context])

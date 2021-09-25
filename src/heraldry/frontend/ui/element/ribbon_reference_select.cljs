(ns heraldry.frontend.ui.element.ribbon-reference-select
  (:require [com.wsscode.common.async-cljs :refer [<? go-catch]]
            [heraldry.frontend.language :refer [tr]]
            [heraldry.frontend.macros :as macros]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.ribbon-select :as ribbon-select]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.form.ribbon-general :as ribbon-general]
            [heraldry.frontend.ui.interface :as interface]
            [re-frame.core :as rf]))

(macros/reg-event-db :set-ribbon-data
  (fn [db [_ path ribbon-data]]
    (let [previous-segments (get-in db (conj path :segments))]
      (-> db
          (assoc-in path ribbon-data)
          (update-in (conj path :segments) (fn [segments]
                                             (ribbon-general/restore-previous-text-segments
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
                             (-> ribbon-data :ribbon)])))]
      (-> db
          (assoc-in path {:id ribbon-id
                          :version ribbon-version})))))

(defn link-to-ribbon [path]
  (fn [ribbon]
    [:a {:on-click #(rf/dispatch [:set-ribbon-reference path ribbon])}
     (:name ribbon)]))

(defn ribbon-reference-select [path]
  (when-let [option @(rf/subscribe [:get-relevant-options path])]
    (let [{ribbon-id :id
           version :version} @(rf/subscribe [:get-value path])
          {:keys [ui]} option
          label (:label ui)
          [_status ribbon-data] (when ribbon-id
                                  (state/async-fetch-data
                                   [:ribbon-references ribbon-id version]
                                   [ribbon-id version]
                                   #(ribbon-select/fetch-ribbon ribbon-id version nil)))
          ribbon-title (-> ribbon-data
                           :name
                           (or "None"))]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu path {:en "Select Ribbon"
                               :de "Band auswählen"} ribbon-title nil
         [ribbon-select/list-ribbon (link-to-ribbon path)]]]])))

(defmethod interface/form-element :ribbon-reference-select [path]
  [ribbon-reference-select path])

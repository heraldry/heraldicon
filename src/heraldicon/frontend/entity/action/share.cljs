(ns heraldicon.frontend.entity.action.share
  (:require
   ["copy-to-clipboard" :as copy-to-clipboard]
   [heraldicon.entity.arms :as entity.arms]
   [heraldicon.entity.attribution :as entity.attribution]
   [heraldicon.frontend.entity.core :as entity]
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.message :as message]
   [re-frame.core :as rf]))

(defn- generate-url [form-id]
  (let [form-db-path (form/data-path form-id)]
    (case form-id
      :heraldicon.entity/arms (entity.arms/short-url @(rf/subscribe [:get form-db-path]))
      :heraldicon.entity/charge (entity.attribution/full-url-for-charge {:path form-db-path})
      :heraldicon.entity/ribbon (entity.attribution/full-url-for-ribbon {:path form-db-path})
      :heraldicon.entity/collection (entity.attribution/full-url-for-collection {:path form-db-path}))))

(defn- invoke [form-id]
  (let [share-url (generate-url form-id)]
    (rf/dispatch-sync [::message/clear form-id])
    (if (copy-to-clipboard share-url)
      (rf/dispatch-sync [::message/set-success form-id :string.user.message/copied-url-for-sharing])
      (rf/dispatch-sync [::message/set-error form-id :string.user.message/copy-to-clipboard-failed]))))

(defn action [form-id]
  (let [form-db-path (form/data-path form-id)
        can-share? (and @(rf/subscribe [::entity/public? form-db-path])
                        @(rf/subscribe [::entity/saved? form-db-path])
                        (not @(rf/subscribe [::form/unsaved-changes? form-id])))]
    {:title :string.button/share
     :icon "fas fa-share-alt"
     :handler (when can-share?
                (partial invoke form-id))
     :disabled? (not can-share?)
     :tooltip (when-not can-share?
                :string.user.message/arms-need-to-be-public-and-saved-for-sharing)}))

(defn button [form-id]
  (let [{:keys [handler
                disabled?
                tooltip]} (action form-id)]
    [:button.button {:style {:flex "initial"
                             :color "#777"}
                     :class (when disabled? "disabled")
                     :title tooltip
                     :on-click handler}
     [:i.fas.fa-share-alt]]))

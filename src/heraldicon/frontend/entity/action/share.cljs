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

(defn- generate-url [entity-type]
  (let [form-db-path (form/data-path entity-type)]
    (case entity-type
      :heraldicon.entity.type/arms (entity.arms/short-url @(rf/subscribe [:get form-db-path]))
      :heraldicon.entity.type/charge (entity.attribution/full-url-for-charge {:path form-db-path})
      :heraldicon.entity.type/ribbon (entity.attribution/full-url-for-ribbon {:path form-db-path})
      :heraldicon.entity.type/collection (entity.attribution/full-url-for-collection {:path form-db-path}))))

(defn- invoke [entity-type]
  (let [share-url (generate-url entity-type)]
    (rf/dispatch-sync [::message/clear entity-type])
    (if (copy-to-clipboard share-url)
      (rf/dispatch-sync [::message/set-success entity-type :string.user.message/copied-url-for-sharing])
      (rf/dispatch-sync [::message/set-error entity-type :string.user.message/copy-to-clipboard-failed]))))

(defn action [entity-type]
  (let [form-db-path (form/data-path entity-type)
        can-share? (and @(rf/subscribe [::entity/public? form-db-path])
                        @(rf/subscribe [::entity/saved? form-db-path])
                        (not @(rf/subscribe [::form/unsaved-changes? entity-type])))]
    {:title :string.button/share
     :icon "fas fa-share-alt"
     :handler (when can-share?
                (partial invoke entity-type))
     :disabled? (not can-share?)
     :tooltip (when-not can-share?
                :string.user.message/arms-need-to-be-public-and-saved-for-sharing)}))

(defn button [entity-type]
  (let [{:keys [handler
                disabled?
                tooltip]} (action entity-type)]
    [:button.button {:style {:flex "initial"
                             :color "#777"}
                     :class (when disabled? "disabled")
                     :title (tr tooltip)
                     :on-click handler}
     [:i.fas.fa-share-alt]]))

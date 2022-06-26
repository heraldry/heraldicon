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

(defn- generate-url [db entity-type]
  (let [form-db-path (form/data-path entity-type)
        context {:path (into [:context :db] form-db-path)
                 :db db}]
    (case entity-type
      :heraldicon.entity.type/arms (entity.arms/short-url (get-in db form-db-path))
      :heraldicon.entity.type/charge (entity.attribution/full-url-for-charge context)
      :heraldicon.entity.type/ribbon (entity.attribution/full-url-for-ribbon context)
      :heraldicon.entity.type/collection (entity.attribution/full-url-for-collection context))))

(rf/reg-event-fx ::invoke
  (fn [{:keys [db]} [_ entity-type]]
    (let [share-url (generate-url db entity-type)]
      {:dispatch (if (copy-to-clipboard share-url)
                   [::message/set-success entity-type :string.user.message/copied-url-for-sharing]
                   [::message/set-error entity-type :string.user.message/copy-to-clipboard-failed])})))

(defn action [entity-type]
  (let [form-db-path (form/data-path entity-type)
        can-share? (and @(rf/subscribe [::entity/public? form-db-path])
                        @(rf/subscribe [::entity/saved? form-db-path])
                        (not @(rf/subscribe [::form/unsaved-changes? entity-type])))]
    {:title :string.button/share
     :icon "fas fa-share-alt"
     :handler (when can-share?
                #(rf/dispatch [::invoke entity-type]))
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

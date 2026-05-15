(ns heraldicon.frontend.entity.action.delete
  (:require
   [com.wsscode.async.async-cljs :refer [<? go-catch]]
   [heraldicon.frontend.api :as api]
   [heraldicon.frontend.entity.core :as entity]
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.repository.entity :as repository.entity]
   [heraldicon.frontend.repository.entity-for-editing :as entity-for-editing]
   [heraldicon.frontend.repository.entity-for-rendering :as entity-for-rendering]
   [heraldicon.frontend.repository.entity-list :as entity-list]
   [heraldicon.frontend.repository.entity-search :as entity-search]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]
   [taoensso.timbre :as log]))

(defn- supports-delete? [entity-type]
  (contains? #{:heraldicon.entity.type/collection
               :heraldicon.entity.type/arms}
             entity-type))

(defn- list-route [entity-type]
  (case entity-type
    :heraldicon.entity.type/arms :route.arms/list
    :heraldicon.entity.type/charge :route.charge/list
    :heraldicon.entity.type/ribbon :route.ribbon/list
    :heraldicon.entity.type/collection :route.collection/list))

(defn- modal-title-key [entity-type]
  (case entity-type
    :heraldicon.entity.type/arms :string.text.title/delete-arms
    :heraldicon.entity.type/charge :string.text.title/delete-charge
    :heraldicon.entity.type/ribbon :string.text.title/delete-ribbon
    :heraldicon.entity.type/collection :string.text.title/delete-collection))

(defn- description-key [entity-type]
  (case entity-type
    :heraldicon.entity.type/arms :string.user.message/delete-arms-description
    :heraldicon.entity.type/charge :string.user.message/delete-charge-description
    :heraldicon.entity.type/ribbon :string.user.message/delete-ribbon-description
    :heraldicon.entity.type/collection :string.user.message/delete-collection-description))

(defn- success-message-key [entity-type]
  (case entity-type
    :heraldicon.entity.type/arms :string.user.message/arms-deleted
    :heraldicon.entity.type/charge :string.user.message/charge-deleted
    :heraldicon.entity.type/ribbon :string.user.message/ribbon-deleted
    :heraldicon.entity.type/collection :string.user.message/collection-deleted))

(rf/reg-event-fx ::confirm
  (fn [{:keys [db]} [_ entity-type entity-id]]
    (let [session (session/data-from-db db)]
      (modal/clear)
      (modal/start-loading)
      (go-catch
       (try
         (let [{:keys [invalidated-ids]} (<? (api/call :delete-entity
                                                       {:id entity-id}
                                                       session))]
           (modal/stop-loading)
           (rf/dispatch-sync [::entity-list/remove entity-type entity-id])
           (rf/dispatch-sync [::entity-search/remove entity-type entity-id])
           (rf/dispatch-sync [::repository.entity/invalidate-entities invalidated-ids])
           (rf/dispatch-sync [::entity-for-rendering/invalidate-entities invalidated-ids])
           (rf/dispatch-sync [::entity-for-editing/invalidate-entities invalidated-ids])
           (rf/dispatch [::message/set-success entity-type (success-message-key entity-type)])
           (reife/push-state (list-route entity-type) nil nil))
         (catch :default e
           (modal/stop-loading)
           (log/error e "delete failed")
           (rf/dispatch [::message/set-error
                         entity-type
                         (or (:message (ex-data e)) (.-message e))]))))
      {})))

(defn- confirm-content [entity-type entity-id]
  [:div
   [:p [tr (description-key entity-type)]]
   [:div.buttons {:style {:display "flex"
                          :margin-top "10px"}}
    [:div {:style {:flex "auto"}}]
    [:button.button
     {:type "button"
      :style {:flex "initial"
              :margin-left "10px"}
      :on-click #(modal/clear)}
     [tr :string.button/cancel]]
    [:button.button.danger
     {:type "button"
      :style {:flex "initial"
              :margin-left "10px"}
      :on-click #(rf/dispatch [::confirm entity-type entity-id])}
     [tr :string.button/delete]]]])

(rf/reg-event-fx ::invoke
  (fn [{:keys [db]} [_ entity-type]]
    (let [form-db-path (form/data-path entity-type)
          entity-id (get-in db (conj form-db-path :id))]
      (modal/create
       [tr (modal-title-key entity-type)]
       [confirm-content entity-type entity-id])
      {})))

(defn action [entity-type]
  (when (supports-delete? entity-type)
    (let [form-db-path (form/data-path entity-type)
          session-data @(rf/subscribe [::session/data])
          can-delete? (and @(rf/subscribe [::session/logged-in?])
                           @(rf/subscribe [::entity/saved? form-db-path])
                           (or @(rf/subscribe [::entity/owned-by? form-db-path session-data])
                               @(rf/subscribe [::session/admin?])))]
      {:title :string.button/delete
       :icon "fas fa-trash"
       :destructive? true
       :handler (when can-delete?
                  #(rf/dispatch [::invoke entity-type]))
       :disabled? (not can-delete?)
       :tooltip (when-not can-delete?
                  :string.user.message/need-to-be-logged-in-and-own-to-delete)})))

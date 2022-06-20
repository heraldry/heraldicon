(ns heraldicon.frontend.entity.details
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.entity.id :as id]
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.frontend.loading :as loading]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.not-found :as not-found]
   [heraldicon.frontend.repository.core :as repository]
   [heraldicon.localization.string :as string]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]))

(rf/reg-sub ::prepare-for-editing
  (fn [[_ entity-id version target-path] _]
    [(rf/subscribe [:get (conj target-path :id)])
     (rf/subscribe [:get (conj target-path :version)])
     (rf/subscribe [::repository/entity-for-editing entity-id version])])

  (fn [[current-id current-version {:keys [status entity] :as result}] [_ _entity-id _version target-path]]
    (if (= status :done)
      (if (= [current-id current-version]
             ((juxt :id :version) entity))
        result
        (do
          (rf/dispatch [:set target-path entity])
          {:status :loading}))
      result)))

(defn by-id-view [form-id entity-id version component-fn]
  (let [form-db-path (form/data-path form-id)
        {:keys [status]} @(rf/subscribe [::prepare-for-editing
                                         entity-id version
                                         form-db-path])]
    (case status
      :done [component-fn form-db-path]
      (nil :loading) [loading/loading]
      :error [not-found/not-found])))

(defn- load-new-entity-data [generate-data-fn target-path]
  (go
    (let [data (<? (generate-data-fn))]
      (rf/dispatch [:set target-path data]))))

(defn create-view [form-id component-fn generate-data-fn]
  (let [form-db-path (form/data-path form-id)
        currently-nil? @(rf/subscribe [:nil? form-db-path])
        current-id @(rf/subscribe [:get (conj form-db-path :id)])
        loading? (or currently-nil?
                     current-id)]
    (if loading?
      (do
        (load-new-entity-data generate-data-fn form-db-path)
        [loading/loading])
      [component-fn form-db-path])))

(defn- details-route [form-id]
  (case form-id
    :heraldicon.entity/arms :route.arms.details/by-id-and-version
    :heraldicon.entity/charge :route.charge.details/by-id-and-version
    :heraldicon.entity/ribbon :route.ribbon.details/by-id-and-version
    :heraldicon.entity/collection :route.collection.details/by-id-and-version))

(defn save [form-id]
  (let [form-db-path (form/data-path form-id)
        entity @(rf/subscribe [:get form-db-path])]
    (repository/store
     form-id entity
     :on-start #(modal/start-loading)
     :on-complete #(modal/stop-loading)
     :on-success (fn [new-entity]
                   (let [new-data (merge entity new-entity)]
                     (rf/dispatch-sync [:set form-db-path new-data])
                     (rf/dispatch-sync [::message/set-success
                                        form-id
                                        (string/str-tr :string.user.message/arms-saved " " (:version new-data))])
                     ;; TODO: this flashes noticeably
                     (reife/replace-state (details-route form-id) {:id (id/for-url (:id new-data))
                                                                   :version (:version new-data)})))
     :on-error (fn [error]
                 (rf/dispatch [::message/set-error form-id (:message (ex-data error))])))))

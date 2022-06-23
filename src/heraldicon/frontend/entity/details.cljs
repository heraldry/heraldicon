(ns heraldicon.frontend.entity.details
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.entity.id :as id]
   [heraldicon.frontend.entity.action.copy-to-new :as copy-to-new]
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.frontend.history.core :as history]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.repository.entity-for-editing :as entity-for-editing]
   [heraldicon.frontend.status :as status]
   [heraldicon.localization.string :as string]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]))

(rf/reg-sub ::prepare-for-editing
  (fn [[_ entity-id version target-path] _]
    [(rf/subscribe [:get (conj target-path :id)])
     (rf/subscribe [:get (conj target-path :version)])
     (rf/subscribe [::entity-for-editing/data entity-id version])])

  (fn [[current-id current-version {:keys [status entity] :as result}] [_ _entity-id _version target-path]]
    (if (= status :done)
      (if (= [current-id current-version]
             ((juxt :id :version) entity))
        result
        (do
          (rf/dispatch [:set target-path entity])
          {:status :loading}))
      result)))

(defn- details-route [entity-type]
  (case entity-type
    :heraldicon.entity.type/arms :route.arms.details/by-id-and-version
    :heraldicon.entity.type/charge :route.charge.details/by-id-and-version
    :heraldicon.entity.type/ribbon :route.ribbon.details/by-id-and-version
    :heraldicon.entity.type/collection :route.collection.details/by-id-and-version))

(defn by-id-view [entity-id version component-fn]
  (let [entity-type (id/type-from-id entity-id)
        form-db-path (form/data-path entity-type)]
    (when @(rf/subscribe [::history/identifier-changed? form-db-path entity-id])
      (rf/dispatch-sync [::history/clear form-db-path entity-id]))
    (status/default
     (rf/subscribe [::prepare-for-editing
                    entity-id version
                    form-db-path])
     (fn [{:keys [entity]}]
       (if version
         [component-fn form-db-path]
         (do
           (reife/replace-state (details-route entity-type) {:id (id/for-url entity-id)
                                                             :version (:version entity)})
           [status/loading])))
     :on-error (fn [error]
                 (if (-> error ex-cause (= :entity-not-found))
                   [status/not-found]
                   [status/error-display])))))

(defn- load-new-entity-data [entity-type generate-data-fn target-path]
  (if @(rf/subscribe [::copy-to-new/copy-data entity-type])
    (rf/dispatch [::copy-to-new/copy entity-type target-path])
    (go
      (rf/dispatch [:set target-path (<? (generate-data-fn))]))))

(defn create-view [entity-type component-fn generate-data-fn]
  (let [form-db-path (form/data-path entity-type)
        currently-nil? @(rf/subscribe [:nil? form-db-path])
        current-id @(rf/subscribe [:get (conj form-db-path :id)])
        loading? (or currently-nil?
                     current-id)]
    (when @(rf/subscribe [::history/identifier-changed? form-db-path nil])
      (rf/dispatch-sync [::history/clear form-db-path nil]))
    (if loading?
      (do
        (load-new-entity-data entity-type generate-data-fn form-db-path)
        [status/loading])
      [component-fn form-db-path])))

(rf/reg-event-fx ::save
  (fn [{:keys [db]} [_ entity-type]]
    (let [form-db-path (form/data-path entity-type)
          entity (get-in db form-db-path)]
      {:fx [[:dispatch [::entity-for-editing/save
                        entity
                        {:on-start #(modal/start-loading)
                         :on-complete #(modal/stop-loading)
                         :on-success (fn [new-entity]
                                       (let [new-data (merge entity new-entity)]
                                         (rf/dispatch-sync [:set form-db-path new-data])
                                         (rf/dispatch-sync [::message/set-success
                                                            entity-type
                                                            (string/str-tr :string.user.message/arms-saved " " (:version new-data))])
                                         ;; TODO: this flashes noticeably
                                         (reife/replace-state (details-route entity-type) {:id (id/for-url (:id new-data))
                                                                                           :version (:version new-data)})))
                         :on-error (fn [error]
                                     (rf/dispatch [::message/set-error entity-type (:message (ex-data error))]))}]]]})))

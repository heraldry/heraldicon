(ns heraldicon.frontend.entity.details
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.entity.id :as id]
   [heraldicon.frontend.entity.action.copy-to-new :as copy-to-new]
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.frontend.history.core :as history]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.repository.entity-for-editing :as entity-for-editing]
   [heraldicon.frontend.status :as status]
   [heraldicon.localization.string :as string]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]))

(defn- int-version [version]
  (if (nil? version)
    version
    (let [n (js/parseInt version)]
      (if (js/isNaN n)
        nil
        n))))

(defn- details-route [entity-type]
  (case entity-type
    :heraldicon.entity.type/arms :route.arms.details/by-id-and-version
    :heraldicon.entity.type/charge :route.charge.details/by-id-and-version
    :heraldicon.entity.type/ribbon :route.ribbon.details/by-id-and-version
    :heraldicon.entity.type/collection :route.collection.details/by-id-and-version))

;; TODO: there probably is a better way to do this
;; but it needs to be outside a subscription, so saving doesn't cause it to
;; write the old, unchanged entity of id/version to the history when saving
;; changes the id/version
(defn- prepare-for-editing [entity-id version]
  (let [entity-type (id/type-from-id entity-id)
        form-db-path (form/data-path entity-type)
        current-id @(rf/subscribe [:get (conj form-db-path :id)])
        current-version @(rf/subscribe [:get (conj form-db-path :version)])
        version (int-version version)]
    (when (not= [entity-id version]
                [current-id current-version])
      (let [{:keys [status entity]} @(rf/subscribe [::entity-for-editing/data entity-id version])]
        (when (= status :done)
          (when (not= [(:id entity) (:version entity)]
                      [current-id current-version])
            (rf/dispatch-sync [:set form-db-path entity]))
          (reife/replace-state (details-route entity-type) {:id (id/for-url entity-id)
                                                            :version (:version entity)}))))))

(defn- clear-history-on-new-identifier [form-db-path entity-id]
  (when @(rf/subscribe [::history/identifier-changed? form-db-path entity-id])
    (rf/dispatch-sync [::history/clear form-db-path entity-id])))

(defn by-id-view [entity-id version component-fn]
  (let [entity-type (id/type-from-id entity-id)
        form-db-path (form/data-path entity-type)]
    (prepare-for-editing entity-id version)
    (clear-history-on-new-identifier form-db-path entity-id)
    (status/default
     (rf/subscribe [::entity-for-editing/data entity-id version])
     (fn [_]
       (if version
         [component-fn form-db-path]
         [status/loading]))
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
    (clear-history-on-new-identifier form-db-path nil)
    (if loading?
      (do
        (load-new-entity-data entity-type generate-data-fn form-db-path)
        [status/loading])
      [component-fn form-db-path])))

(rf/reg-fx ::replace-route
  (fn [[route params]]
    (reife/replace-state route params)))

(macros/reg-event-fx ::replace-data
  (fn [{:keys [db]} [_ {entity-id :id
                        version :version
                        :as entity}]]
    (let [entity-type (id/type-from-id entity-id)
          form-db-path (form/data-path entity-type)]
      {:db (assoc-in db form-db-path entity)
       ::replace-route [(details-route entity-type)
                        {:id (id/for-url entity-id)
                         :version version}]})))

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
                                         (rf/dispatch-sync [::replace-data new-data])
                                         (rf/dispatch-sync [::message/set-success
                                                            entity-type
                                                            (string/str-tr :string.user.message/arms-saved " " (:version new-data))])))
                         :on-error (fn [error]
                                     (rf/dispatch [::message/set-error entity-type (:message (ex-data error))]))}]]]})))

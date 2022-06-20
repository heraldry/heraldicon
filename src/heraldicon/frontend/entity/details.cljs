(ns heraldicon.frontend.entity.details
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.frontend.loading :as loading]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.not-found :as not-found]
   [heraldicon.frontend.repository.core :as repository]
   [heraldicon.localization.string :as string]
   [re-frame.core :as rf]))

(rf/reg-sub ::prepare-for-editing
  (fn [[_ entity-id version target-paths] _]
    [(rf/subscribe [:get (first target-paths)])
     (rf/subscribe [::repository/entity-for-editing entity-id version])])

  (fn [[current {:keys [status entity] :as result}] [_ _entity-id _version target-paths]]
    (if (= status :done)
      (if (= (select-keys current [:id :version])
             (select-keys entity [:id :version]))
        result
        (do
          (doseq [target-path target-paths]
            (rf/dispatch [:set target-path entity]))
          {:status :loading}))
      result)))

(defn by-id-view [form-id entity-id version component-fn]
  (let [form-db-path (form/data-path form-id)
        saved-data-db-path (form/saved-data-path form-id)
        {:keys [status]} @(rf/subscribe [::prepare-for-editing
                                         entity-id version
                                         [form-db-path saved-data-db-path]])]
    (case status
      :done [component-fn]
      (nil :loading) [loading/loading]
      :error [not-found/not-found])))

(defn- load-new-entity-data [generate-data-fn target-paths]
  (go
    (let [data (<? (generate-data-fn))]
      (doseq [target-path target-paths]
        (rf/dispatch [:set target-path data])))))

(defn create-view [form-id component-fn generate-data-fn]
  (let [form-db-path (form/data-path form-id)
        saved-data-db-path (form/saved-data-path form-id)
        currently-nil? @(rf/subscribe [:nil? form-db-path])
        current-id @(rf/subscribe [:get (conj form-db-path :id)])
        loading? (or currently-nil?
                     current-id)]
    (if loading?
      (do
        (load-new-entity-data generate-data-fn [form-db-path saved-data-db-path])
        [loading/loading])
      [component-fn])))

(defn save [form-id]
  (let [form-db-path (form/data-path form-id)
        entity @(rf/subscribe [:get form-db-path])]
    (repository/store
     form-id entity
     :on-start #(modal/start-loading)
     :on-complete #(modal/stop-loading)
     :on-success (fn [response]
                   ;; TODO: wire up things
                   ;; - update saved data
                   ;; - redirect to new route
                   (rf/dispatch [::message/set-success
                                 form-id
                                 (string/str-tr :string.user.message/arms-saved " " (:version response))]))
     :on-error (fn [error]
                 (rf/dispatch [::message/set-error form-id (:message (ex-data error))])))))

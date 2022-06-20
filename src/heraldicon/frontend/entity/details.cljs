(ns heraldicon.frontend.entity.details
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.frontend.loading :as loading]
   [heraldicon.frontend.not-found :as not-found]
   [heraldicon.frontend.repository.core :as repository]
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

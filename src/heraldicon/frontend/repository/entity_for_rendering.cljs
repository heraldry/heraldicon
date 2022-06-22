(ns heraldicon.frontend.repository.entity-for-rendering
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<? go-catch]]
   [heraldicon.frontend.http :as http]
   [heraldicon.frontend.repository.core :as repository]
   [heraldicon.frontend.repository.entity :as entity]
   [re-frame.core :as rf]
   [taoensso.timbre :as log])
  (:require-macros [reagent.ratom :refer [reaction]]))

(def ^:private db-path-entity-for-rendering
  (conj repository/db-path-base :entity-for-rendering))

(defn- entity-for-rendering-path [entity-id version]
  (conj db-path-entity-for-rendering [entity-id version]))

(rf/reg-event-db ::store
  (fn [db [_ {:keys [id version] :as entity}]]
    (assoc-in db (entity-for-rendering-path id version) {:status :done
                                                         :entity entity})))

(rf/reg-event-db ::store-error
  (fn [db [_ entity-id version error]]
    (assoc-in db (entity-for-rendering-path entity-id version) {:status :error
                                                                :error error})))

(defmulti ^:private load-editing-data :type)

(defmethod load-editing-data :heraldicon.entity.type/charge [entity]
  (go-catch
   (let [edn-data (get-in entity [:data :edn-data])]
     (cond-> entity
       (not edn-data) (assoc-in [:data :edn-data] (<? (http/fetch (-> entity :data :edn-data-url))))))))

(defmethod load-editing-data :default [entity]
  (go entity))

(defn- prepare-for-editing [entity]
  (go
    (try
      (let [updated-entity (<? (load-editing-data entity))]
        (rf/dispatch [::store updated-entity]))
      (catch :default e
        (log/error "fetching entity data for editing error:" e)
        (rf/dispatch [::store-error (:id entity) (:version entity) e])))))

(defn- fetch-entity-for-rendering [entity-id version]
  (let [{:keys [status entity error] :as result} @(rf/subscribe [::entity/data entity-id version])]
    (case status
      :done (prepare-for-editing entity)
      :error (rf/dispatch [::store-error entity-id version error])
      result)))

(rf/reg-sub-raw ::data
  (fn [_app-db [_ entity-id version]]
    (reaction
     (let [version @(rf/subscribe [::entity/effective-version entity-id version])]
       (repository/async-query-data (entity-for-rendering-path entity-id version)
                                    (partial fetch-entity-for-rendering entity-id version))))))

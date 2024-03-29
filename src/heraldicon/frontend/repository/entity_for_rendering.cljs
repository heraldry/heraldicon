(ns heraldicon.frontend.repository.entity-for-rendering
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<? go-catch]]
   [heraldicon.frontend.http :as http]
   [heraldicon.frontend.repository.core :as repository]
   [heraldicon.frontend.repository.entity :as entity]
   [heraldicon.frontend.repository.query :as query]
   [re-frame.core :as rf]
   [taoensso.timbre :as log])
  (:require-macros [reagent.ratom :refer [reaction]]))

(def ^:private db-path-entity-for-rendering
  (conj repository/db-path-base :entity-for-rendering))

(defn- entity-for-rendering-path [entity-id version]
  (conj db-path-entity-for-rendering [entity-id version]))

(rf/reg-event-db ::store
  (fn [db [_ {:keys [id version] :as entity}]]
    (let [path (entity-for-rendering-path id version)]
      (assoc-in db path {:status :done
                         :entity entity
                         :path path}))))

(rf/reg-event-db ::store-error
  (fn [db [_ entity-id version error]]
    (let [path (entity-for-rendering-path entity-id version)]
      (assoc-in db path {:status :error
                         :error error
                         :path path}))))

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
    (let [query-id [::prepare-for-editing (:id entity) (:version entity)]]
      (when-not (query/running? query-id)
        (query/register query-id)
        (try
          (let [updated-entity (<? (load-editing-data entity))]
            (rf/dispatch [::store updated-entity]))
          (catch :default e
            (log/error e "fetching entity data for editing error")
            (rf/dispatch [::store-error (:id entity) (:version entity) e]))
          (finally
            (query/unregister query-id)))))))

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

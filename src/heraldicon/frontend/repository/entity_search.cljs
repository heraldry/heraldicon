(ns heraldicon.frontend.repository.entity-search
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.frontend.api :as api]
   [heraldicon.frontend.entity.action.favorite :as favorite]
   [heraldicon.frontend.repository.core :as repository]
   [heraldicon.frontend.repository.query :as query]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]
   [taoensso.timbre :as log])
  (:require-macros [reagent.ratom :refer [reaction]]))

(def ^:private db-path-entity-search
  (conj repository/db-path-base :entity-search))

(defn- entity-search-path [id entity-type]
  (conj db-path-entity-search id entity-type))

(defn- add-favorites
  [db entities]
  (reduce (fn [db {:keys [id favorite-count is-user-favorite]}]
            (-> db
                (favorite/set-favorite-count id favorite-count)
                (favorite/set-is-user-favorite id is-user-favorite)))
          db
          entities))

(rf/reg-event-db ::store
  (fn [db [_ id entity-type query total entities tags]]
    (let [path (entity-search-path id entity-type)]
      (-> db
          (assoc-in path {:status :done
                          :total total
                          :entities entities
                          :tags tags
                          :query query
                          :path (conj path :entities)})
          (add-favorites entities)))))

(rf/reg-event-db ::store-more
  (fn [db [_ id entity-type total entities]]
    (let [path (entity-search-path id entity-type)
          original-entities (get-in db (conj path :entities))
          merged-entities (->> (concat original-entities entities)
                               distinct
                               (into []))]
      (-> db
          (assoc-in (conj path :total) total)
          (assoc-in (conj path :entities) merged-entities)
          (add-favorites entities)))))

(rf/reg-event-db ::store-error
  (fn [db [_ id entity-type query error]]
    (let [path (entity-search-path id entity-type)]
      (assoc-in db path {:status :error
                         :query query
                         :error error}))))

(rf/reg-event-db ::clear
  (fn [db [_ id entity-type]]
    (let [path (entity-search-path id entity-type)]
      (assoc-in db path nil))))

(defn- fetch-entity-search-api-function [enty-type]
  (case enty-type
    :arms :search-arms
    :charge :search-charges
    :ribbon :search-ribbons
    :collection :search-collections))

(defn- fetch [id entity-type query session]
  (go
    (let [query-id [::fetch id entity-type session]]
      (when-not (query/running? query-id)
        (query/register query-id)
        (try
          (let [{:keys [items total tags]} (<? (api/call (fetch-entity-search-api-function entity-type)
                                                         query
                                                         session))]
            (rf/dispatch [::store id entity-type query total items tags]))
          (catch :default e
            (log/error e "search entity error")
            (rf/dispatch [::store-error id entity-type query e]))
          (finally
            (query/unregister query-id)))))))

(defn- fetch-more [id entity-type query offset page-size session]
  (go
    (let [query-id [::fetch id entity-type session]
          query (assoc query
                       :offset offset
                       :page-size page-size)]
      (when-not (query/running? query-id)
        (query/register query-id)
        (try
          (let [{:keys [items total]} (<? (api/call (fetch-entity-search-api-function entity-type)
                                                    query
                                                    session))]
            (rf/dispatch [::store-more id entity-type total items]))
          (catch :default e
            (log/error e "search entity error"))
          (finally
            (query/unregister query-id)))))))

(rf/reg-event-db ::load-more
  (fn [db [_ id entity-type page-size]]
    (let [path (entity-search-path id entity-type)
          {:keys [query entities]} (get-in db path)
          offset (count entities)
          session (session/data-from-db db)]
      (fetch-more id entity-type query offset page-size session)
      db)))

(rf/reg-sub-raw ::data
  (fn [_app-db [_ id entity-type query]]
    (reaction
     (let [session @(rf/subscribe [::session/data])]
       (repository/async-query-data (entity-search-path id entity-type)
                                    (partial fetch id entity-type query session)
                                    :data-stale? (fn [data]
                                                   (not= (:query data) query)))))))

(rf/reg-sub-raw ::data-raw
  (fn [_app-db [_ id entity-type]]
    (reaction
     (let [path (entity-search-path id entity-type)]
       @(rf/subscribe [:get (conj path :entities)])))))

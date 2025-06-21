(ns heraldicon.frontend.repository.entity-search
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.frontend.api :as api]
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

(rf/reg-event-db ::store
  (fn [db [_ id entity-type total entities]]
    (js/console.log :total total :entities entities)
    (let [path (entity-search-path id entity-type)]
      (-> db
          (assoc-in path {:status :done
                          :total total
                          :entities entities
                          :path (conj path :entities)})))))

(rf/reg-event-db ::store-error
  (fn [db [_ id entity-type error]]
    (assoc-in db (entity-search-path id entity-type) {:status :error
                                                      :error error})))

(rf/reg-event-db ::update
  (fn [db [_ id new-entity]]
    (let [entity-type (:type new-entity)
          path (conj (entity-search-path id entity-type) :entities)
          entities (get-in db path)
          known-id? (some #(-> % :id (= (:id new-entity))) entities)
          new-entities (if known-id?
                         (mapv (fn [entity]
                                 (if (and (= (:id new-entity)
                                             (:id entity))
                                          (> (:version new-entity)
                                             (:version entity)))
                                   new-entity
                                   entity))
                               entities)
                         (when-not (nil? entities)
                           (conj entities new-entity)))]

      (cond-> db
        entities (assoc-in path new-entities)))))

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

(defn- fetch [id entity-type session]
  (go
    (let [query-id [::fetch id entity-type session]]
      (when-not (query/running? query-id)
        (query/register query-id)
        (try
          (let [{:keys [items total]} (<? (api/call (fetch-entity-search-api-function entity-type)
                                                    {}
                                                    session))]
            (rf/dispatch [::store id entity-type total items]))
          (catch :default e
            (log/error e "search entity error")
            (rf/dispatch [::store-error id entity-type e]))
          (finally
            (query/unregister query-id)))))))

(rf/reg-sub-raw ::data
  (fn [_app-db [_ id entity-type]]
    (reaction
     (let [session @(rf/subscribe [::session/data])]
       (repository/async-query-data (entity-search-path id entity-type)
                                    (partial fetch id entity-type session))))))

(rf/reg-sub-raw ::data-raw
  (fn [_app-db [_ id entity-type]]
    (reaction
     (let [path (entity-search-path id entity-type)]
       @(rf/subscribe [:get (conj path :entities)])))))

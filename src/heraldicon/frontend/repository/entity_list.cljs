(ns heraldicon.frontend.repository.entity-list
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.frontend.repository.core :as repository]
   [heraldicon.frontend.repository.entity :as repository.entity]
   [heraldicon.frontend.repository.query :as query]
   [heraldicon.frontend.repository.request :as request]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]
   [taoensso.timbre :as log])
  (:require-macros [reagent.ratom :refer [reaction]]))

(def ^:private db-path-entity-list
  (conj repository/db-path-base :entity-list))

(defn- entity-list-path [entity-type]
  (conj db-path-entity-list entity-type))

(rf/reg-event-db ::store
  (fn [db [_ entity-type entities]]
    (let [path (entity-list-path entity-type)]
      (-> db
          (assoc-in path {:status :done
                          :entities entities
                          :path (conj path :entities)})
          (repository.entity/update-latest-versions entities)))))

(rf/reg-event-db ::store-error
  (fn [db [_ entity-type error]]
    (assoc-in db (entity-list-path entity-type) {:status :error
                                                 :error error})))

(rf/reg-event-db ::update
  (fn [db [_ new-entity]]
    (let [entity-type (:type new-entity)
          path (conj (entity-list-path entity-type) :entities)
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
  (fn [db [_ entity-type]]
    (let [path (entity-list-path entity-type)]
      (assoc-in db path nil))))

(defn- fetch-entity-list-api-function [enty-type]
  (case enty-type
    :heraldicon.entity.type/arms :fetch-arms-list
    :heraldicon.entity.type/charge :fetch-charges-list
    :heraldicon.entity.type/ribbon :fetch-ribbons-list
    :heraldicon.entity.type/collection :fetch-collections-list))

(defn- fetch [entity-type session & {:keys [on-loaded]}]
  (go
    (let [query-id [::fetch entity-type session]]
      (when-not (query/running? query-id)
        (query/register query-id)
        (try
          (let [entities (:items (<? (request/call (fetch-entity-list-api-function entity-type)
                                                   {}
                                                   session)))]
            (rf/dispatch [::store entity-type entities])
            (when on-loaded
              (on-loaded entities)))
          (catch :default e
            (log/error e "fetch entity list error")
            (rf/dispatch [::store-error entity-type e]))
          (finally
            (query/unregister query-id)))))))

(rf/reg-sub-raw ::data
  (fn [_app-db [_ entity-type on-loaded]]
    (reaction
     (let [session @(rf/subscribe [::session/data])]
       (repository/async-query-data (entity-list-path entity-type)
                                    (partial fetch entity-type session)
                                    :on-loaded on-loaded)))))

(rf/reg-fx ::fetch
  (fn [{:keys [entity-type on-loaded session]}]
    (fetch entity-type session :on-loaded on-loaded)))

(rf/reg-event-fx ::load-if-absent
  (fn [{:keys [db]} [_ entity-type on-loaded]]
    (let [path (entity-list-path entity-type)
          session (session/data-from-db db)]
      (cond-> {}
        (not (get-in db path)) (assoc ::fetch {:entity-type entity-type
                                               :on-loaded on-loaded
                                               :session session})))))

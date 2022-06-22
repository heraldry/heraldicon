(ns heraldicon.frontend.repository.entity-list
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.frontend.repository.core :as repository]
   [heraldicon.frontend.repository.request :as request]
   [heraldicon.frontend.user :as user]
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
      (assoc-in db path {:status :done
                         :entities entities
                         :path (conj path :entities)}))))

(rf/reg-event-db ::store-error
  (fn [db [_ entity-type error]]
    (assoc-in db (entity-list-path entity-type) {:status :error
                                                 :error error})))

(rf/reg-event-db ::update
  (fn [db [_ new-entity]]
    (let [entity-type (:type new-entity)
          path (conj (entity-list-path entity-type) :entities)
          entities (get-in db path)]
      (cond-> db
        entities (assoc-in path (mapv (fn [entity]
                                        (if (and (= (:id new-entity)
                                                    (:id entity))
                                                 (> (:version new-entity)
                                                    (:version entity)))
                                          new-entity
                                          entity))
                                      entities))))))

(rf/reg-event-db ::clear
  (fn [db [_ entity-type]]
    (let [path (entity-list-path entity-type)]
      (assoc-in db path nil))))

(rf/reg-event-db ::clear-all
  (fn [db _]
    (assoc-in db db-path-entity-list nil)))

(defn- fetch-entity-list-api-function [enty-type]
  (case enty-type
    :heraldicon.entity.type/arms :fetch-arms-list
    :heraldicon.entity.type/charge :fetch-charges-list
    :heraldicon.entity.type/ribbon :fetch-ribbons-list
    :heraldicon.entity.type/collection :fetch-collections-list))

(defn- fetch [entity-type & {:keys [on-loaded]}]
  (let [user-data (user/data)]
    (go
      (try
        (let [entities (:items (<? (request/call (fetch-entity-list-api-function entity-type)
                                                 {}
                                                 user-data)))]
          (rf/dispatch [::store entity-type entities])
          (when on-loaded
            (on-loaded entities)))
        (catch :default e
          (log/error "fetch entity list error:" e)
          (rf/dispatch [::store-error entity-type e]))))))

(rf/reg-sub-raw ::data
  (fn [_app-db [_ entity-type on-loaded]]
    (reaction
     (repository/async-query-data (entity-list-path entity-type)
                                  (partial fetch entity-type)
                                  :on-loaded on-loaded))))

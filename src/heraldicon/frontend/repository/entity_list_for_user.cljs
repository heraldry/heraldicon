(ns heraldicon.frontend.repository.entity-list-for-user
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.frontend.repository.core :as repository]
   [heraldicon.frontend.repository.request :as request]
   [heraldicon.frontend.user :as user]
   [re-frame.core :as rf]
   [taoensso.timbre :as log])
  (:require-macros [reagent.ratom :refer [reaction]]))

(def ^:private db-path-entity-list-for-user
  (conj repository/db-path-base :entity-list-for-user))

(defn- entity-list-for-user-path [entity-type user-id]
  (conj db-path-entity-list-for-user [entity-type user-id]))

(rf/reg-event-fx ::store
  (fn [{:keys [db]} [_ entity-type user-id entities]]
    {:db (let [path (entity-list-for-user-path entity-type user-id)]
           (assoc-in db path {:status :done
                              :entities entities
                              :path (conj path :entities)}))
     :fx [[:dispatch [:heraldicon.frontend.repository.entity/update-latest-versions entities]]]}))

(rf/reg-event-db ::store-error
  (fn [db [_ entity-type user-id error]]
    (assoc-in db (entity-list-for-user-path entity-type user-id) {:status :error
                                                                  :error error})))

(rf/reg-event-db ::update
  (fn [db [_ new-entity]]
    (let [entity-type (:type new-entity)
          user-id (:user-id new-entity)
          path (conj (entity-list-for-user-path entity-type user-id) :entities)
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
  (fn [db [_ entity-type user-id]]
    (let [path (entity-list-for-user-path entity-type user-id)]
      (assoc-in db path nil))))

(rf/reg-event-db ::clear-all
  (fn [db _]
    (assoc-in db db-path-entity-list-for-user nil)))

(defn- fetch-entity-list-for-user-api-function [enty-type]
  (case enty-type
    :heraldicon.entity.type/arms :fetch-arms-for-user
    :heraldicon.entity.type/charge :fetch-charges-for-user
    :heraldicon.entity.type/ribbon :fetch-ribbons-for-user
    :heraldicon.entity.type/collection :fetch-collections-for-user))

(defn- fetch [entity-type user-id]
  (let [user-data (user/data)]
    (go
      (try
        (let [entities (:items (<? (request/call (fetch-entity-list-for-user-api-function entity-type)
                                                 {:user-id user-id}
                                                 user-data)))]
          (rf/dispatch [::store entity-type user-id entities]))
        (catch :default e
          (log/error "fetch entity list error:" e)
          (rf/dispatch [::store-error entity-type user-id e]))))))

(rf/reg-sub-raw ::data
  (fn [_app-db [_ entity-type user-id]]
    (reaction
     (repository/async-query-data (entity-list-for-user-path entity-type user-id)
                                  (partial fetch entity-type user-id)))))

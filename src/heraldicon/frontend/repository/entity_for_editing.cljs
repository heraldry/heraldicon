(ns heraldicon.frontend.repository.entity-for-editing
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<? go-catch]]
   [heraldicon.frontend.http :as http]
   [heraldicon.frontend.repository.api :as api]
   [heraldicon.frontend.repository.core :as repository]
   [heraldicon.frontend.repository.entity :as entity]
   [heraldicon.frontend.user :as user]
   [heraldicon.util.core :as util]
   [re-frame.core :as rf]
   [taoensso.timbre :as log])
  (:require-macros [reagent.ratom :refer [reaction]]))

(def ^:private db-path-entity-for-editing
  (conj repository/db-path-base :entity-for-editing))

(defn- entity-for-editing-path [entity-id version]
  (conj db-path-entity-for-editing [entity-id version]))

(rf/reg-event-db ::store
  (fn [db [_ {:keys [id version] :as entity}]]
    (assoc-in db (entity-for-editing-path id version) {:status :done
                                                       :entity entity})))

(rf/reg-event-db ::store-error
  (fn [db [_ entity-id version error]]
    (assoc-in db (entity-for-editing-path entity-id version) {:status :error
                                                              :error error})))

(defmulti ^:private load-editing-data :type)

(defmethod load-editing-data :heraldicon.entity.type/charge [entity]
  (go-catch
   (let [edn-data (get-in entity [:data :edn-data])
         svg-data (get-in entity [:data :svg-data])]
     (cond-> entity
       (not edn-data) (assoc-in [:data :edn-data] (<? (http/fetch (-> entity :data :edn-data-url))))
       (not svg-data) (assoc-in [:data :svg-data] (<? (http/fetch (-> entity :data :svg-data-url))))))))

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

(defn- fetch-entity-for-editing [entity-id version]
  (let [{:keys [status entity error] :as result} @(rf/subscribe [::entity/data entity-id version])]
    (case status
      :done (prepare-for-editing entity)
      :error (rf/dispatch [::store-error entity-id version error])
      result)))

(rf/reg-sub-raw ::data
  (fn [_app-db [_ entity-id version]]
    (reaction
     (let [version @(rf/subscribe [::entity/effective-version entity-id version])]
       (repository/async-query-data (entity-for-editing-path entity-id version)
                                    (partial fetch-entity-for-editing entity-id version))))))

(defn- save-entity-api-function [entity-type]
  (case entity-type
    :heraldicon.entity.type/arms :save-arms
    :heraldicon.entity.type/charge :save-charge
    :heraldicon.entity.type/ribbon :save-ribbon
    :heraldicon.entity.type/collection :save-collection))

(defn store [entity & {:keys [on-start on-complete on-success on-error]}]
  (let [user-data (user/data)]
    (go
      (when on-start
        (on-start))
      (try
        (let [new-entity (<? (api/call
                              (save-entity-api-function (:type entity))
                              entity user-data))]
         ;; TODO: wire up things
         ;; - update list repository

          (rf/dispatch [::entity/store new-entity])
          (rf/dispatch [::store (util/deep-merge-with
                                 (fn [_left right]
                                   right)
                                 entity new-entity)])

          (when on-success
            (on-success new-entity)))

        (catch :default e
          (log/error "save entity error:" e)
          (when on-error
            (on-error e)))

        (finally
          (when on-complete
            (on-complete)))))))

(ns heraldicon.frontend.repository.core
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<? go-catch]]
   [heraldicon.entity.id :as id]
   [heraldicon.frontend.api.request :as api.request]
   [heraldicon.frontend.http :as http]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.repository.request :as request]
   [heraldicon.frontend.user :as user]
   [heraldicon.util.core :as util]
   [re-frame.core :as rf]
   [taoensso.timbre :as log])
  (:require-macros [reagent.ratom :refer [reaction]]))

(def ^:private db-path-base
  [:repository])

(def ^:private db-path-entity
  (conj db-path-base :entity))

(defn- entity-path [entity-id version]
  (conj db-path-entity [entity-id version]))

(def ^:private db-path-entity-for-editing
  (conj db-path-base :entity-for-editing))

(defn- entity-for-editing-path [entity-id version]
  (conj db-path-entity-for-editing [entity-id version]))

(def ^:private db-path-latest-versions
  (conj db-path-base :latest-versions))

(defn- latest-version-path [entity-id]
  (conj db-path-latest-versions entity-id))

(rf/reg-sub ::latest-entity-version
  (fn [[_ entity-id] _]
    (rf/subscribe [:get (latest-version-path entity-id)]))

  (fn [version [_ _entity-id]]
    version))

(defn- fetch-entity-api-function [entity-type]
  (case entity-type
    :heraldicon.entity/arms :fetch-arms
    :heraldicon.entity/charge :fetch-charge
    :heraldicon.entity/ribbon :fetch-ribbon
    :heraldicon.entity/collection :fetch-collection))

(defn- save-entity-api-function [entity-type]
  (case entity-type
    :heraldicon.entity/arms :save-arms
    :heraldicon.entity/charge :save-charge
    :heraldicon.entity/ribbon :save-ribbon
    :heraldicon.entity/collection :save-collection))

(macros/reg-event-db ::store-entity
  (fn [db [_ {:keys [id version] :as entity}]]
    (-> db
        (assoc-in (entity-path id version) {:status :done
                                            :entity entity})
        (update-in (latest-version-path id) (fn [previous-version]
                                              (max (or previous-version 0) version))))))

(macros/reg-event-db ::store-entity-load-error
  (fn [db [_ entity-id version error]]
    (assoc-in db (entity-path entity-id version) {:status :error
                                                  :entity error})))

(macros/reg-event-db ::store-entity-for-editing
  (fn [db [_ {:keys [id version] :as entity}]]
    (assoc-in db (entity-for-editing-path id version) {:status :done
                                                       :entity entity})))

(macros/reg-event-db ::store-entity-for-editing-load-error
  (fn [db [_ entity-id version error]]
    (assoc-in db (entity-for-editing-path entity-id version) {:status :error
                                                              :entity error})))

(defn- fetch-entity [entity-id version]
  (go
    (try
      (let [entity (<? (request/call (fetch-entity-api-function (id/type-from-id entity-id))
                                     {:id entity-id
                                      :version version}
                                     (user/data)))]
        (when-not entity
          (throw (ex-info "Not found" {} :entity-not-found)))
        (rf/dispatch [::store-entity entity]))
      (catch :default e
        (log/error "fetch entity error:" e)
        (rf/dispatch [::store-entity-load-error entity-id version e])))))

(defmulti ^:private load-editing-data (fn [entity]
                                        (some-> entity :id id/type-from-id)))

(defmethod load-editing-data :heraldicon.entity/charge [entity]
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
        (rf/dispatch [::store-entity-for-editing updated-entity]))
      (catch :default e
        (log/error "fetching entity data for editing error:" e)
        (rf/dispatch [::store-entity-for-editing-load-error (:id entity) (:version entity) e])))))

(defn- fetch-entity-for-editing [entity-id version]
  (let [{:keys [status entity error] :as result} @(rf/subscribe [::entity entity-id version])]
    (case status
      :done (prepare-for-editing entity)
      :error (rf/dispatch [::store-entity-for-editing-load-error entity-id version error])
      result)))

(defn- async-query-data [path load-fn]
  (let [data @(rf/subscribe [:get path])]
    (if data
      data
      (do
        (load-fn)
        {:status :loading}))))

(defn- parse-version [entity-id version]
  (let [parsed-version (cond
                         (nil? version) nil
                         (int? version) version
                         (string? version) (let [n (js/parseInt version)]
                                             (if (js/isNaN n)
                                               (throw (ex-info "Invalid version" {}))
                                               n))
                         :else (throw (ex-info "Invalid version" {})))]
    (if (or (nil? parsed-version)
            (zero? parsed-version))
      @(rf/subscribe [::latest-entity-version entity-id])
      parsed-version)))

(rf/reg-sub-raw ::entity
  (fn [_app-db [_ entity-id version]]
    (reaction
     (let [version (parse-version entity-id version)]
       (async-query-data (entity-path entity-id version)
                         (partial fetch-entity entity-id version))))))

(rf/reg-sub-raw ::entity-for-editing
  (fn [_app-db [_ entity-id version]]
    (reaction
     (let [version (parse-version entity-id version)]
       (async-query-data (entity-for-editing-path entity-id version)
                         (partial fetch-entity-for-editing entity-id version))))))

(defn store [entity-type entity & {:keys [on-start on-complete on-success on-error]}]
  (go
    (when on-start
      (on-start))
    (try
      (let [user-data (user/data)
            new-entity (<? (api.request/call
                            (save-entity-api-function entity-type)
                            entity user-data))]
        ;; TODO: wire up things
        ;; - update list repository

        (rf/dispatch [::store-entity new-entity])
        (rf/dispatch [::store-entity-for-editing (util/deep-merge-with
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
          (on-complete))))))

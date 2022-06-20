(ns heraldicon.frontend.repository.core
  (:require
   [cljs.core.async :refer [go]]
   [clojure.string :as s]
   [com.wsscode.async.async-cljs :refer [<? go-catch]]
   [heraldicon.frontend.api.request :as api.request]
   [heraldicon.frontend.http :as http]
   [heraldicon.frontend.repository.request :as request]
   [heraldicon.frontend.user :as user]
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

(defn- entity-type [entity-id]
  (case (some-> entity-id
                (s/split #":")
                first)
    "arms" :heraldicon.entity/arms
    "charge" :heraldicon.entity/charge
    "ribbon" :heraldicon.entity/ribbon
    "collection" :heraldicon.entity/collection))

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

(defn- fetch-entity [entity-id version path]
  (go
    (rf/dispatch-sync [:set path {:status :loading}])
    (try
      (let [entity (<? (request/call (fetch-entity-api-function (entity-type entity-id))
                                     {:id entity-id
                                      :version version}
                                     (user/data)))]
        (when-not entity
          (throw (ex-info "Not found" {} :entity-not-found)))
        ;; TODO: handle nil version case, store id/(latest-)version
        (rf/dispatch [:set path {:status :done
                                 :entity entity}]))
      (catch :default e
        (log/error "fetch entity error:" e)
        (rf/dispatch [:set path {:status :error
                                 :error e}])))))

(defmulti ^:private load-editing-data (fn [entity]
                                        (some-> entity :id entity-type)))

(defmethod load-editing-data :heraldicon.entity/charge [entity]
  (go-catch
   (let [edn-data (get-in entity [:data :edn-data])
         svg-data (get-in entity [:data :svg-data])]
     (cond-> entity
       (not edn-data) (assoc-in [:data :edn-data] (<? (http/fetch (-> entity :data :edn-data-url))))
       (not svg-data) (assoc-in [:data :svg-data] (<? (http/fetch (-> entity :data :svg-data-url))))))))

(defmethod load-editing-data :default [entity]
  (go entity))

(defn- prepare-for-editing [entity path]
  (go
    (rf/dispatch-sync [:set path {:status :loading}])
    (try
      (let [updated-entity (<? (load-editing-data entity))]
        (rf/dispatch [:set path {:status :done
                                 :entity updated-entity}]))
      (catch :default e
        (log/error "fetching entity data for editing error:" e)
        (rf/dispatch [:set path {:status :error
                                 :error e}])))))

(defn- fetch-entity-for-editing [entity-id version path]
  (let [{:keys [status entity] :as result} @(rf/subscribe [::entity entity-id version])]
    (case status
      :done (prepare-for-editing entity path)
      :error (rf/dispatch [:set path result])
      nil)))

(defn- async-query-data [path load-fn]
  (reaction
   (let [data @(rf/subscribe [:get path])]
     (if data
       data
       (do
         (load-fn path)
         {:status :loading})))))

(rf/reg-sub-raw ::entity
  (fn [_app-db [_ entity-id version]]
    (async-query-data (entity-path entity-id version)
                      (partial fetch-entity entity-id version))))

(rf/reg-sub-raw ::entity-for-editing
  (fn [_app-db [_ entity-id version]]
    (async-query-data (entity-for-editing-path entity-id version)
                      (partial fetch-entity-for-editing entity-id version))))

(defn store [entity-type entity & {:keys [on-start on-complete on-success on-error]}]
  (go
    (when on-start
      (on-start))
    (try
      (let [user-data (user/data)
            response (<? (api.request/call
                          (save-entity-api-function entity-type)
                          entity user-data))]
        ;; TODO: wire up things
        ;; - add new id/version in repository
        ;; - update is/nil in repository
        ;; - update list repository
        (when on-success
          (on-success response)))

      (catch :default e
        (log/error "save entity error:" e)
        (when on-error
          (on-error e)))

      (finally
        (when on-complete
          (on-complete))))))

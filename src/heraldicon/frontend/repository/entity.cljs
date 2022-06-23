(ns heraldicon.frontend.repository.entity
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.entity.id :as id]
   [heraldicon.frontend.repository.core :as repository]
   [heraldicon.frontend.repository.request :as request]
   [heraldicon.frontend.user :as user]
   [re-frame.core :as rf]
   [taoensso.timbre :as log])
  (:require-macros [reagent.ratom :refer [reaction]]))

(def ^:private db-path-entity
  (conj repository/db-path-base :entity))

(defn- entity-path [entity-id version]
  (conj db-path-entity [entity-id version]))

(def ^:private db-path-latest-versions
  (conj repository/db-path-base :latest-versions))

(defn- latest-version-path [entity-id]
  (conj db-path-latest-versions entity-id))

(rf/reg-sub ::latest-version
  (fn [[_ entity-id] _]
    (rf/subscribe [:get (latest-version-path entity-id)]))

  (fn [version [_ _entity-id]]
    version))

(rf/reg-sub ::effective-version
  (fn [[_ entity-id _version] _]
    (rf/subscribe [::latest-version entity-id]))

  (fn [latest-version [_ _entity-id version]]
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
        latest-version
        parsed-version))))

(defn update-latest-versions [db entities]
  (let [version-map (into {}
                          (map (juxt :id :version))
                          entities)]
    (update-in
     db db-path-latest-versions
     #(merge-with (fn [a b]
                    (if (and a b)
                      (max a b)
                      (or a b)))
                  % version-map))))

(rf/reg-event-fx ::store
  (fn [{:keys [db]} [_ {:keys [id version] :as entity}]]
    {:db (-> db
             (assoc-in (entity-path id version) {:status :done
                                                 :entity entity})
             (update-latest-versions [entity]))
     :fx [[:dispatch [:heraldicon.frontend.repository.entity-list/update entity]]]}))

(rf/reg-event-db ::store-error
  (fn [db [_ entity-id version error]]
    (assoc-in db (entity-path entity-id version) {:status :error
                                                  :error error})))

(defn- fetch-entity-api-function [entity-type]
  (case entity-type
    :heraldicon.entity.type/arms :fetch-arms
    :heraldicon.entity.type/charge :fetch-charge
    :heraldicon.entity.type/ribbon :fetch-ribbon
    :heraldicon.entity.type/collection :fetch-collection))

(defn- fetch [entity-id version user-data & {:keys [on-loaded]}]
  (go
    (try
      (let [entity (<? (request/call (fetch-entity-api-function (id/type-from-id entity-id))
                                     {:id entity-id
                                      :version version}
                                     user-data))]
        (when-not entity
          (throw (ex-info "Not found" {} :entity-not-found)))
        (rf/dispatch [::store entity])
        (when on-loaded
          (on-loaded entity)))
      (catch :default e
        (log/error "fetch entity error:" e)
        (rf/dispatch [::store-error entity-id version e])))))

(rf/reg-sub-raw ::data
  (fn [_app-db [_ entity-id version on-loaded]]
    (reaction
     (let [version @(rf/subscribe [::effective-version entity-id version])
           user-data (user/data)]
       (repository/async-query-data (entity-path entity-id version)
                                    (partial fetch entity-id version user-data)
                                    :on-loaded on-loaded)))))

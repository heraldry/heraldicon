(ns heraldicon.frontend.entity.action.copy-to-new
  (:require
   [heraldicon.entity.attribution :as attribution]
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.message :as message]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]))

(defn copy-data-path [form-id]
  [:copy-data form-id])

(defn- set-attribution [entity source-entity]
  (let [{source-id :id
         source-version :version
         source-latest-version :latest-version} source-entity
        source-version (if (zero? source-version)
                         source-latest-version
                         source-version)
        {:keys [license license-version]} (:attribution source-entity)
        {creator :username
         source-name :name} source-entity]
    (cond-> entity
      source-id (update :attribution
                        assoc
                        :nature :derivative
                        :license license
                        :license-version license-version
                        :source-name source-name
                        :source-link (attribution/full-url-for-entity source-id source-version)
                        :source-license license
                        :source-license-version license-version
                        :source-creator-name creator
                        :source-creator-link (attribution/full-url-for-username creator)))))

(rf/reg-sub ::copy-data
  (fn [db [_ form-id]]
    (get-in db (copy-data-path form-id))))

(macros/reg-event-db ::set-copy-data
  (fn [db [_ form-id data]]
    (assoc-in db (copy-data-path form-id) data)))

(macros/reg-event-db ::copy
  (fn [db [_ form-id target-path]]
    (let [data-path (copy-data-path form-id)
          data (get-in db data-path)]
      (-> db
          (assoc-in target-path data)
          (assoc-in data-path nil)))))

(defn- copy-entity [source-entity]
  (-> source-entity
      (select-keys [:type :name :tags :metadata :data])
      (set-attribution source-entity)))

(defn- create-route [form-id]
  (case form-id
    :heraldicon.entity/arms :route.arms/create
    :heraldicon.entity/charge :route.charge/create
    :heraldicon.entity/ribbon :route.ribbon/create
    :heraldicon.entity/collection :route.collection/create))

(defn- invoke [form-id]
  (let [form-db-path (form/data-path form-id)
        source-entity @(rf/subscribe [:get form-db-path])
        new-entity (copy-entity source-entity)]
    (rf/dispatch [::set-copy-data form-id new-entity])
    (rf/dispatch [::message/set-success form-id :string.user.message/created-unsaved-copy])
    (reife/push-state (create-route form-id))))

(defn action [form-id]
  {:title :string.button/copy-to-new
   :icon "fas fa-clone"
   :handler (partial invoke form-id)
   :disabled? false})

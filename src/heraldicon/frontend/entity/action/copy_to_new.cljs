(ns heraldicon.frontend.entity.action.copy-to-new
  (:require
   [heraldicon.entity.attribution :as attribution]
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.message :as message]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]))

(defn copy-data-path [entity-type]
  [:copy-data entity-type])

(defn- set-attribution [entity source-entity]
  (let [{source-id :id
         source-version :version} source-entity
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
  (fn [db [_ entity-type]]
    (get-in db (copy-data-path entity-type))))

(macros/reg-event-db ::set-copy-data
  (fn [db [_ entity-type data]]
    (assoc-in db (copy-data-path entity-type) data)))

(macros/reg-event-db ::copy
  (fn [db [_ entity-type target-path]]
    (let [data-path (copy-data-path entity-type)
          data (get-in db data-path)]
      (-> db
          (assoc-in target-path data)
          (assoc-in data-path nil)))))

(defn- copy-entity [source-entity]
  (-> source-entity
      (select-keys [:type :name :tags :metadata :data])
      (set-attribution source-entity)))

(defn- create-route [entity-type]
  (case entity-type
    :heraldicon.entity.type/arms :route.arms.details/create
    :heraldicon.entity.type/charge :route.charge.details/create
    :heraldicon.entity.type/ribbon :route.ribbon.details/create
    :heraldicon.entity.type/collection :route.collection.details/create))

(defn- invoke [entity-type]
  (let [form-db-path (form/data-path entity-type)
        source-entity @(rf/subscribe [:get form-db-path])
        new-entity (copy-entity source-entity)]
    (rf/dispatch [::set-copy-data entity-type new-entity])
    (rf/dispatch [::message/set-success entity-type :string.user.message/created-unsaved-copy])
    (reife/push-state (create-route entity-type))))

(defn action [entity-type]
  {:title :string.button/copy-to-new
   :icon "fas fa-clone"
   :handler (partial invoke entity-type)
   :disabled? false})

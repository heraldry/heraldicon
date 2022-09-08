(ns heraldicon.frontend.entity.action.copy-to-new
  (:require
   [heraldicon.entity.attribution :as attribution]
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]))

(defn- copy-data-path [entity-type]
  [:copy-data entity-type])

(defn- set-attribution [entity source-entity set-attribution?]
  (let [{source-id :id} source-entity
        {:keys [license license-version]} (:attribution source-entity)
        {:keys [attribution]
         creator :username
         source-name :name} source-entity]
    (assoc entity :attribution
           (if (and set-attribution?
                    source-id)
             {:nature :derivative
              :license license
              :license-version license-version
              :source-name source-name
              :source-link (attribution/full-url-for-entity-data source-entity)
              :source-license license
              :source-license-version license-version
              :source-creator-name creator
              :source-creator-link (attribution/full-url-for-username creator)}
             (dissoc attribution :source-modification)))))

(rf/reg-sub ::copy-data
  (fn [db [_ entity-type]]
    (get-in db (copy-data-path entity-type))))

(macros/reg-event-db ::copy
  (fn [db [_ entity-type target-path]]
    (let [data-path (copy-data-path entity-type)
          data (get-in db data-path)]
      (cond-> db
        data (->
               (assoc-in target-path data)
               (assoc-in data-path nil))))))

(defn- copy-entity [source-entity set-attribution?]
  (-> source-entity
      (select-keys [:type :name :tags :metadata :data])
      (set-attribution source-entity set-attribution?)))

(defn- create-route [entity-type]
  (case entity-type
    :heraldicon.entity.type/arms :route.arms.details/create
    :heraldicon.entity.type/charge :route.charge.details/create
    :heraldicon.entity.type/ribbon :route.ribbon.details/create
    :heraldicon.entity.type/collection :route.collection.details/create))

(rf/reg-fx ::set-create-entity-route
  (fn [entity-type]
    (reife/push-state (create-route entity-type))))

(rf/reg-event-fx ::invoke
  (fn [{:keys [db]} [_ entity-type]]
    (let [form-db-path (form/data-path entity-type)
          source-entity (get-in db form-db-path)
          session (session/data-from-db db)
          set-attribution? (not= (:user-id source-entity) (:user-id session))
          new-entity (copy-entity source-entity set-attribution?)]
      {:db (assoc-in db (copy-data-path entity-type) new-entity)
       :fx [[:dispatch [::message/set-success entity-type :string.user.message/created-unsaved-copy]]
            [::set-create-entity-route entity-type]]})))

(defn action [entity-type]
  {:title :string.button/copy-to-new
   :icon "fas fa-clone"
   :handler #(rf/dispatch [::invoke entity-type])
   :disabled? false})

(ns heraldicon.frontend.entity.form
  (:require
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.repository.core :as repository]
   [re-frame.core :as rf])
  (:require-macros [reagent.ratom :refer [reaction]]))

(def ^:private db-path-base
  [:forms])

(defn data-path [form-id]
  (conj db-path-base form-id))

(macros/reg-event-db ::clear
  (fn [db [_ form-id]]
    (update-in db db-path-base dissoc form-id)))

(rf/reg-sub-raw ::unsaved-changes?
  (fn [_app-db [_ form-id]]
    (reaction
     (let [{:keys [id version]
            :as current} @(rf/subscribe [:get (data-path form-id)])]
       (if (and id version)
         (let [{status :status
                saved :entity} @(rf/subscribe [::repository/entity-for-editing id version])]
           (if (= status :done)
             (case form-id
               :heraldicon.entity/arms (not= (assoc-in current [:data :achievement :render-options] nil)
                                             (assoc-in saved [:data :achievement :render-options] nil))
               (not= current saved))
             true))
         true)))))

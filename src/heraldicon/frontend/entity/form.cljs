(ns heraldicon.frontend.entity.form
  (:require
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.repository.entity-for-editing :as entity-for-editing]
   [re-frame.core :as rf])
  (:require-macros [reagent.ratom :refer [reaction]]))

(def ^:private db-path-base
  [:forms])

(defn data-path [entity-type]
  (conj db-path-base entity-type))

(macros/reg-event-db ::clear
  (fn [db [_ entity-type]]
    (update-in db db-path-base dissoc entity-type)))

(rf/reg-sub-raw ::unsaved-changes?
  (fn [_app-db [_ entity-type]]
    (reaction
     (let [{:keys [id version]
            :as current} @(rf/subscribe [:get (data-path entity-type)])]
       (if (and id version)
         (let [{status :status
                saved :entity} @(rf/subscribe [::entity-for-editing/data id version])]
           (if (= status :done)
             (case entity-type
               :heraldicon.entity.type/arms (not= (assoc-in current [:data :achievement :render-options] nil)
                                                  (assoc-in saved [:data :achievement :render-options] nil))
               (not= current saved))
             true))
         true)))))
